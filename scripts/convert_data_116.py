#!/usr/bin/env python3
"""Convert the mod's 1.20.1 data files to the formats Minecraft 1.16.5 reads.

The 1.16.5 line has no datagen. Its data is the 1.20.1 line's -- written by that line's Fabric
datagen into `common/src/generated/resources`, plus the hand-written `common/src/main/resources/data`
-- converted by this script. Re-run it whenever the 1.20.1 line's data changes, and commit both this
script and what it writes.

Usage (inside the dev shell, from this checkout's root):

    python3 scripts/convert_data_116.py [--from ../hempdustry-1.20.1-2.1] [--client-jar JAR] [--check]

  --from        the 1.20.1 checkout whose data is converted (default: ../hempdustry-1.20.1-2.1)
  --client-jar  a 1.16.5 client jar; every minecraft: item, block and tag the output names is
                checked against it (default: Loom's cached one, skipped with a warning if absent)
  --check       write nothing; exit 1 if the checkout's data differs from what would be written

What it writes:
  - common/src/generated/resources/data is replaced whole by the converted generated data;
  - each file of the 1.20.1 common/src/main/resources/data is written converted to the same path
    here, and removed here when 1.16.5 has no use for it. Files only this line has are left alone.

A file whose content needs no change is copied byte for byte; a changed one is written the way
datagen writes (two-space indent, sorted keys, no final newline) for the generated tree, and with
its own key order for the hand-written one.

What changes, and why (each rule names what 1.16.5 reads instead):

  Content 1.16.5 cannot hold
    - DROPPED_IDS: blocks and items the 1.16.5 line does not register (hanging signs came with
      1.20, the chest boat with 1.19). Their recipes, loot tables and recipe advancements are
      removed, and so is every tag entry and advancement criterion naming them.
    - DROPPED_RECIPES: recipes whose ingredients do not exist on 1.16.5 and have no agreed
      stand-in, with their recipe advancements. The item stays; it has no recipe.
    - INGREDIENT_SUBSTITUTES: vanilla items newer than 1.16.5 used as ingredients, replaced by a
      1.16.5 item. Each one is a stand-in until the mod's author decides.
    - Worldgen JSON (`worldgen/`): 1.16.5 registers the mod's features in code (world/*).
    - Registries 1.16.5 does not have or that the mod registers in code: painting variants,
      jukebox songs, banner patterns, damage types, trims, and their tags.
    - Tag folders 1.16.5 does not load (only blocks, items, fluids and entity_types), the mining
      tool tags (`tags/blocks/mineable/*`: 1.16.5's blocks name their tool in code), and vanilla
      tags 1.16.5 does not have (nothing would read them). TAG_RENAMES maps the few that 1.16.5
      has under an older name (`wool_carpets` was `carpets`).

  Recipes
    - `category` and `show_notification` (1.19.3 and 1.20) are removed; 1.16.5 has neither.

  Loot tables
    - `random_sequence` (1.20) is removed.
    - Number providers: a constant is a plain number (whole numbers as ints), a uniform range keeps
      `min` and `max`; `bonus_rolls` of 0 is left out, 1.16.5's default.
    - `set_count` has no `add` on 1.16.5; `"add": false` is dropped, `true` is an error.
    - `minecraft:any_of` is 1.16.5's `minecraft:alternative`. `minecraft:all_of` (1.20) does not
      exist: in a list of conditions -- already an "and" -- its terms are spliced in; anywhere
      else it becomes `inverted(alternative(inverted(term)...))`.
    - A location check naming several blocks (`"blocks": [...]`) becomes an alternative of one
      check per block; 1.16.5's block predicate names one block.

  Predicates (loot conditions and advancement criteria)
    - Item predicate: `"items": [one]` is 1.16.5's `"item": one`. Several items cannot be one
      vanilla 1.16.5 predicate; in an advancement criterion they become the mod's own Forge
      predicate type, `{"type": "hempdustry:any_of", "items": [...]}` (`forge/AnyOfItemPredicate`),
      so the criterion stays one criterion under its own name and a world taken on to a newer
      line keeps the progress.
    - Block predicate: `"blocks": [one]` is `"block": one`.

  Advancements
    - `sends_telemetry_event` (1.19) is removed.
    - Criteria whose conditions 1.20 writes as a list of loot conditions read a predicate object
      on 1.16.5: `placed_block` takes `block`/`state` from its `block_state_property`, and
      `location`/`item` from `location_check`/`match_tool`; `item_used_on_block` takes `location`
      and `item` the same way. Entity predicates written as condition lists (`entity`, `player`)
      are read as lists by 1.16.5 too, and only their conditions are converted.
    - A criterion naming a dropped id is removed, and its requirement with it; an advancement
      left with no criteria, or a recipe advancement whose recipe is gone, is removed.

Any shape the rules above do not know is an error, not a guess: the script stops and names the
file, so a change in the 1.20.1 line's data is looked at before it ships.
"""

import argparse
import json
import os
import sys
import zipfile

# --------------------------------------------------------------------------- the decisions

