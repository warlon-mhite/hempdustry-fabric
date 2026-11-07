#!/usr/bin/env python3
"""Convert the mod's 1.20.1 data files to the formats Minecraft 1.18.2 reads.

The 1.18.2 line has no datagen. Its data is the 1.20.1 line's -- written by that line's Fabric
datagen into `common/src/generated/resources`, by its Forge data run into
`forge/src/generated/resources`, plus the hand-written `common/src/main/resources/data` -- converted
by this script. Re-run it whenever the 1.20.1 line's data changes, and commit both this script and
what it writes.

Usage (inside the dev shell, from this checkout's root):

    python3 scripts/convert_data_118.py [--from ../hempdustry-1.20.1-2.1] [--client-jar JAR] [--check]

  --from        the 1.20.1 checkout whose data is converted (default: ../hempdustry-1.20.1-2.1)
  --client-jar  a 1.18.2 client jar; every minecraft: item, block and tag the output names is
                checked against it, and so is the list of vanilla tags below (default: the one
                ModDevGradle's NeoForm runtime caches, skipped with a warning if absent)
  --check       write nothing; exit 1 if the checkout's data differs from what would be written

What it writes:
  - common/src/generated/resources/data is replaced whole by the converted generated data;
  - each file of the 1.20.1 common/src/main/resources/data and forge/src/generated/resources/data
    is written converted to the same path here, and removed here when 1.18.2 has no use for it.
    Files only this line has in those two trees are left alone.

A file whose content needs no change is copied byte for byte; a changed one is written the way
datagen writes (two-space indent, sorted keys, no final newline) for the two generated trees, and
with its own key order for the hand-written one.

1.18.2 sits between the two lines' formats: number providers, `set_count`'s `add`, item predicates
naming several `items`, block predicates naming several `blocks`, mining tool tags and biome tags
all read on 1.18.2 exactly as 1.20.1 writes them, so they pass through untouched. What changes, and
why (each rule names what 1.18.2 reads instead):

  Content 1.18.2 cannot hold
    - DROPPED_IDS: blocks and items the 1.18.2 line does not register (hanging signs came with
      1.20, the chest boat with 1.19). Their recipes, loot tables and recipe advancements are
      removed, and so is every tag entry, predicate entry and advancement criterion naming them.
      Copper, amethyst and tinted glass all exist on 1.18.2, so the tinted bong stays and no
      ingredient needs a stand-in.
    - DROPPED_RECIPES and INGREDIENT_SUBSTITUTES: recipes left out, and vanilla ingredients
      replaced, when a 1.20.1 ingredient does not exist on 1.18.2. Both are empty today.
    - Worldgen JSON (`worldgen/`): 1.18.2 registers the mod's features in code.
    - Registries 1.18.2 does not have or that the mod registers in code: painting variants (the
      1.18.2 registry is the built-in `motive`), jukebox songs, banner patterns, damage types,
      trims, and their tags. Forge's biome modifiers (`forge/biome_modifier`, Forge 1.19+) go too:
      Forge 40 adds features to biomes from code.
    - Tag folders 1.18.2 does not load (it loads blocks, items, fluids, entity_types, game_events
      and worldgen/biome), and vanilla tags 1.18.2 does not have (nothing would read them).
      TAG_RENAMES maps the few that 1.18.2 has under an older name (`wool_carpets` was `carpets`,
      which is also the tag a 1.18.2 llama takes as its carpet).
    - The mod's own biome tags (`hempdustry/tags/worldgen/biome`): Forge 40 adds a feature to a
      biome as the biomes load, before any tag is bound, so the wild plants' biomes are picked in
      code (`ModTags.Biomes`, the same biomes by id and modded ones by category). A tag file left
      in the jar would look editable and change nothing.
    - A vanilla block, item or biome newer than 1.18.2 named in a tag file is left out of it
      (`mangrove_swamp` came with 1.19); a required entry would make the whole tag fail to load.
    - TAG_REFERENCE_SUBSTITUTES: a vanilla tag 1.18.2 does not have, named in a tag file or a
      recipe ingredient, becomes the items that tag holds which 1.18.2 has (`#minecraft:swords`
      came with 1.19.3).

  Recipes
    - `category` and `show_notification` (1.19.3 and 1.20) are removed; 1.18.2 has neither.

  Loot tables
    - `random_sequence` (1.20) is removed.
    - `minecraft:any_of` is 1.18.2's `minecraft:alternative`. `minecraft:all_of` (1.20) does not
      exist: in a list of conditions -- already an "and" -- its terms are spliced in; anywhere
      else it becomes `inverted(alternative(inverted(term)...))`.

  Advancements
    - `sends_telemetry_event` (1.19) is removed.
    - Criteria whose conditions 1.20 writes as a list of loot conditions read a predicate object
      on 1.18.2: `placed_block` takes `block`/`state` from its `block_state_property`, and
      `location`/`item` from `location_check`/`match_tool`; `item_used_on_block` takes `location`
      and `item` the same way. Entity predicates written as condition lists (`entity`, `player`)
      are read as lists by 1.18.2 too, and only their conditions are converted.
    - A criterion naming only dropped ids is removed, and its requirement with it; an advancement
      left with no criteria, or a recipe advancement whose recipe is gone, is removed.

  Left as they are
    - The mod's own datapack registries under `data/<ns>/hempdustry/` (strains, balance files):
      the mod reads them itself, in the same format on every line.
    - `c:` and `forge:` tags: Forge 40 loads a tag in any namespace.

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
}

# Recipe ids (data/<ns>/recipes/<path>.json) left out, with why.
DROPPED_RECIPES = {
}

# Vanilla items newer than 1.18.2 used as ingredients, and the 1.18.2 item that stands in.
INGREDIENT_SUBSTITUTES = {
}

# Vanilla tags that 1.18.2 has under another name.
TAG_RENAMES = {
    ("blocks", "minecraft:wool_carpets"): "minecraft:carpets",
    ("items", "minecraft:wool_carpets"): "minecraft:carpets",
}

# Vanilla tags 1.18.2 does not have, named by a tag file or a recipe ingredient: the items the
# newer tag holds that 1.18.2 has. A tag file lists them; an ingredient becomes "any of them".
TAG_REFERENCE_SUBSTITUTES = {
    # #minecraft:swords came with 1.19.3; these are the swords it holds that 1.18.2 has.
    ("items", "#minecraft:swords"): [
        "minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword",
        "minecraft:golden_sword", "minecraft:diamond_sword", "minecraft:netherite_sword",
    ],
}

# Vanilla 1.18.2's own tags (data/minecraft/tags in its client jar), checked against the jar.
VANILLA_TAGS = {
    "blocks": set("""
        acacia_logs animals_spawnable_on anvil axolotls_spawnable_on azalea_grows_on
        azalea_root_replaceable bamboo_plantable_on banners base_stone_nether base_stone_overworld
        beacon_base_blocks beds bee_growables beehives big_dripleaf_placeable birch_logs buttons
        campfires candle_cakes candles carpets cauldrons cave_vines climbable coal_ores copper_ores
        coral_blocks coral_plants corals crimson_stems crops crystal_sound_blocks dark_oak_logs
        deepslate_ore_replaceables diamond_ores dirt doors dragon_immune
        dripstone_replaceable_blocks emerald_ores enderman_holdable fall_damage_resetting
        features_cannot_replace fence_gates fences fire flower_pots flowers foxes_spawnable_on
        geode_invalid_blocks goats_spawnable_on gold_ores guarded_by_piglins hoglin_repellents ice
        impermeable infiniburn_end infiniburn_nether infiniburn_overworld inside_step_sound_blocks
        iron_ores jungle_logs lapis_ores lava_pool_stone_cannot_replace leaves logs logs_that_burn
        lush_ground_replaceable mineable/axe mineable/hoe mineable/pickaxe mineable/shovel
        mooshrooms_spawnable_on moss_replaceable mushroom_grow_block needs_diamond_tool
        needs_iron_tool needs_stone_tool non_flammable_wood nylium oak_logs
        occludes_vibration_signals parrots_spawnable_on piglin_repellents planks
        polar_bears_spawnable_on_in_frozen_ocean portals pressure_plates
        prevent_mob_spawning_inside rabbits_spawnable_on rails redstone_ores replaceable_plants sand
        saplings shulker_boxes signs slabs small_dripleaf_placeable small_flowers snow
        soul_fire_base_blocks soul_speed_blocks spruce_logs stairs standing_signs stone_bricks
        stone_ore_replaceables stone_pressure_plates strider_warm_blocks tall_flowers terracotta
        trapdoors underwater_bonemeals unstable_bottom_center valid_spawn wall_corals
        wall_post_override wall_signs walls warped_stems wart_blocks wither_immune
        wither_summon_base_blocks wolves_spawnable_on wooden_buttons wooden_doors wooden_fences
        wooden_pressure_plates wooden_slabs wooden_stairs wooden_trapdoors wool
    """.split()),
    "items": set("""
        acacia_logs anvil arrows axolotl_tempt_items banners beacon_payment_items beds birch_logs
        boats buttons candles carpets cluster_max_harvestables coal_ores coals copper_ores
        creeper_drop_music_discs crimson_stems dark_oak_logs diamond_ores dirt doors emerald_ores
        fences fishes flowers fox_food freeze_immune_wearables gold_ores ignored_by_piglin_babies
        iron_ores jungle_logs lapis_ores leaves lectern_books logs logs_that_burn music_discs
        non_flammable_wood oak_logs occludes_vibration_signals piglin_food piglin_loved
        piglin_repellents planks rails redstone_ores sand saplings signs slabs small_flowers
        soul_fire_base_blocks spruce_logs stairs stone_bricks stone_crafting_materials
        stone_tool_materials tall_flowers terracotta trapdoors walls warped_stems wooden_buttons
        wooden_doors wooden_fences wooden_pressure_plates wooden_slabs wooden_stairs
        wooden_trapdoors wool
    """.split()),
    "fluids": {"lava", "water"},
    "entity_types": set("""
        arrows axolotl_always_hostiles axolotl_hunt_targets beehive_inhabitors
        freeze_hurts_extra_types freeze_immune_entity_types impact_projectiles
        powder_snow_walkable_mobs raiders skeletons
    """.split()),
    "game_events": {"ignore_vibrations_sneaking", "vibrations"},
    "worldgen/biome": set("""
        has_structure/bastion_remnant has_structure/buried_treasure has_structure/desert_pyramid
        has_structure/end_city has_structure/igloo has_structure/jungle_temple
        has_structure/mineshaft has_structure/mineshaft_mesa has_structure/nether_fortress
        has_structure/nether_fossil has_structure/ocean_monument has_structure/ocean_ruin_cold
        has_structure/ocean_ruin_warm has_structure/pillager_outpost
        has_structure/ruined_portal_desert has_structure/ruined_portal_jungle
        has_structure/ruined_portal_mountain has_structure/ruined_portal_nether
        has_structure/ruined_portal_ocean has_structure/ruined_portal_standard
        has_structure/ruined_portal_swamp has_structure/shipwreck has_structure/shipwreck_beached
        has_structure/stronghold has_structure/swamp_hut has_structure/village_desert
        has_structure/village_plains has_structure/village_savanna has_structure/village_snowy
        has_structure/village_taiga has_structure/woodland_mansion is_badlands is_beach
        is_deep_ocean is_forest is_hill is_jungle is_mountain is_nether is_ocean is_river is_taiga
    """.split()),
    # Loaded by 1.18.2, never written by the mod; listed so the jar check sees every folder.
    "worldgen/configured_structure_feature": set("""
        dolphin_located eye_of_ender_located mineshaft ocean_ruin on_ocean_explorer_maps
        on_treasure_maps on_woodland_explorer_maps ruined_portal shipwreck village
    """.split()),
}

# Tag folders the mod's data may ship on 1.18.2.
TAG_FOLDERS = {"blocks", "items", "fluids", "entity_types", "game_events", "worldgen/biome"}

# Vanilla 1.18.2's biomes (net.minecraft.world.level.biome.Biomes; built in, so the client jar
# carries no file per biome to read them from).
VANILLA_BIOMES = {"minecraft:" + b for b in """
    the_void plains sunflower_plains snowy_plains ice_spikes desert swamp forest flower_forest
    birch_forest dark_forest old_growth_birch_forest old_growth_pine_taiga old_growth_spruce_taiga
    taiga snowy_taiga savanna savanna_plateau windswept_hills windswept_gravelly_hills
    windswept_forest windswept_savanna jungle sparse_jungle bamboo_jungle badlands eroded_badlands
    wooded_badlands meadow grove snowy_slopes frozen_peaks jagged_peaks stony_peaks river
    frozen_river beach snowy_beach stony_shore warm_ocean lukewarm_ocean deep_lukewarm_ocean ocean
    deep_ocean cold_ocean deep_cold_ocean frozen_ocean deep_frozen_ocean mushroom_fields
    dripstone_caves lush_caves nether_wastes warped_forest crimson_forest soul_sand_valley
    basalt_deltas the_end end_highlands end_midlands small_end_islands end_barrens
