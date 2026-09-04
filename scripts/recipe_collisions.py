#!/usr/bin/env python3
"""Find crafting recipes that can match the same grid.

Only one of two recipes that accept the same ingredients will ever fire, and which one
wins is hash-bucket order inside RecipeManager's HashMap — deterministic for a given id
set, but it flips silently when any other recipe is added or removed. The loser still
generates its unlock advancement, so the recipe book shows something that does nothing.

Usage:
    python3 scripts/recipe_collisions.py src/main/generated [vanilla-data-dir]

The optional second argument is a directory holding an extracted vanilla `data/` tree,
which lets the scan catch mod-vs-vanilla collisions as well as mod-vs-mod:

    unzip -q ~/.gradle/caches/fabric-loom/<mcver>/minecraft-client.jar 'data/minecraft/*' -d /tmp/mcdata

Custom crafting recipe types this mod adds are mapped onto the vanilla shape they behave like
(see SHAPELESS_TYPES / SHAPED_TYPES) -- they still occupy the crafting grid and can still collide,
so leaving them out would make the scan silently under-report.

Matching rules mirror vanilla:
  - shaped vs shaped   -> collide when ingredients AND shape match (shape up to the
                          horizontal mirror ShapedRecipe.matches also tests)
  - anything shapeless -> collides on the ingredient multiset alone, since a shapeless
                          recipe matches any arrangement of its ingredients

Tags are compared as opaque atoms, so two *different* tags that happen to overlap are
not reported. Exit status is 1 when a collision involving this mod is found.

Two things this scan structurally cannot see, so a clean run does not rule them out:

  - `hempdustry:packing` is a SpecialCraftingRecipe with no ingredient list in its JSON,
    so it is invisible here. A future shapeless recipe taking a device plus buds would
    collide with packing and this would still print zero.
  - A tag that *overlaps* another tag or a plain item, since tags are atoms (above).
"""

import collections
import json
import os
import sys


# Recipe types that match the grid like a vanilla shapeless recipe. hempdustry:container_carried
# is an ordinary shapeless recipe that suppresses its ingredients' recipe remainders.
SHAPELESS_TYPES = {
    "minecraft:crafting_shapeless",
    "hempdustry:container_carried",   # shapeless; suppresses its ingredients' recipe remainders
    "hempdustry:infused_shapeless",   # shapeless; copies cannabutter's potency/quality to the output
}
SHAPED_TYPES = {
    "minecraft:crafting_shaped",
    "hempdustry:infused_shaped",      # shaped; copies cannabutter's potency/quality to the output
}
# Recipe types that never touch the crafting grid, so they cannot collide in it. Listed rather than
# left to fall through the `else`, because a silently ignored type is exactly how this scanner has
# lost recipes twice -- an unlisted type and a deliberately excluded one look identical from the
# outside. Anything not in one of these three sets is reported below.
NON_CRAFTING_TYPES = {
    "hempdustry:decarboxylating",     # one tray-load in the Decarboxylator
    "hempdustry:infusing",            # the Infuser's whole conversion, in one recipe
    # In the grid, but unscannable: a SpecialCraftingRecipe's JSON carries no ingredient list at
    # all, so there is nothing here to compare. Its matcher requires a hempdustry device plus
    # hempdustry buds, which nothing else in the game can produce. See the module docstring.
    "hempdustry:packing",
}


def ingredient(v):
    # Since 1.21.5 an ingredient is a bare string -- "minecraft:paper", or "#c:strings" for a tag --
    # rather than {"item": ...} / {"tag": ...}. Both shapes are accepted so the script still reads a
    # older (pre-1.21.5) data dump.
    if isinstance(v, list):
        return "|".join(sorted(ingredient(x) for x in v))
    if isinstance(v, str):
        return v
    return "#" + v["tag"] if "tag" in v else v["item"]