DROPPED_IDS = {
    "hempdustry:hemp_planks_hanging_sign",
    "hempdustry:hemp_planks_wall_hanging_sign",
    "hempdustry:hemp_chest_boat",
    # Made of tinted glass, which came with 1.17: the 1.16.5 line has no tinted bong at all.
    "hempdustry:tinted_bong",
}

# Recipe ids (data/<ns>/recipes/<path>.json) left out, with why.
DROPPED_RECIPES = {
}

INGREDIENT_SUBSTITUTES = {
    # Copper came with 1.17. Iron is the metal 1.16.5 has at that point of the game: the
    # Decarboxylator, the Hydro Tray and the Grow Lamp.
    "minecraft:copper_ingot": "minecraft:iron_ingot",
    # Amethyst came with 1.17. The Grow Lamp's violet crystal around a redstone lamp becomes
    # violet glass around it, which is what the amethyst stood for: the lamp's UV light.
    "minecraft:amethyst_shard": "minecraft:purple_stained_glass",
}

# Vanilla tags that 1.16.5 has under another name.
TAG_RENAMES = {
    ("blocks", "minecraft:wool_carpets"): "minecraft:carpets",
    ("items", "minecraft:wool_carpets"): "minecraft:carpets",
}

# Vanilla tags 1.16.5 does not have, named by a tag file or a recipe ingredient: the items the
# newer tag holds that 1.16.5 has. A tag file lists them; an ingredient becomes "any of them".
TAG_REFERENCE_SUBSTITUTES = {
    # #minecraft:dirt as an item tag came with 1.18 (1.16.5 has only Forge's block tag).
    ("items", "#minecraft:dirt"): [
        "minecraft:dirt", "minecraft:grass_block", "minecraft:podzol", "minecraft:coarse_dirt",
        "minecraft:mycelium",
    ],
    # #minecraft:swords came with 1.19.3; these are the swords it holds that 1.16.5 has.
    ("items", "#minecraft:swords"): [
        "minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword",
        "minecraft:golden_sword", "minecraft:diamond_sword", "minecraft:netherite_sword",
    ],
}

# Vanilla 1.16.5's own tags (data/minecraft/tags in its client jar).
VANILLA_TAGS = {
    "blocks": set("""
        acacia_logs anvil bamboo_plantable_on banners base_stone_nether base_stone_overworld
        beacon_base_blocks beds bee_growables beehives birch_logs buttons campfires carpets climbable
        coral_blocks coral_plants corals crimson_stems crops dark_oak_logs doors dragon_immune
        enderman_holdable fence_gates fences fire flower_pots flowers gold_ores guarded_by_piglins
        hoglin_repellents ice impermeable infiniburn_end infiniburn_nether infiniburn_overworld
        jungle_logs leaves logs logs_that_burn mushroom_grow_block non_flammable_wood nylium oak_logs
        piglin_repellents planks portals pressure_plates prevent_mob_spawning_inside rails sand
        saplings shulker_boxes signs slabs small_flowers soul_fire_base_blocks soul_speed_blocks
        spruce_logs stairs standing_signs stone_bricks stone_pressure_plates strider_warm_blocks
        tall_flowers trapdoors underwater_bonemeals unstable_bottom_center valid_spawn wall_corals
        wall_post_override wall_signs walls warped_stems wart_blocks wither_immune
        wither_summon_base_blocks wooden_buttons wooden_doors wooden_fences wooden_pressure_plates
        wooden_slabs wooden_stairs wooden_trapdoors wool
    """.split()),
    "items": set("""
        acacia_logs anvil arrows banners beacon_payment_items beds birch_logs boats buttons carpets
        coals creeper_drop_music_discs crimson_stems dark_oak_logs doors fences fishes flowers
        gold_ores jungle_logs leaves lectern_books logs logs_that_burn music_discs non_flammable_wood
        oak_logs piglin_loved piglin_repellents planks rails sand saplings signs slabs small_flowers
        soul_fire_base_blocks spruce_logs stairs stone_bricks stone_crafting_materials
        stone_tool_materials tall_flowers trapdoors walls warped_stems wooden_buttons wooden_doors
        wooden_fences wooden_pressure_plates wooden_slabs wooden_stairs wooden_trapdoors wool
    """.split()),
    "fluids": {"lava", "water"},
    "entity_types": {"arrows", "beehive_inhabitors", "impact_projectiles", "raiders", "skeletons"},
}

TAG_FOLDERS = {"blocks", "items", "fluids", "entity_types"}

# Top-level data folders 1.16.5 reads; everything else under data/<ns>/ is a newer registry.
# `hempdustry/` is the mod's own datapack registries (strains, balance), loaded by the mod itself.
DATA_FOLDERS = {"recipes", "loot_tables", "advancements", "tags", "hempdustry", "structures",
                "functions", "predicates", "loot_modifiers"}