""".split()}

# Top-level data folders 1.18.2 reads; everything else under data/<ns>/ is a newer registry, or
# one the mod fills in code. `hempdustry/` is the mod's own datapack registries (strains,
# balance), loaded by the mod itself; `loot_modifiers/` is Forge's.
DATA_FOLDERS = {"recipes", "loot_tables", "advancements", "tags", "hempdustry", "structures",
                "functions", "predicates", "item_modifiers", "loot_modifiers"}

LOOT_CONDITIONS = {
    "minecraft:inverted", "minecraft:alternative", "minecraft:random_chance",
    "minecraft:random_chance_with_looting", "minecraft:entity_properties",
    "minecraft:killed_by_player", "minecraft:entity_scores", "minecraft:block_state_property",
    "minecraft:match_tool", "minecraft:table_bonus", "minecraft:survives_explosion",
    "minecraft:damage_source_properties", "minecraft:location_check", "minecraft:weather_check",
    "minecraft:reference", "minecraft:time_check", "minecraft:value_check",
}

LOOT_FUNCTIONS = {
    "minecraft:set_count", "minecraft:enchant_with_levels", "minecraft:enchant_randomly",
    "minecraft:set_enchantments", "minecraft:set_nbt", "minecraft:furnace_smelt",
    "minecraft:looting_enchant", "minecraft:set_damage", "minecraft:set_attributes",
    "minecraft:set_name", "minecraft:exploration_map", "minecraft:set_stew_effect",
    "minecraft:copy_name", "minecraft:set_contents", "minecraft:limit_count",
    "minecraft:apply_bonus", "minecraft:set_loot_table", "minecraft:explosion_decay",
    "minecraft:set_lore", "minecraft:fill_player_head", "minecraft:copy_nbt",
    "minecraft:copy_state", "minecraft:set_banner_pattern", "minecraft:set_potion",
}

LOOT_ENTRIES = {
    "minecraft:empty", "minecraft:item", "minecraft:loot_table", "minecraft:dynamic", "minecraft:tag",
    "minecraft:alternatives", "minecraft:sequence", "minecraft:group",
    "hempdustry:spoiling",
}

LOOT_TABLE_TYPES = {
    "minecraft:empty", "minecraft:chest", "minecraft:command", "minecraft:selector",
    "minecraft:fishing", "minecraft:entity", "minecraft:gift", "minecraft:barter",
    "minecraft:advancement_reward", "minecraft:advancement_entity", "minecraft:generic",
    "minecraft:block",
}

NUMBER_PROVIDERS = {"minecraft:constant", "minecraft:uniform", "minecraft:binomial", "minecraft:score"}

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
    """Every minecraft: item, block, biome and tag id the output names, for the check against the jar."""

    def __init__(self):
        self.ids = {"items": set(), "blocks": set(), "worldgen/biome": set()}
        self.tags = set()  # (folder, id)
        self.where = {}

    def add(self, folder, id_, where):
        self.ids[folder].add(ns(id_))
        self.where.setdefault((folder, ns(id_)), where)

    def item(self, id_, where):
        self.add("items", id_, where)

    def block(self, id_, where):
        self.add("blocks", id_, where)

    def tag(self, folder, id_, where):
        self.tags.add((folder, ns(id_)))
        self.where.setdefault(("tag", folder, ns(id_)), where)


# --------------------------------------------------------------------------- predicates

class Dropped(Exception):
    """The thing being converted names only dropped ids; whoever holds it removes it."""


def number_provider(value, where):
    """Checked, not changed: 1.18.2 reads 1.20.1's number providers as they are."""
    if isinstance(value, dict):
        kind = ns(value.get("type", "minecraft:uniform"))
        if kind not in NUMBER_PROVIDERS:
            raise ConversionError(f"{where}: number provider type {kind} has no 1.18.2 form")
        for key, inner in value.items():
            if key in ("min", "max", "n"):
                number_provider(inner, where)
    elif not isinstance(value, (int, float)):
        raise ConversionError(f"{where}: number provider {value!r}")
    return value


