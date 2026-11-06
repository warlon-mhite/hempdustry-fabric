#!/usr/bin/env python3
"""Check that every multi-block plant's loot table can only pay out once.

A plant that occupies more than one block rolls its loot table TWICE per break: once for the
half the player struck, and once as the orphaned half pops off in the neighbour update. A pool
with no "which half am I" condition therefore pays out twice -- two flowers, or two rolls of a
seed bonus, from one plant. It is silent, and it errs in the generous direction, so nothing about
playing the game reveals it.

The fix vanilla uses on rose_bush, and this mod uses on both crops and the wild Lemon Haze, is to
condition the pool on the lower half. This script asserts that every pool of every multi-block
plant carries such a condition -- and that it names the plant's ROOT, where the age and the trim
flags live: the lower half of a plant standing on the ground, the upper half of one hanging from a
ceiling (a crop class whose hangs() returns true). A hanging plant pinned to its lower half would
still pay out once, but from the tip, which carries no trim flags, so every trimmed plant would pay
as an untrimmed one.

Usage:
    python3 scripts/tall_plant_loot.py

Exits non-zero and names the offending pools.
"""
import json
import pathlib
import re
import sys

import layout

HANGS = re.compile(r"boolean\s+hangs\s*\(\s*\)\s*\{\s*return\s+true\s*;")
CLASS = re.compile(r"^public\s+(?:final\s+)?class\s+(\w+)", re.MULTILINE)

LAYOUT = layout.of()
GENERATED = LAYOUT.generated
ASSETS = LAYOUT.assets

# Blockstate properties that mean "which slice of a multi-block plant is this".
SEGMENT_PROPERTIES = ("half", "segment")
# ...but a door and a stairs block carry "half" too, and neither is a plant: a door drops from one
# half by vanilla's own rule, and a stairs block is one block. A plant does not face anywhere, so
# the absence of "facing" is what tells them apart.
NOT_A_PLANT = ("facing",)


def state_properties(definition):
    """Every property a blockstate file selects on, in either form it can be written.

    A `variants` map keys on "half=lower,age=7"; a `multipart` list keys on {"half": "lower"},
    optionally nested under AND/OR. Reading only the first form is how this check silently stopped
    covering the three crops the day their blockstates became multipart -- it reported the plants it
    still knew about and said nothing about the ones it had lost."""
    props = set()
    for key in definition.get("variants", {}):
        props.update(part.split("=")[0] for part in key.split(",") if "=" in part)

    def walk(when):
        for key, value in when.items():
            if key in ("AND", "OR"):
                for clause in value:
                    walk(clause)
            else:
                props.add(key)

    for part in definition.get("multipart", []):
        walk(part.get("when", {}))
    return props


def multi_block_plants():
    """block id -> the segment property it splits on, for every blockstate that has one."""
    found = {}
    for assets in ASSETS:
        for state in sorted((assets / "blockstates").glob("*.json")):
            keys = state_properties(json.loads(state.read_text(encoding="utf-8")))
            if keys.intersection(NOT_A_PLANT):
                continue
            for prop in SEGMENT_PROPERTIES:
                if prop in keys:
                    found[state.stem] = prop
    return found


def hanging_blocks():
    """The block ids registered with a crop class that hangs from a ceiling.

    Read from the code rather than listed here, so a new hanging plant is held to its root the day it
    is registered. Each hanging class must be registered under some id, or this says so: a scanner
    that found the class and lost the block would be checking nothing."""
    classes = set()
    sources = list(LAYOUT.java_files())
    for path in sources:
        text = path.read_text(encoding="utf-8")
        if HANGS.search(text):
            found = CLASS.search(text)
            if found:
                classes.add(found.group(1))
    blocks = {}
    for path in sources:
        text = path.read_text(encoding="utf-8")
        for name in classes:
            for match in re.finditer(r'"([a-z0-9_]+)"\s*,\s*' + name + r"::new", text):
                blocks[match.group(1)] = name
    missing = classes - set(blocks.values())
    if missing:
        sys.exit(f"hanging crop classes with no block registered under them: {sorted(missing)} -- "
                 "has the registration pattern changed?")
    return blocks


def guard_values(pool, prop):
    """The values of `prop` this pool's own conditions require -- empty when it names none."""
    values = set()
    for condition in pool.get("conditions", []):
        if condition.get("condition") == "minecraft:block_state_property" \
                and prop in condition.get("properties", {}):
            values.add(condition["properties"][prop])
    return values


def main():
    plants = multi_block_plants()
    if not plants:
        sys.exit("no multi-block plants found -- has the blockstate layout changed?")

    hanging = hanging_blocks()
    problems = []
    checked = []
    for block, prop in sorted(plants.items()):
        path = GENERATED / "data/hempdustry/loot_table/blocks" / f"{block}.json"
        if not path.exists():
            continue  # a block with no loot table of its own drops nothing; nothing to double
        table = json.loads(path.read_text(encoding="utf-8"))
        pools = table.get("pools", [])
        root = "upper" if block in hanging else "lower"
        checked.append(f"{block} ({len(pools)} pool(s), on '{prop}={root}'"
                       + (", hanging" if block in hanging else "") + ")")
        for index, pool in enumerate(pools):
            values = guard_values(pool, prop)
            if not values:
                problems.append(f"{block}: pool {index} has no '{prop}' condition, so it rolls "
                                f"once per block of the plant and pays out twice")
            elif values != {root}:
                problems.append(f"{block}: pool {index} is pinned to {prop}={sorted(values)}, not the "
                                f"plant's root {prop}={root}, where its age and trim flags live")

    if problems:
        print("multi-block plant loot can double-roll or pay from the wrong half:")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print("multi-block plant loot OK: " + "; ".join(checked))
    return 0


if __name__ == "__main__":
    sys.exit(main())