LOOT_CONDITIONS = {
    "minecraft:inverted", "minecraft:alternative", "minecraft:random_chance",
    "minecraft:random_chance_with_looting", "minecraft:entity_properties",
    "minecraft:killed_by_player", "minecraft:entity_scores", "minecraft:block_state_property",
    "minecraft:match_tool", "minecraft:table_bonus", "minecraft:survives_explosion",
    "minecraft:damage_source_properties", "minecraft:location_check", "minecraft:weather_check",
    "minecraft:reference", "minecraft:time_check",
}

LOOT_FUNCTIONS = {
    "minecraft:set_count", "minecraft:enchant_with_levels", "minecraft:enchant_randomly",
    "minecraft:set_nbt", "minecraft:furnace_smelt", "minecraft:looting_enchant",
    "minecraft:set_damage", "minecraft:set_attributes", "minecraft:set_name",
    "minecraft:exploration_map", "minecraft:set_stew_effect", "minecraft:copy_name",
    "minecraft:set_contents", "minecraft:limit_count", "minecraft:apply_bonus",
    "minecraft:set_loot_table", "minecraft:explosion_decay", "minecraft:set_lore",
    "minecraft:fill_player_head", "minecraft:copy_nbt", "minecraft:copy_state",
}

LOOT_ENTRIES = {
    "minecraft:empty", "minecraft:item", "minecraft:loot_table", "minecraft:dynamic", "minecraft:tag",
    "minecraft:alternatives", "minecraft:sequence", "minecraft:group",
    "hempdustry:spoiling",
}

RECIPE_TYPES = {
    "minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smelting",
    "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking",
    "minecraft:stonecutting", "minecraft:smithing",
    "hempdustry:crafting_shaped", "hempdustry:crafting_shapeless", "hempdustry:infused_shaped",
    "hempdustry:infused_shapeless", "hempdustry:container_carried", "hempdustry:packing",
    "hempdustry:decarboxylating", "hempdustry:infusing", "hempdustry:pressing",
}

TRIGGERS = {
    "minecraft:inventory_changed", "minecraft:consume_item", "minecraft:item_durability_changed",
    "minecraft:item_used_on_block", "minecraft:placed_block", "minecraft:player_interacted_with_entity",
    "minecraft:tame_animal", "minecraft:recipe_unlocked", "minecraft:impossible", "minecraft:tick",
    "hempdustry:smoke", "hempdustry:harvest_hemp",
}


class ConversionError(Exception):
    pass


def ns(id_):
    return id_ if ":" in id_ else "minecraft:" + id_


# --------------------------------------------------------------------------- ids the output names

class Names:
    """Every minecraft: item, block and tag id the output names, for the check against the jar."""

    def __init__(self):
        self.items = set()
        self.blocks = set()
        self.tags = set()  # (folder, id)
        self.where = {}

    def item(self, id_, where):
        self.items.add(ns(id_))
        self.where.setdefault(("item", ns(id_)), where)

    def block(self, id_, where):
        self.blocks.add(ns(id_))
        self.where.setdefault(("block", ns(id_)), where)

    def tag(self, folder, id_, where):
        self.tags.add((folder, ns(id_)))
        self.where.setdefault(("tag", folder, ns(id_)), where)


# --------------------------------------------------------------------------- predicates

def number(value):
    """A loot number: a plain constant, whole numbers as ints."""
    if isinstance(value, float) and value.is_integer():
        return int(value)
    return value


def number_provider(value, where):
    if isinstance(value, (int, float)):
        return number(value)
    if not isinstance(value, dict):
        raise ConversionError(f"{where}: number provider {value!r}")
    kind = ns(value.get("type", "minecraft:uniform"))
    if kind == "minecraft:constant":
        return number(value["value"])
    if kind == "minecraft:uniform":
        if not all(isinstance(value.get(k), (int, float)) for k in ("min", "max")):
            raise ConversionError(f"{where}: uniform bounds must be plain numbers on 1.16.5")
        return {"type": "minecraft:uniform", "min": number(value["min"]), "max": number(value["max"])}
    if kind == "minecraft:binomial":
        return {"type": "minecraft:binomial", "n": number(value["n"]), "p": value["p"]}
    raise ConversionError(f"{where}: number provider type {kind} has no 1.16.5 form")


class Dropped(Exception):
    """The thing being converted names a dropped id; whoever holds it removes it."""


def item_predicate(pred, where, names):
    """One 1.20.1 item predicate as 1.16.5 predicates: a list, one per item it names."""
    pred = dict(pred)
    items = pred.pop("items", None)
    if "tag" in pred:
        names.tag("items", pred["tag"], where)
    if items is None:
        return [pred]
    if isinstance(items, str):
        items = [items]
    out = []
    for item in items:
        item = ns(item)
        if item in DROPPED_IDS:
            continue
        names.item(item, where)
        one = dict(pred)
        one["item"] = item
        out.append(one)
    if not out:
        raise Dropped(where)
    return out


def any_of(predicates):
    """item_predicate's list as one predicate: itself for one item, the mod's any_of type for several."""
    if len(predicates) == 1:
        return predicates[0]
    rest = {k: v for k, v in predicates[0].items() if k != "item"}
    return {"type": "hempdustry:any_of", "items": [p["item"] for p in predicates], **rest}