def id_list_predicate(pred, key, folder, where, names):
    """An item or block predicate with its dropped ids taken out of its `items`/`blocks` list."""
    pred = dict(pred)
    if "tag" in pred:
        names.tag(folder, pred["tag"], where)
    if key not in pred:
        return pred
    ids = pred[key]
    if not isinstance(ids, list):
        raise ConversionError(f"{where}: {key} {ids!r} is not a list")
    kept = [i for i in ids if ns(i) not in DROPPED_IDS]
    if not kept:
        raise Dropped(where)
    for i in kept:
        names.add(folder, i, where)
    pred[key] = kept
    return pred


def item_predicate(pred, where, names):
    return id_list_predicate(pred, "items", "items", where, names)


def block_predicate(pred, where, names):
    return id_list_predicate(pred, "blocks", "blocks", where, names)


def location_predicate(pred, where, names):
    pred = dict(pred)
    if "block" in pred:
        pred["block"] = block_predicate(pred["block"], where, names)
    if "biome" in pred:
        names.add("worldgen/biome", pred["biome"], where)
    return pred


def entity_predicate(pred, where, names):
    pred = dict(pred)
    if "equipment" in pred:
        pred["equipment"] = {slot: item_predicate(p, where, names) for slot, p in pred["equipment"].items()}
    if "location" in pred:
        pred["location"] = location_predicate(pred["location"], where, names)
    return pred


