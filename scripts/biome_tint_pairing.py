#!/usr/bin/env python3
"""Check that biome tinting is wired up on both sides for every plant.

A block is only tinted when TWO unrelated things agree: its baked model carries
`"tintindex": 0` (inherited from minecraft:block/tinted_cross or tinted_flower_pot_cross),
and HempdustryClient registers a ColorProviderRegistry.BLOCK provider for it. Break either
half and nothing errors -- the plant simply renders as drawn, or the provider is never
asked, and the only symptom is a crop that stubbornly refuses to match its biome. That is
exactly the class of failure nobody notices in a screenshot.

Usage:
    python3 scripts/biome_tint_pairing.py

Exits non-zero and names the offenders when the two halves disagree.
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
CLIENT = ROOT / "src/main/java/com/warlonmhite/hempdustry/HempdustryClient.java"
ASSETS = [ROOT / "src/main/resources/assets/hempdustry",
          ROOT / "src/main/generated/assets/hempdustry"]

# The vanilla parents that put "tintindex": 0 on a plant's faces.
TINTED_PARENTS = {"minecraft:block/tinted_cross", "minecraft:block/tinted_flower_pot_cross"}


def registered_blocks():
    """Block ids passed to ColorProviderRegistry.BLOCK.register(...) in HempdustryClient."""
    source = CLIENT.read_text(encoding="utf-8")
    start = source.find("ColorProviderRegistry.BLOCK.register(")
    if start < 0:
        sys.exit("no ColorProviderRegistry.BLOCK.register(...) call in HempdustryClient")
    depth, i = 0, source.index("(", start)
    for i in range(i, len(source)):
        depth += (source[i] == "(") - (source[i] == ")")
        if depth == 0:
            break
    call = source[start:i]
    return {name.lower() for name in re.findall(r"ModBlocks\.([A-Z0-9_]+)", call)}


def models_by_block():
    """block id -> every hempdustry model its blockstate can show."""
    out = {}
    for assets in ASSETS:
        for state in sorted((assets / "blockstates").glob("*.json")):
            refs = set(re.findall(r"hempdustry:block/([a-z0-9_]+)", state.read_text(encoding="utf-8")))
            out.setdefault(state.stem, set()).update(refs)
    return out


def find_model(name):
    for assets in ASSETS:
        path = assets / "models" / "block" / f"{name}.json"
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
    return None


def is_tinted(name, seen=()):
    """Walk the parent chain until it leaves our namespace, then judge the vanilla parent."""
    if name in seen:
        return False  # a cycle is broken art, but that is not this script's job to report
    model = find_model(name)
    if model is None:
        return False
    parent = model.get("parent", "")
    if parent.startswith("hempdustry:block/"):
        return is_tinted(parent.split("/", 1)[1], (*seen, name))
    return parent in TINTED_PARENTS


def main():
    registered = registered_blocks()
    by_block = models_by_block()
    tinted = {block for block, models in by_block.items()
              if models and all(is_tinted(model) for model in models)}
    partial = {block for block, models in by_block.items()
               if any(is_tinted(model) for model in models)} - tinted

    problems = []
    for block in sorted(registered - tinted - partial):
        problems.append(f"{block}: has a colour provider, but no model of it is tinted "
                        f"(parent minecraft:block/tinted_cross?)")
    for block in sorted(tinted - registered):
        problems.append(f"{block}: models are tinted, but HempdustryClient registers no "
                        f"colour provider -- it will render pitch-tinted by BlockColors' -1 default")
    for block in sorted(partial):
        models = sorted(m for m in by_block[block] if not is_tinted(m))
        problems.append(f"{block}: only some models are tinted, so the plant changes colour "
                        f"as it grows. Untinted: {', '.join(models)}")

    if problems:
        print("biome tint pairing is broken:")
        for problem in problems:
            print(f"  - {problem}")
        return 1
    print(f"biome tint pairing OK: {len(registered)} block(s) tinted on both sides "
          f"({', '.join(sorted(registered))})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