def block_predicate(pred, where, names):
    """One 1.20.1 block predicate as 1.16.5 predicates: a list, one per block it names."""
    pred = dict(pred)
    blocks = pred.pop("blocks", None)
    if "tag" in pred:
        names.tag("blocks", pred["tag"], where)
    if blocks is None:
        return [pred]
    if isinstance(blocks, str):
        blocks = [blocks]
    out = []
    for block in blocks:
        block = ns(block)
        if block in DROPPED_IDS:
            continue
        names.block(block, where)
        one = dict(pred)
        one["block"] = block
        out.append(one)
    if not out:
        raise Dropped(where)
    return out


def single(predicates, what, where):
    if len(predicates) != 1:
        raise ConversionError(f"{where}: a 1.16.5 {what} predicate names one; this one names {len(predicates)}")
    return predicates[0]


def location_predicate(pred, where, names):
    """A location predicate as 1.16.5 predicates: one per block its block predicate names."""
    pred = dict(pred)
    if "block" not in pred:
        return [pred]
    out = []
    for block in block_predicate(pred["block"], where, names):
        one = dict(pred)
        one["block"] = block
        out.append(one)
    return out


def entity_predicate(pred, where, names):
    pred = dict(pred)
    if "equipment" in pred:
        pred["equipment"] = {slot: single(item_predicate(p, where, names), "item", where)
                             for slot, p in pred["equipment"].items()}
    if "location" in pred:
        pred["location"] = single(location_predicate(pred["location"], where, names), "location", where)
    return pred


# --------------------------------------------------------------------------- loot conditions

def invert(term):
    return {"condition": "minecraft:inverted", "term": term}


def condition(cond, where, names):
    """One 1.20.1 loot condition as one 1.16.5 condition."""
    cond = dict(cond)
    kind = ns(cond["condition"])
    if kind == "minecraft:any_of":
        return {"condition": "minecraft:alternative",
                "terms": [condition(t, where, names) for t in cond["terms"]]}
    if kind == "minecraft:all_of":
        # not (not a or not b ...): 1.16.5 has no all_of.
        return invert({"condition": "minecraft:alternative",
                       "terms": [invert(condition(t, where, names)) for t in cond["terms"]]})
    if kind not in LOOT_CONDITIONS:
        raise ConversionError(f"{where}: loot condition {kind} has no 1.16.5 form")
    cond["condition"] = kind
    if kind == "minecraft:alternative":
        cond["terms"] = [condition(t, where, names) for t in cond["terms"]]
    elif kind == "minecraft:inverted":
        cond["term"] = condition(cond["term"], where, names)
    elif kind == "minecraft:match_tool" and "predicate" in cond:
        cond["predicate"] = single(item_predicate(cond["predicate"], where, names), "item", where)
    elif kind == "minecraft:location_check" and "predicate" in cond:
        checks = []
        for pred in location_predicate(cond["predicate"], where, names):
            one = dict(cond)
            one["predicate"] = pred
            checks.append(one)
        if len(checks) > 1:
            return {"condition": "minecraft:alternative", "terms": checks}
        return checks[0]
    elif kind == "minecraft:block_state_property":
        if ns(cond["block"]) in DROPPED_IDS:
            raise Dropped(where)
        names.block(cond["block"], where)
    elif kind == "minecraft:entity_properties" and "predicate" in cond:
        cond["predicate"] = entity_predicate(cond["predicate"], where, names)
    return cond


def conditions(conds, where, names):
    """A list of conditions -- an "and" -- with every all_of spliced into it."""
    out = []
    for cond in conds:
        if ns(cond["condition"]) == "minecraft:all_of":
            out.extend(conditions(cond["terms"], where, names))
        else:
            out.append(condition(cond, where, names))
    return out


# --------------------------------------------------------------------------- loot tables

def loot_function(fn, where, names):
    fn = dict(fn)
    kind = ns(fn["function"])
    if kind not in LOOT_FUNCTIONS:
        raise ConversionError(f"{where}: loot function {kind} has no 1.16.5 form")
    fn["function"] = kind
    if "conditions" in fn:
        fn["conditions"] = conditions(fn["conditions"], where, names)
    if kind == "minecraft:set_count":
        if fn.pop("add", False):
            raise ConversionError(f"{where}: set_count with add has no 1.16.5 form")
        fn["count"] = number_provider(fn["count"], where)
    elif kind == "minecraft:set_damage":
        fn["damage"] = number_provider(fn["damage"], where)
    elif kind == "minecraft:copy_state":
        names.block(fn["block"], where)
    return fn