# --------------------------------------------------------------------------- loot conditions

def invert(term):
    return {"condition": "minecraft:inverted", "term": term}


def condition(cond, where, names):
    """One 1.20.1 loot condition as one 1.18.2 condition."""
    cond = dict(cond)
    kind = ns(cond["condition"])
    if kind == "minecraft:any_of":
        return {"condition": "minecraft:alternative",
                "terms": [condition(t, where, names) for t in cond["terms"]]}
    if kind == "minecraft:all_of":
        # not (not a or not b ...): 1.18.2 has no all_of.
        return invert({"condition": "minecraft:alternative",
                       "terms": [invert(condition(t, where, names)) for t in cond["terms"]]})
    if kind not in LOOT_CONDITIONS:
        raise ConversionError(f"{where}: loot condition {kind} has no 1.18.2 form")
    cond["condition"] = kind
    if kind == "minecraft:alternative":
        cond["terms"] = [condition(t, where, names) for t in cond["terms"]]
    elif kind == "minecraft:inverted":
        cond["term"] = condition(cond["term"], where, names)
    elif kind == "minecraft:match_tool" and "predicate" in cond:
        cond["predicate"] = item_predicate(cond["predicate"], where, names)
    elif kind == "minecraft:location_check" and "predicate" in cond:
        cond["predicate"] = location_predicate(cond["predicate"], where, names)
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
        raise ConversionError(f"{where}: loot function {kind} has no 1.18.2 form")
    fn["function"] = kind
    if "conditions" in fn:
        fn["conditions"] = conditions(fn["conditions"], where, names)
    if kind == "minecraft:set_count":
        number_provider(fn["count"], where)
    elif kind == "minecraft:set_damage":
        number_provider(fn["damage"], where)
    elif kind == "minecraft:copy_state":
        names.block(fn["block"], where)
    return fn


