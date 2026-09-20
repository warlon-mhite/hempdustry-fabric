#!/usr/bin/env python3
"""Check that every multi-block plant's loot table can only pay out once.

A plant that occupies more than one block rolls its loot table TWICE per break: once for the
half the player struck, and once as the orphaned half pops off in the neighbour update. A pool
with no "which half am I" condition therefore pays out twice -- two flowers, or two rolls of a
seed bonus, from one plant. It is silent, and it errs in the generous direction, so nothing about
playing the game reveals it (see .claude/docs/crops.md, "the tall-plant loot double-roll").

The fix vanilla uses on rose_bush, and this mod uses on both crops and the wild Lemon Haze, is to
condition the pool on the lower half. This script asserts that every pool of every multi-block
plant carries such a condition.

Usage:
    python3 scripts/tall_plant_loot.py

Exits non-zero and names the offending pools.
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
GENERATED = ROOT / "src/main/generated"
ASSETS = [ROOT / "src/main/resources/assets/hempdustry", GENERATED / "assets/hempdustry"]

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


def guarded(pool, prop):
    """True when this pool cannot fire for every segment of the plant."""
    for condition in pool.get("conditions", []):
        if condition.get("condition") == "minecraft:block_state_property" \
                and prop in condition.get("properties", {}):
            return True
    return False


def main():
    plants = multi_block_plants()
    if not plants:
        sys.exit("no multi-block plants found -- has the blockstate layout changed?")

    problems = []
    checked = []
    for block, prop in sorted(plants.items()):
        path = GENERATED / "data/hempdustry/loot_table/blocks" / f"{block}.json"
        if not path.exists():
            continue  # a block with no loot table of its own drops nothing; nothing to double
        table = json.loads(path.read_text(encoding="utf-8"))
        pools = table.get("pools", [])
        checked.append(f"{block} ({len(pools)} pool(s), on '{prop}')")
        for index, pool in enumerate(pools):
            if not guarded(pool, prop):
                problems.append(f"{block}: pool {index} has no '{prop}' condition, so it rolls "
                                f"once per block of the plant and pays out twice")

    if problems:
        print("multi-block plant loot can double-roll:")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print("multi-block plant loot OK: " + "; ".join(checked))
    return 0


if __name__ == "__main__":
    sys.exit(main())