def loot_entry(entry, where, names):
    entry = dict(entry)
    kind = ns(entry["type"])
    if kind not in LOOT_ENTRIES:
        raise ConversionError(f"{where}: loot entry {kind} has no 1.16.5 form")
    entry["type"] = kind
    if "conditions" in entry:
        entry["conditions"] = conditions(entry["conditions"], where, names)
    if "functions" in entry:
        entry["functions"] = [loot_function(f, where, names) for f in entry["functions"]]
    if kind == "minecraft:item":
        if ns(entry["name"]) in DROPPED_IDS:
            raise Dropped(where)
        names.item(entry["name"], where)
    elif kind == "minecraft:tag":
        names.tag("items", entry["name"], where)
    elif kind in ("minecraft:alternatives", "minecraft:sequence", "minecraft:group"):
        entry["children"] = [loot_entry(c, where, names) for c in entry["children"]]
    elif kind == "hempdustry:spoiling":
        entry["child"] = loot_entry(entry["child"], where, names)
        names.item(entry["into"], where)
    return entry


def loot_table(table, where, names):
    table = dict(table)
    table.pop("random_sequence", None)
    if "functions" in table:
        table["functions"] = [loot_function(f, where, names) for f in table["functions"]]
    pools = []
    for pool in table.get("pools", []):
        pool = dict(pool)
        pool["rolls"] = number_provider(pool["rolls"], where)
        if "bonus_rolls" in pool:
            bonus = number_provider(pool["bonus_rolls"], where)
            if bonus == 0:
                del pool["bonus_rolls"]
            elif isinstance(bonus, dict):
                # 1.16.5 reads bonus rolls as a plain range.
                pool["bonus_rolls"] = {"min": bonus["min"], "max": bonus["max"]}
            else:
                pool["bonus_rolls"] = bonus
        if "conditions" in pool:
            pool["conditions"] = conditions(pool["conditions"], where, names)
        if "functions" in pool:
            pool["functions"] = [loot_function(f, where, names) for f in pool["functions"]]
        pool["entries"] = [loot_entry(e, where, names) for e in pool["entries"]]
        pools.append(pool)
    if "pools" in table:
        table["pools"] = pools
    return table


# --------------------------------------------------------------------------- recipes

def ingredient(ing, where, names):
    if isinstance(ing, list):
        out = []
        for one in ing:
            converted = ingredient(one, where, names)
            out.extend(converted if isinstance(converted, list) else [converted])
        return out
    ing = dict(ing)
    if "item" in ing:
        item = ns(ing["item"])
        if item in DROPPED_IDS:
            raise Dropped(where)
        item = INGREDIENT_SUBSTITUTES.get(item, item)
        ing["item"] = item
        names.item(item, where)
    elif "tag" in ing:
        substitute = TAG_REFERENCE_SUBSTITUTES.get(("items", "#" + ns(ing["tag"])))
        if substitute is not None:
            for item in substitute:
                names.item(item, where)
            return [{"item": item} for item in substitute]
        names.tag("items", ing["tag"], where)
    else:
        raise ConversionError(f"{where}: ingredient {ing!r}")
    return ing


def recipe(rec, where, names):
    rec = dict(rec)
    kind = ns(rec["type"])
    if kind not in RECIPE_TYPES:
        raise ConversionError(f"{where}: recipe type {kind} has no 1.16.5 form")
    rec.pop("category", None)
    rec.pop("show_notification", None)
    if kind == "minecraft:smithing_transform":
        raise ConversionError(f"{where}: smithing_transform")
    if "key" in rec:
        rec["key"] = {k: ingredient(v, where, names) for k, v in rec["key"].items()}
    if "ingredients" in rec:
        rec["ingredients"] = [ingredient(i, where, names) for i in rec["ingredients"]]
    for field in ("ingredient", "container", "hemp", "washed_hemp", "scorched_hemp", "base", "addition"):
        if field in rec:
            rec[field] = ingredient(rec[field], where, names)
    if "result" in rec:
        result = rec["result"]
        item = result if isinstance(result, str) else result["item"]
        if ns(item) in DROPPED_IDS:
            raise Dropped(where)
        names.item(item, where)
    return rec


# --------------------------------------------------------------------------- advancements