def loot_entry(entry, where, names):
    entry = dict(entry)
    kind = ns(entry["type"])
    if kind not in LOOT_ENTRIES:
        raise ConversionError(f"{where}: loot entry {kind} has no 1.18.2 form")
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
    if "type" in table and ns(table["type"]) not in LOOT_TABLE_TYPES:
        raise ConversionError(f"{where}: loot table type {table['type']} has no 1.18.2 form")
    if "functions" in table:
        table["functions"] = [loot_function(f, where, names) for f in table["functions"]]
    pools = []
    for pool in table.get("pools", []):
        pool = dict(pool)
        number_provider(pool["rolls"], where)
        if "bonus_rolls" in pool:
            number_provider(pool["bonus_rolls"], where)
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
        if item in INGREDIENT_SUBSTITUTES:
            ing["item"] = item = INGREDIENT_SUBSTITUTES[item]
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
        raise ConversionError(f"{where}: recipe type {kind} has no 1.18.2 form")
    rec.pop("category", None)
    rec.pop("show_notification", None)
    if kind == "minecraft:smithing" and "template" in rec:
        raise ConversionError(f"{where}: a smithing template has no 1.18.2 form")
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

def location_list(trigger, location, cond, where, names):
    """A 1.20 criterion's `location` condition list as the fields 1.18.2's criterion reads."""
    for c in location:
        kind = ns(c["condition"])
        if kind == "minecraft:location_check":
            if any(c.get(k) for k in ("offsetX", "offsetY", "offsetZ")):
                raise ConversionError(f"{where}: an offset location check has no 1.18.2 criterion form")
            if "location" in cond:
                raise ConversionError(f"{where}: two location checks in one {trigger} criterion")
            cond["location"] = location_predicate(c["predicate"], where, names)
        elif kind == "minecraft:match_tool":
            if "item" in cond:
                raise ConversionError(f"{where}: two item checks in one {trigger} criterion")
            cond["item"] = item_predicate(c["predicate"], where, names)
        elif kind == "minecraft:block_state_property" and trigger == "minecraft:placed_block":
            if ns(c["block"]) in DROPPED_IDS:
                raise Dropped(where)
            names.block(c["block"], where)
            cond["block"] = c["block"]
            if "properties" in c:
                cond["state"] = c["properties"]
        else:
            raise ConversionError(f"{where}: {kind} in a {trigger} location list")