def load(root, namespace):
    out = []
    base = os.path.join(root, "data", namespace, "recipe")
    for dirpath, _, filenames in os.walk(base):
        for name in filenames:
            if not name.endswith(".json"):
                continue
            path = os.path.join(dirpath, name)
            with open(path) as fh:
                recipe = json.load(fh)
            rid = namespace + ":" + os.path.relpath(path, base)[:-len(".json")]
            kind = recipe.get("type", "")
            if kind in SHAPED_TYPES:
                key = {k: ingredient(v) for k, v in recipe["key"].items()}
                width = max(len(row) for row in recipe["pattern"])
                pattern = [row.ljust(width) for row in recipe["pattern"]]
                counts = collections.Counter()
                for row in pattern:
                    for cell in row:
                        if cell != " ":
                            counts[key[cell]] += 1
                shape = tuple(tuple(key.get(c, "") for c in row) for row in pattern)
                mirror = tuple(tuple(reversed(row)) for row in shape)
                out.append((rid, "shaped", frozenset(counts.items()), min(shape, mirror)))
            elif kind in SHAPELESS_TYPES:
                counts = collections.Counter(ingredient(v) for v in recipe["ingredients"])
                out.append((rid, "shapeless", frozenset(counts.items()), None))
            elif kind not in NON_CRAFTING_TYPES and not kind.startswith("minecraft:"):
                # A hempdustry type nobody has classified. Saying so is the whole point: an
                # unlisted crafting type is scanned as if it did not exist, and the run still
                # prints "0 collisions".
                print(f"UNSCANNED  {rid} has type {kind!r}, which is in none of SHAPED_TYPES, "
                      f"SHAPELESS_TYPES or NON_CRAFTING_TYPES. Classify it in this script or it "
                      f"is not being checked.")
    return out


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    recipes = load(argv[1], "hempdustry")
    if len(argv) < 3:
        # Refusing outright would break the one legitimate use -- a quick mod-vs-mod check --
        # so this warns instead. But it warns *loudly*, because "0 collisions" from a scan that
        # never opened vanilla reads exactly like "0 collisions" from one that did, and most of
        # this mod's recipes compete with vanilla rather than with each other.
        print("WARNING: no vanilla data directory given, so this scan has only compared this "
              "mod's recipes against each other. A collision with a VANILLA recipe cannot be "
              "found this way. Extract vanilla's data and pass the directory holding `data/`:\n"
              "  unzip -q ~/.gradle/caches/fabric-loom/<mcver>/minecraft-client.jar "
              "'data/minecraft/*' -d /tmp/mcdata\n"
              "  python3 scripts/recipe_collisions.py src/main/generated /tmp/mcdata\n")
    else:
        vanilla = load(argv[2], "minecraft")
        # A wrong path here is silent otherwise: the scan still runs, still says "0 collisions",
        # and has simply not looked at vanilla at all. That is the same class of under-reporting
        # as an unlisted custom recipe type, and it is easy to walk into, since the argument wants
        # the directory *containing* `data/` and not the `data/` directory itself.
        if not vanilla:
            print(f"ERROR: no vanilla recipes found under {argv[2]!r}. Expected "
                  f"{os.path.join(argv[2], 'data', 'minecraft', 'recipe')} to exist — pass the "
                  f"directory that holds `data/`, not `data/` itself. Refusing to report a "
                  f"mod-only scan as a clean one.")
            return 2
        recipes += vanilla

    by_ingredients = collections.defaultdict(list)
    for recipe in recipes:
        by_ingredients[recipe[2]].append(recipe)

    collisions = 0
    for ingredients, group in by_ingredients.items():
        for i in range(len(group)):
            for j in range(i + 1, len(group)):
                a, b = group[i], group[j]
                if a[1] == "shaped" and b[1] == "shaped" and a[3] != b[3]:
                    continue
                if not (a[0].startswith("hempdustry") or b[0].startswith("hempdustry")):
                    continue
                collisions += 1
                print(f"COLLISION  {a[0]} ({a[1]})  <->  {b[0]} ({b[1]})")
                print(f"           ingredients: {dict(ingredients)}")

    print(f"\n{len(recipes)} crafting recipes scanned, {collisions} collision(s)")
    return 1 if collisions else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