def criterion(crit, where, names):
    """One 1.20.1 criterion as a list of 1.16.5 criteria (several when an item list is split)."""
    crit = dict(crit)
    trigger = ns(crit["trigger"])
    if trigger not in TRIGGERS:
        raise ConversionError(f"{where}: trigger {trigger} is not known to this script")
    cond = dict(crit.get("conditions", {}))
    for field in ("player", "entity"):
        if isinstance(cond.get(field), list):
            cond[field] = conditions(cond[field], where, names)
        elif isinstance(cond.get(field), dict):
            cond[field] = entity_predicate(cond[field], where, names)

    variants = [cond]
    if trigger == "minecraft:inventory_changed":
        for pred in cond.get("items", []):
            single(item_predicate(pred, where, names), "item", where)
        cond["items"] = [item_predicate(p, where, names)[0] for p in cond.get("items", [])]
    elif trigger in ("minecraft:item_used_on_block", "minecraft:placed_block"):
        location = cond.pop("location", None)
        if isinstance(location, list):
            for c in location:
                kind = ns(c["condition"])
                if kind == "minecraft:location_check":
                    if any(c.get(k) for k in ("offsetX", "offsetY", "offsetZ")):
                        raise ConversionError(f"{where}: an offset location check has no 1.16.5 criterion form")
                    cond["location"] = single(location_predicate(c["predicate"], where, names), "location", where)
                elif kind == "minecraft:match_tool":
                    cond["item"] = single(item_predicate(c["predicate"], where, names), "item", where)
                elif kind == "minecraft:block_state_property" and trigger == "minecraft:placed_block":
                    if ns(c["block"]) in DROPPED_IDS:
                        raise Dropped(where)
                    names.block(c["block"], where)
                    cond["block"] = c["block"]
                    if "properties" in c:
                        cond["state"] = c["properties"]
                else:
                    raise ConversionError(f"{where}: {kind} in a {trigger} location list")
        elif location is not None:
            cond["location"] = single(location_predicate(location, where, names), "location", where)
        if "item" in cond and isinstance(cond["item"], dict) and "items" in cond["item"]:
            cond["item"] = single(item_predicate(cond["item"], where, names), "item", where)
    elif trigger == "hempdustry:harvest_hemp":
        if "bed" in cond:
            cond["bed"] = single(block_predicate(cond["bed"], where, names), "block", where)
    elif trigger == "minecraft:recipe_unlocked":
        pass
    if "item" in cond and trigger in ("minecraft:consume_item", "minecraft:item_durability_changed",
                                      "minecraft:player_interacted_with_entity", "hempdustry:smoke"):
        cond["item"] = any_of(item_predicate(cond["item"], where, names))
    out = []
    for one in variants:
        c = {"trigger": trigger}
        if one:
            c["conditions"] = one
        out.append(c)
    return out


def advancement(adv, where, names, recipes_kept):
    adv = dict(adv)
    adv.pop("sends_telemetry_event", None)
    if "display" in adv:
        icon = adv["display"].get("icon")
        if icon and ns(icon["item"]) in DROPPED_IDS:
            return None
        if icon:
            names.item(icon["item"], where)
    renamed = {}
    criteria = {}
    for name, crit in adv["criteria"].items():
        if ns(crit["trigger"]) == "minecraft:recipe_unlocked" and ns(crit["conditions"]["recipe"]) not in recipes_kept:
            return None  # the recipe it unlocks is gone
        try:
            converted = criterion(crit, f"{where} criterion {name}", names)
        except Dropped:
            renamed[name] = []
            continue
        names_for = [name]
        for extra in converted[1:]:
            item = extra["conditions"]["item"]["item"]
            names_for.append(name + "_" + item.split(":", 1)[1].replace("/", "_"))
        renamed[name] = names_for
        for n, c in zip(names_for, converted):
            criteria[n] = c
    if not criteria:
        return None
    adv["criteria"] = criteria
    if "requirements" in adv:
        groups = []
        for group in adv["requirements"]:
            new = [n for old in group for n in renamed.get(old, [old])]
            if new:
                groups.append(new)
        adv["requirements"] = groups
    if "rewards" in adv and "recipes" in adv["rewards"]:
        adv["rewards"] = dict(adv["rewards"])
        adv["rewards"]["recipes"] = [r for r in adv["rewards"]["recipes"] if ns(r) in recipes_kept]
        if not adv["rewards"]["recipes"]:
            del adv["rewards"]["recipes"]
        if not adv["rewards"]:
            del adv["rewards"]
    return adv


# --------------------------------------------------------------------------- tags

def tag_file(tag, folder, where, names):
    tag = dict(tag)
    values = []
    for value in tag["values"]:
        ref = value["id"] if isinstance(value, dict) else value
        required = not isinstance(value, dict) or value.get("required", True)
        is_tag = ref.startswith("#")
        bare = ns(ref[1:] if is_tag else ref)
        if bare in DROPPED_IDS:
            continue
        if is_tag and (folder, "#" + bare) in TAG_REFERENCE_SUBSTITUTES:
            values.extend(TAG_REFERENCE_SUBSTITUTES[(folder, "#" + bare)])
            for item in TAG_REFERENCE_SUBSTITUTES[(folder, "#" + bare)]:
                (names.item if folder == "items" else names.block)(item, where)
            continue
        if is_tag:
            renamed = TAG_RENAMES.get((folder, bare), bare)
            if renamed.startswith("minecraft:") and not vanilla_tag(folder, renamed) and not ours(folder, renamed):
                continue  # a vanilla tag 1.16.5 does not have
            if renamed != bare:
                value = {"id": "#" + renamed, "required": False} if not required else "#" + renamed
            if required:
                names.tag(folder, renamed, where)
        elif bare.startswith("minecraft:") and not vanilla_id_exists(folder, bare):
            continue  # a vanilla block or item newer than 1.16.5
        elif required:
            (names.item if folder == "items" else names.block)(bare, where)
        values.append(value)
    if not values:
        return None
    tag["values"] = values
    return tag


VANILLA_IDS = {"items": None, "blocks": None}
OUR_TAGS = set()


def vanilla_tag(folder, id_):
    return id_.split(":", 1)[1] in VANILLA_TAGS.get(folder, set())