def criterion(crit, where, names):
    crit = dict(crit)
    trigger = ns(crit["trigger"])
    if trigger not in TRIGGERS:
        raise ConversionError(f"{where}: trigger {trigger} is not known to this script")
    crit["trigger"] = trigger
    if "conditions" not in crit:
        return crit
    cond = dict(crit["conditions"])
    for field in ("player", "entity"):
        if isinstance(cond.get(field), list):
            cond[field] = conditions(cond[field], where, names)
        elif isinstance(cond.get(field), dict):
            cond[field] = entity_predicate(cond[field], where, names)

    if trigger == "minecraft:inventory_changed":
        cond["items"] = [item_predicate(p, where, names) for p in cond.get("items", [])]
    elif trigger in ("minecraft:item_used_on_block", "minecraft:placed_block"):
        location = cond.pop("location", None)
        if isinstance(location, list):
            location_list(trigger, location, cond, where, names)
        elif location is not None:
            cond["location"] = location_predicate(location, where, names)
    elif trigger == "hempdustry:harvest_hemp":
        if "bed" in cond:
            cond["bed"] = block_predicate(cond["bed"], where, names)
    if "item" in cond and trigger in ("minecraft:consume_item", "minecraft:item_durability_changed",
                                      "minecraft:player_interacted_with_entity", "hempdustry:smoke"):
        cond["item"] = item_predicate(cond["item"], where, names)
    crit["conditions"] = cond
    return crit


def advancement(adv, where, names, recipes_kept):
    adv = dict(adv)
    adv.pop("sends_telemetry_event", None)
    if "display" in adv:
        icon = adv["display"].get("icon")
        if icon and ns(icon["item"]) in DROPPED_IDS:
            return None
        if icon:
            names.item(icon["item"], where)
    gone = set()
    criteria = {}
    for name, crit in adv["criteria"].items():
        if ns(crit["trigger"]) == "minecraft:recipe_unlocked" and ns(crit["conditions"]["recipe"]) not in recipes_kept:
            return None  # the recipe it unlocks is gone
        try:
            criteria[name] = criterion(crit, f"{where} criterion {name}", names)
        except Dropped:
            gone.add(name)
    if not criteria:
        return None
    adv["criteria"] = criteria
    if "requirements" in adv:
        groups = []
        for group in adv["requirements"]:
            new = [n for n in group if n not in gone]
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
            for item in TAG_REFERENCE_SUBSTITUTES[(folder, "#" + bare)]:
                names.add(folder, item, where)
                values.append(item)
            continue
        if is_tag:
            renamed = TAG_RENAMES.get((folder, bare), bare)
            if renamed.startswith("minecraft:") and not vanilla_tag(folder, renamed) and not ours(folder, renamed):
                continue  # a vanilla tag 1.18.2 does not have
            if renamed != bare:
                value = {"id": "#" + renamed, "required": False} if not required else "#" + renamed
            if required:
                names.tag(folder, renamed, where)
        elif bare.startswith("minecraft:") and not vanilla_id_exists(folder, bare):
            continue  # a vanilla block, item or biome newer than 1.18.2
        elif required and folder in names.ids:
            names.add(folder, bare, where)
        values.append(value)
    if not values:
        return None
    tag["values"] = values
    return tag


