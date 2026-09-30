#!/usr/bin/env python3
"""Check that everything the game names has an English name, and that no name outlives its thing.

The locale gate only proves the lang files agree with EACH OTHER: twelve files can be perfectly
identical and all twelve missing the same key, and the game then shows the raw key to every player.
This reads what exists -- the client item definitions, the blockstates, the item tags -- and asserts
en_us names each of them:

  - every item:   item.hempdustry.<id> or block.hempdustry.<id>
  - every block:  block.hempdustry.<id>, or item.hempdustry.<id> when its item is what names it
                  (the bongs are blocks their device item places)
  - every item tag outside minecraft: tag.item.<ns>.<path, slashes as periods>, in our file or in
    Fabric's convention tags, which already name the c: tags we only add to (c:strings, c:crops...).
    Fabric's names are read from the Fabric API jar gradle.properties pins, in the Gradle cache.

It also reports ORPHANS, an item./block./tag.item. key naming nothing that exists any more, which is
how a rename half-lands. Block tags are not checked: nothing in the game shows a block tag's name.

Usage:
    python3 scripts/lang_coverage.py [checkout]

Exits non-zero and names every missing key and orphan.
"""
import glob
import io
import json
import os
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parent.parent
SOURCES = [ROOT / "src/main/resources", ROOT / "src/main/generated"]
LANG = ROOT / "src/main/resources/assets/hempdustry/lang/en_us.json"

# Client item definitions with no item behind them: the press renderer draws its platen and screw
# through them, and nothing ever shows their name.
UNNAMED = {"hemp_press_platen", "hemp_press_screw"}


def ids(folder):
    return {p.stem for root in SOURCES for p in (root / "assets/hempdustry" / folder).glob("*.json")}


def item_ids():
    # Client item definitions arrived in 1.21.4; before them (the 1.21.1 line) every item has a model
    # of its own instead, beside the variants its overrides switch to (trims, a packed device), which
    # are not items. Finding none either way means the format moved again, not that all is well.
    found = ids("items")
    if not found:
        models = [p for root in SOURCES for p in (root / "assets/hempdustry/models/item").glob("*.json")]
        variants = {o["model"].rsplit("/", 1)[1] for p in models
                    for o in json.loads(p.read_text()).get("overrides", [])}
        found = {p.stem for p in models} - variants
    if not found:
        sys.exit("No items found in items/ or models/item/: this script no longer reads the format.")
    return found


def item_tags():
    tags = set()
    for root in SOURCES:
        for p in root.glob("data/*/tags/item/**/*.json"):
            ns = p.relative_to(root / "data").parts[0]
            path = p.relative_to(root / "data" / ns / "tags/item").with_suffix("")
            tags.add(f"{ns}.{'.'.join(path.parts)}")
    return tags


def fabric_names():
    props = dict(line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
                 if "=" in line and not line.startswith("#"))
    version = props["fabric_version"].strip()
    jars = glob.glob(os.path.expanduser(
        f"~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/{version}/*/fabric-api-{version}.jar"))
    if not jars:
        sys.exit(f"Fabric API {version} is not in the Gradle cache, so its tag names cannot be read: "
                 "run ./gradlew build once first.")
    names = {}
    with zipfile.ZipFile(jars[0]) as api:
        for nested in (n for n in api.namelist() if "convention-tags" in n and n.endswith(".jar")):
            with zipfile.ZipFile(io.BytesIO(api.read(nested))) as module:
                for entry in module.namelist():
                    if entry.endswith("/lang/en_us.json"):
                        names.update(json.loads(module.read(entry)))
    return names


def main():
    lang = json.loads(LANG.read_text(encoding="utf-8"))
    items, blocks, tags = item_ids(), ids("blockstates"), item_tags()
    fabric = fabric_names()
    missing = [f"item {i}" for i in sorted(items - UNNAMED)
               if f"item.hempdustry.{i}" not in lang and f"block.hempdustry.{i}" not in lang]
    missing += [f"block {b}" for b in sorted(blocks)
                if f"block.hempdustry.{b}" not in lang and f"item.hempdustry.{b}" not in lang]
    missing += [f"item tag {t}" for t in sorted(tags)
                if not t.startswith("minecraft.") and f"tag.item.{t}" not in lang and f"tag.item.{t}" not in fabric]

    # A key may run on past its id (item.hempdustry.bong.packed), and a banner pattern's names hang
    # off the translation_key its data file declares rather than off an id.
    named = {f"hempdustry.{i}" for i in items | blocks}
    for root in SOURCES:
        for p in root.glob("data/*/banner_pattern/*.json"):
            named.add(json.loads(p.read_text())["translation_key"].split(".", 1)[1])
    orphans = [k for k in lang if k.startswith(("item.", "block.")) and not any(
        k.split(".", 1)[1] == n or k.split(".", 1)[1].startswith(n + ".") for n in named)]
    orphans += [k for k in lang if k.startswith("tag.item.") and k[len("tag.item."):] not in tags]

    for line in missing:
        print(f"MISSING  {line}")
    for key in orphans:
        print(f"ORPHAN   {key}")
    print(f"{len(items)} items, {len(blocks)} blocks, {len(tags)} item tags checked against "
          f"{len(lang)} keys ({len(fabric)} of Fabric's): {len(missing)} missing, {len(orphans)} orphan(s)")
    sys.exit(1 if missing or orphans else 0)


if __name__ == "__main__":
    main()