def ours(folder, id_):
    return (folder, id_) in OUR_TAGS


def vanilla_id_exists(folder, id_):
    known = VANILLA_IDS.get(folder)
    if known is None:
        return id_ not in NEWER_THAN_116
    return id_ in known


# Vanilla ids the 1.20.1 data names that 1.16.5 does not have; used when no client jar is at hand.
NEWER_THAN_116 = {"minecraft:lava_cauldron", "minecraft:copper_ingot", "minecraft:amethyst_shard",
                  "minecraft:tinted_glass", "minecraft:moss_carpet"}


def load_vanilla(jar):
    """The 1.16.5 item and block ids, read off the client jar's item models and blockstates."""
    items, blocks = set(), set()
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if name.startswith("assets/minecraft/models/item/") and name.endswith(".json"):
                items.add("minecraft:" + name.rsplit("/", 1)[1][:-5])
            elif name.startswith("assets/minecraft/blockstates/") and name.endswith(".json"):
                blocks.add("minecraft:" + name.rsplit("/", 1)[1][:-5])
            elif name.startswith("data/minecraft/tags/") and name.endswith(".json"):
                folder, path = name[len("data/minecraft/tags/"):-5].split("/", 1)
                if path not in VANILLA_TAGS.get(folder, set()):
                    raise ConversionError(f"VANILLA_TAGS is missing {folder}/{path} from {jar}")
    VANILLA_IDS["items"] = items
    VANILLA_IDS["blocks"] = blocks


# --------------------------------------------------------------------------- the walk

def read_tree(root):
    """{relative path under data/: (bytes, parsed json or None)}"""
    out = {}
    if not os.path.isdir(root):
        return out
    for dirpath, _, files in os.walk(root):
        for f in files:
            full = os.path.join(dirpath, f)
            rel = os.path.relpath(full, root).replace(os.sep, "/")
            with open(full, "rb") as fh:
                raw = fh.read()
            out[rel] = (raw, json.loads(raw) if f.endswith(".json") else None)
    return out


def resource_id(rel, folder):
    """data path 'hempdustry/recipes/foo/bar.json' -> 'hempdustry:foo/bar'."""
    namespace, rest = rel.split("/", 1)
    return namespace + ":" + rest[len(folder) + 1:-len(".json")]


def convert_tree(files, names, recipes_kept, log):
    """{rel: (raw, obj)} -> {rel: obj or raw bytes}; files 1.16.5 has no use for are left out."""
    out = {}
    for rel in sorted(files):
        raw, obj = files[rel]
        parts = rel.split("/")
        namespace, folder = parts[0], parts[1]
        where = rel
        if folder not in DATA_FOLDERS:
            log.append(f"removed {rel} ({folder} is code or newer than 1.16.5)")
            continue
        if obj is None:
            out[rel] = raw
            continue
        try:
            if folder == "tags":
                tag_folder = parts[2]
                if tag_folder not in TAG_FOLDERS:
                    log.append(f"removed {rel} (1.16.5 loads no {tag_folder} tags)")
                    continue
                if tag_folder == "blocks" and parts[3] == "mineable":
                    log.append(f"removed {rel} (1.16.5 blocks name their tool in code)")
                    continue
                tag_id = namespace + ":" + "/".join(parts[3:])[:-5]
                renamed = TAG_RENAMES.get((tag_folder, tag_id), tag_id)
                if renamed != tag_id:
                    new_rel = "/".join([renamed.split(":")[0], "tags", tag_folder] + renamed.split(":")[1].split("/")) + ".json"
                    log.append(f"moved {rel} to {new_rel} (1.16.5's name for it)")
                    rel = new_rel
                elif namespace == "minecraft" and not vanilla_tag(tag_folder, tag_id):
                    log.append(f"removed {rel} (1.16.5 has no {tag_id} tag)")
                    continue
                converted = tag_file(obj, tag_folder, where, names)
                if converted is None:
                    log.append(f"removed {rel} (nothing in it exists on 1.16.5)")
                    continue
            elif folder == "recipes":
                rid = resource_id(rel, "recipes")
                if rid not in recipes_kept:
                    continue  # logged when recipes_kept was built
                converted = recipe(obj, where, names)
            elif folder == "loot_tables":
                if ns(resource_id(rel, "loot_tables").replace(":blocks/", ":")) in DROPPED_IDS:
                    log.append(f"removed {rel} (its block is not on 1.16.5)")
                    continue
                converted = loot_table(obj, where, names)
            elif folder == "advancements":
                converted = advancement(obj, where, names, recipes_kept)
                if converted is None:
                    log.append(f"removed {rel} (what it was for is not on 1.16.5)")
                    continue
            else:
                converted = obj
        except Dropped:
            log.append(f"removed {rel} (it names an id not on 1.16.5)")
            continue
        out[rel] = (raw, obj, converted)
    return out