VANILLA_IDS = {"items": None, "blocks": None, "worldgen/biome": VANILLA_BIOMES}
OUR_TAGS = set()


def vanilla_tag(folder, id_):
    return id_.split(":", 1)[1] in VANILLA_TAGS.get(folder, set())


def ours(folder, id_):
    return (folder, id_) in OUR_TAGS


def vanilla_id_exists(folder, id_):
    known = VANILLA_IDS.get(folder)
    if known is None:
        return id_ not in NEWER_THAN_118
    return id_ in known


# Vanilla items and blocks the 1.20.1 data names that 1.18.2 does not have; used when no client
# jar is at hand. (Biomes are checked against VANILLA_BIOMES either way.)
NEWER_THAN_118 = set()


def load_vanilla(jar):
    """The 1.18.2 item and block ids, read off the client jar's item models and blockstates; and
    VANILLA_TAGS held to the jar's own tag files, both ways."""
    items, blocks, tags = set(), set(), set()
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if name.startswith("assets/minecraft/models/item/") and name.endswith(".json"):
                items.add("minecraft:" + name.rsplit("/", 1)[1][:-5])
            elif name.startswith("assets/minecraft/blockstates/") and name.endswith(".json"):
                blocks.add("minecraft:" + name.rsplit("/", 1)[1][:-5])
            elif name.startswith("data/minecraft/tags/") and name.endswith(".json"):
                path = name[len("data/minecraft/tags/"):-5]
                folder, rest = path.split("/", 1)
                if folder == "worldgen":
                    sub, rest = rest.split("/", 1)
                    folder += "/" + sub
                tags.add((folder, rest))
    listed = {(folder, path) for folder, paths in VANILLA_TAGS.items() for path in paths}
    for folder, path in sorted(tags - listed):
        raise ConversionError(f"VANILLA_TAGS is missing {folder}/{path} from {jar}")
    for folder, path in sorted(listed - tags):
        raise ConversionError(f"VANILLA_TAGS lists {folder}/{path}, which {jar} does not have")
    if not items or not blocks:
        raise ConversionError(f"{jar} has no item models or blockstates; is it a client jar?")
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


def tag_parts(parts):
    """data path parts of a tag file -> (tag folder, path parts of the tag id)."""
    if parts[2] == "worldgen" and len(parts) > 4:
        return "worldgen/" + parts[3], parts[4:]
    return parts[2], parts[3:]


def convert_tree(files, names, recipes_kept, log):
    """{rel: (raw, obj)} -> {rel: entry}; files 1.18.2 has no use for are left out."""
    out = {}
    for rel in sorted(files):
        raw, obj = files[rel]
        parts = rel.split("/")
        namespace, folder = parts[0], parts[1]
        where = rel
        if folder not in DATA_FOLDERS:
            log.append(f"removed {rel} ({folder} is code or newer than 1.18.2)")
            continue
        if obj is None:
            out[rel] = raw
            continue
        try:
            if folder == "tags":
                tag_folder, path = tag_parts(parts)
                if tag_folder not in TAG_FOLDERS:
                    log.append(f"removed {rel} (1.18.2 loads no {tag_folder} tags)")
                    continue
                if tag_folder == "worldgen/biome" and namespace == "hempdustry":
                    log.append(f"removed {rel} (the wild plants' biomes are picked in code on 1.18.2)")
                    continue
                tag_id = namespace + ":" + "/".join(path)[:-5]
                renamed = TAG_RENAMES.get((tag_folder, tag_id), tag_id)
                if renamed != tag_id:
                    new_ns, new_path = renamed.split(":", 1)
                    new_rel = "/".join([new_ns, "tags", tag_folder, new_path]) + ".json"
                    log.append(f"moved {rel} to {new_rel} (1.18.2's name for it)")
                    rel = new_rel
                elif namespace == "minecraft" and not vanilla_tag(tag_folder, tag_id):
                    log.append(f"removed {rel} (1.18.2 has no {tag_id} tag)")
                    continue
                converted = tag_file(obj, tag_folder, where, names)
                if converted is None:
                    log.append(f"removed {rel} (nothing in it exists on 1.18.2)")
                    continue
            elif folder == "recipes":
                if resource_id(rel, "recipes") not in recipes_kept:
                    continue  # logged when recipes_kept was built
                converted = recipe(obj, where, names)
            elif folder == "loot_tables":
                if ns(resource_id(rel, "loot_tables").replace(":blocks/", ":")) in DROPPED_IDS:
                    log.append(f"removed {rel} (its block is not on 1.18.2)")
                    continue
                converted = loot_table(obj, where, names)
            elif folder == "advancements":
                converted = advancement(obj, where, names, recipes_kept)
                if converted is None:
                    log.append(f"removed {rel} (what it was for is not on 1.18.2)")
                    continue
            else:
                converted = obj
        except Dropped:
            log.append(f"removed {rel} (it names an id not on 1.18.2)")
            continue
        out[rel] = (raw, obj, converted)
    return out


def kept_recipes(files, log):
    """The recipe ids that survive: not dropped by name, and naming no dropped id."""
    kept = set()
    for rel, (raw, obj) in sorted(files.items()):
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
            log.append(f"removed {rel} (it names an id not on 1.18.2)")
            continue
        kept.add(rid)
    return kept


def our_tags(files):
    for rel in files:
        parts = rel.split("/")
        if len(parts) > 3 and parts[1] == "tags" and parts[0] != "minecraft":
            # a minecraft: tag of ours counts only if vanilla has it
            folder, path = tag_parts(parts)
            OUR_TAGS.add((folder, parts[0] + ":" + "/".join(path)[:-5]))


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
    problems = []
    what = {"items": "an item", "blocks": "a block", "worldgen/biome": "a biome"}
    for folder, ids in names.ids.items():
        known = VANILLA_IDS[folder]
        if known is None:
            continue
        for id_ in sorted(ids):
            # A block's id also names its item, so a blockstate counts for an item.
            ok =id_ in known or (folder == "items" and id_ in VANILLA_IDS["blocks"])
            if id_.startswith("minecraft:") and not ok:
                problems.append(f"{names.where[(folder, id_)]}: {id_} is not {what[folder]} on 1.18.2")
    for folder, tag in sorted(names.tags):
        if tag.startswith("minecraft:") and not vanilla_tag(folder, tag) and not ours(folder, tag):
            problems.append(f"{names.where[('tag', folder, tag)]}: #{tag} is not a {folder} tag on 1.18.2")
    return problems


# (path, written the way datagen writes, the whole tree is this script's)
TREES = [
    ("common/src/generated/resources/data", True, True),
    ("common/src/main/resources/data", False, False),
    ("forge/src/generated/resources/data", True, False),
]


def main():
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--from", dest="source", default=os.path.join(here, "..", "hempdustry-1.20.1-2.1"))
    parser.add_argument("--client-jar", default=os.path.expanduser(
        "~/.gradle/caches/neoformruntime/artifacts/minecraft_1.18.2_client.jar"))
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    source = {t: read_tree(os.path.join(args.source, t)) for t, _, _ in TREES}
    if not source[TREES[0][0]]:
        sys.exit(f"no 1.20.1 data under {args.source}")
    try:
        if os.path.isfile(args.client_jar):
            load_vanilla(args.client_jar)
        else:
            print(f"warning: no client jar at {args.client_jar}; vanilla ids are not checked", file=sys.stderr)

        log = []
        every = {rel: entry for tree in source.values() for rel, entry in tree.items()}
        our_tags(every)
        recipes_kept = kept_recipes(every, log)
        names = Names()
        converted = {t: convert_tree(source[t], names, recipes_kept, log) for t, _, _ in TREES}
    except ConversionError as error:
        sys.exit(f"error: {error}")
    problems = check_names(names)
    if problems:
        sys.exit("error: the output names what 1.18.2 does not have:\n  " + "\n  ".join(problems))

    differs = []
    for tree, generated, whole in TREES:
        root = os.path.join(here, tree)
        want = {rel: render(entry, generated) for rel, entry in converted[tree].items()}
        have = read_tree(root)
        # A tree that is all the script's loses whatever it would not write; the others only
        # where 1.20.1 has a file.
        stale = set(have) - set(want) if whole else \
            {rel for rel in have if rel in source[tree] and rel not in want}
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