def kept_recipes(files, log):
    """The recipe ids that survive: not dropped by name, and naming no dropped id."""
    kept = set()
    for rel, (raw, obj) in files.items():
        parts = rel.split("/")
        if len(parts) < 3 or parts[1] != "recipes" or obj is None:
            continue
        rid = resource_id(rel, "recipes")
        if rid in DROPPED_RECIPES:
            log.append(f"removed {rel} ({DROPPED_RECIPES[rid]})")
            continue
        try:
            recipe(obj, rel, Names())
        except Dropped:
            log.append(f"removed {rel} (it names an id not on 1.16.5)")
            continue
        kept.add(rid)
    return kept


def our_tags(files):
    for rel in files:
        parts = rel.split("/")
        if len(parts) > 3 and parts[1] == "tags":
            if parts[0] != "minecraft":  # a minecraft: tag of ours counts only if vanilla has it
                OUR_TAGS.add((parts[2], parts[0] + ":" + "/".join(parts[3:])[:-5]))


def render(entry, generated):
    raw, obj, converted = entry if isinstance(entry, tuple) else (entry, None, None)
    if obj is None:
        return raw
    # Compared as text: 1 == 1.0 in Python, and a whole number written as an int is a change.
    if json.dumps(converted, sort_keys=True) == json.dumps(obj, sort_keys=True):
        return raw
    if generated:
        return json.dumps(datagen_order(converted), indent=2, ensure_ascii=False).encode("utf-8")
    return (json.dumps(converted, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def datagen_order(value):
    """Keys as vanilla's data providers write them: "type" and "parent" first, the rest sorted."""
    if isinstance(value, dict):
        first = {"type": 0, "parent": 1}
        keys = sorted(value, key=lambda k: (first.get(k, 2), k))
        return {k: datagen_order(value[k]) for k in keys}
    if isinstance(value, list):
        return [datagen_order(v) for v in value]
    return value


def check_names(names):
    if VANILLA_IDS["items"] is None:
        return []
    problems = []
    for item in sorted(names.items):
        if item.startswith("minecraft:") and item not in VANILLA_IDS["items"] and item not in VANILLA_IDS["blocks"]:
            problems.append(f"{names.where[('item', item)]}: {item} is not an item on 1.16.5")
    for block in sorted(names.blocks):
        if block.startswith("minecraft:") and block not in VANILLA_IDS["blocks"]:
            problems.append(f"{names.where[('block', block)]}: {block} is not a block on 1.16.5")
    for folder, tag in sorted(names.tags):
        if tag.startswith("minecraft:") and not vanilla_tag(folder, tag) and not ours(folder, tag):
            problems.append(f"{names.where[('tag', folder, tag)]}: #{tag} is not a {folder} tag on 1.16.5")
    return problems


def main():
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--from", dest="source", default=os.path.join(here, "..", "hempdustry-1.20.1-2.1"))
    parser.add_argument("--client-jar", default=os.path.expanduser(
        "~/.gradle/caches/fabric-loom/1.16.5/minecraft-client.jar"))
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    trees = [("common/src/generated/resources/data", True), ("common/src/main/resources/data", False)]
    source = {g: read_tree(os.path.join(args.source, t)) for t, g in trees}
    if not source[True]:
        sys.exit(f"no 1.20.1 data under {args.source}")
    if os.path.isfile(args.client_jar):
        load_vanilla(args.client_jar)
    else:
        print(f"warning: no client jar at {args.client_jar}; vanilla ids are not checked", file=sys.stderr)

    log = []
    every = {**source[True], **source[False]}
    our_tags(every)
    recipes_kept = kept_recipes(every, log)
    names = Names()
    try:
        converted = {g: convert_tree(source[g], names, recipes_kept, log) for _, g in trees}
    except ConversionError as error:
        sys.exit(f"error: {error}")
    problems = check_names(names)
    if problems:
        sys.exit("error: the output names what 1.16.5 does not have:\n  " + "\n  ".join(problems))

    differs = []
    for tree, generated in trees:
        root = os.path.join(here, tree)
        want = {rel: render(entry, generated) for rel, entry in converted[generated].items()}
        have = read_tree(root)
        # The generated tree is all the script's; the hand-written one only where 1.20.1 has a file.
        stale = set(have) - set(want) if generated else \
            {rel for rel in have if rel in source[False] and rel not in want}
        for rel in sorted(stale):
            differs.append(f"delete {tree}/{rel}")
        for rel, data in sorted(want.items()):
            if rel not in have or have[rel][0] != data:
                differs.append(f"write  {tree}/{rel}")
        if args.check:
            continue
        for rel in stale:
            os.remove(os.path.join(root, rel))
        for rel, data in want.items():
            path = os.path.join(root, rel)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as fh:
                fh.write(data)
        for dirpath, _, _ in os.walk(root, topdown=False):
            if dirpath != root and not os.listdir(dirpath):
                os.rmdir(dirpath)

    for line in log:
        print(line)
    if args.check:
        for line in differs:
            print(line)
        sys.exit(1 if differs else 0)
    print(f"{len(differs)} file(s) changed")


if __name__ == "__main__":
    main()
