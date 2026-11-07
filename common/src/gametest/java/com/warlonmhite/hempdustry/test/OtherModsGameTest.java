package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TallHemp;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What other mods read off our plants, held to what they expect of any crop.
 *
 * <p>Harvesters in other mods (Industrial Foregoing's plant gatherer, Create's mechanical harvester) take any
 * {@code CropBlock} whose {@code isMaxAge} holds, one block at a time; when the blocks above a tall plant's
 * root claimed to be ripe, they cut a growing plant down to its root for no drops. And mods that react to
 * growth hear it through the loader's crop-growth events, which a crop with its own growth step has to fire
 * itself.
 */
public final class OtherModsGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();

    /** A random that lands every roll, so one random tick certainly grows the plant. */
    private static final RandomSource HIT = new SingleThreadedRandomSource(0L) {
        @Override
        public float nextFloat() {
            return 0.0F;
        }
    };

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyBlockOfAPlantTellsItsTrueAge(GameTestHelper context) {
        Map<String, String> findings = new TreeMap<>();
        int seen = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof CropBlock crop) || !Sweep.ours(BuiltInRegistries.BLOCK.getKey(block))) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (!state.hasProperty(BlockStateProperties.AGE_7)) {
                    findings.put(state.toString(), "a crop of ours without the age property this sweep reads");
                    continue;
                }
                boolean ripe = state.getValue(BlockStateProperties.AGE_7) >= crop.getMaxAge();
                if (crop.isMaxAge(state) != ripe) {
                    findings.put(state.toString(), "isMaxAge says " + crop.isMaxAge(state) + " at age "
                            + state.getValue(BlockStateProperties.AGE_7));
                }
                seen++;
            }
        }
        Sweep.atLeast(context, "crop block states", seen, 100);
        Sweep.report(context, "every block of a crop of ours answers isMaxAge with its own age", findings, Map.of());
        context.succeed();
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void onlyTheRootIsTheRoot(GameTestHelper context) {
        for (Block block : List.of(ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP, ModBlocks.BELDIA_CROP)) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                boolean lower = state.hasProperty(SativaCropBlock.SEGMENT)
                        ? state.getValue(SativaCropBlock.SEGMENT) == TriplePlantSegment.LOWER
                        : state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
                context.assertTrue(TallHemp.isRoot(state) == lower, "TallHemp.isRoot is " + TallHemp.isRoot(state)
                        + " for " + state);
            }
        }
        context.assertTrue(TallHemp.isRoot(Blocks.WHEAT.defaultBlockState()), "wheat is not its own root");
        context.succeed();
    }

    /**
     * Every recipe we ship for another mod's machine names that mod in each loader's conditions, so without it
     * the file is never parsed (its recipe type would not exist, and the log gate would fail on the error) --
     * and so a loader never loads it on the other's key alone. A file may instead be turned off for one loader
     * ({@code neoforge:false}, or Fabric's {@code not} of {@code true}) when the partner writes it differently there, as Farmer's
     * Delight writes its knife; then the other loader must wait for the mod, and a twin with the same type,
     * ingredients and results must load on the loader this one is off for, so no recipe exists on one loader
     * only by accident. A file both loaders turn off is dead data.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyRecipeForAnotherModWaitsForIt(GameTestHelper context) {
        Map<String, String> findings = new TreeMap<>();
        // What a loader-only file needs a twin of: the recipe without its conditions and its loader's own form.
        Map<String, Set<String>> loadedOnNeoForgeOnly = new TreeMap<>();
        Map<String, Set<String>> loadedOnFabricOnly = new TreeMap<>();
        int seen = 0;
        for (Map.Entry<String, Path> file : Sweep.modFiles().entrySet()) {
            String path = file.getKey();
            if (!path.startsWith("data/") || !path.contains("/recipe/") || !path.endsWith(".json")) {
                continue;
            }
            JsonObject recipe = Sweep.json(file.getValue()).getAsJsonObject();
            String type = recipe.has("type") ? recipe.get("type").getAsString() : "";
            String mod = type.contains(":") ? type.substring(0, type.indexOf(':')) : "minecraft";
            // A crafting-table recipe for another mod's item (its rope, its sack) waits for that mod too: without
            // it the result names an item that does not exist.
            if (mod.equals("minecraft") && recipe.has("result") && recipe.get("result").isJsonObject()
                    && recipe.getAsJsonObject("result").has("id")) {
                String result = recipe.getAsJsonObject("result").get("id").getAsString();
                mod = result.contains(":") ? result.substring(0, result.indexOf(':')) : "minecraft";
            }
            if (mod.equals("minecraft") || mod.equals(Hempdustry.MOD_ID)) {
                continue;
            }
            seen++;
            Gate neo = neoForgeGate(recipe.get("neoforge:conditions"), mod);
            Gate fabric = fabricGate(recipe.get("fabric:load_conditions"), mod);
            if (neo == Gate.OTHER) {
                findings.put(path, "neoforge:conditions neither wait for " + mod + " nor turn the file off: "
                        + recipe.get("neoforge:conditions"));
            } else if (fabric == Gate.OTHER) {
                findings.put(path, "fabric:load_conditions neither wait for " + mod + " nor turn the file off: "
                        + recipe.get("fabric:load_conditions"));
            } else if (neo == Gate.OFF && fabric == Gate.OFF) {
                findings.put(path, "both loaders turn it off: dead data");
            } else if (neo == Gate.OFF) {
                loadedOnFabricOnly.computeIfAbsent(twinKey(recipe), k -> new TreeSet<>()).add(path);
            } else if (fabric == Gate.OFF) {
                loadedOnNeoForgeOnly.computeIfAbsent(twinKey(recipe), k -> new TreeSet<>()).add(path);
            }
        }
        int twins = 0;
        for (Map.Entry<String, Set<String>> neoOnly : loadedOnNeoForgeOnly.entrySet()) {
            if (loadedOnFabricOnly.containsKey(neoOnly.getKey())) {
                twins++;
            } else {
                neoOnly.getValue().forEach(path -> findings.put(path, "loads on NeoForge only, and no file "
                        + "loading on Fabric only has its type, ingredients and results"));
            }
        }
        for (Map.Entry<String, Set<String>> fabricOnly : loadedOnFabricOnly.entrySet()) {
            if (!loadedOnNeoForgeOnly.containsKey(fabricOnly.getKey())) {
                fabricOnly.getValue().forEach(path -> findings.put(path, "loads on Fabric only, and no file "
                        + "loading on NeoForge only has its type, ingredients and results"));
            }
        }
        Sweep.atLeast(context, "recipes for other mods' machines", seen, 20);
        Sweep.report(context, "every recipe for another mod's machine loads only with that mod", findings, Map.of());
        Sweep.atLeast(context, "recipes for another mod written once per loader", twins, 2);
        context.succeed();
    }

    /** What one loader's conditions do with a file: wait for the mod, turn it off, or something else. */
    private enum Gate { WAITS, OFF, OTHER }

    private static Gate neoForgeGate(JsonElement conditions, String mod) {
        if (conditions == null || !conditions.isJsonArray()) {
            return Gate.OTHER;
        }
        for (JsonElement condition : conditions.getAsJsonArray()) {
            JsonObject c = condition.getAsJsonObject();
            String type = c.has("type") ? c.get("type").getAsString() : "";
            if (type.equals("neoforge:mod_loaded") && c.has("modid") && c.get("modid").getAsString().equals(mod)) {
                return Gate.WAITS;
            }
            if (type.equals("neoforge:false")) {
                return Gate.OFF;
            }
        }
        return Gate.OTHER;
    }

    private static Gate fabricGate(JsonElement conditions, String mod) {
        if (conditions == null || !conditions.isJsonArray()) {
            return Gate.OTHER;
        }
        for (JsonElement condition : conditions.getAsJsonArray()) {
            JsonObject c = condition.getAsJsonObject();
            String type = c.has("condition") ? c.get("condition").getAsString() : "";
            if (type.equals("fabric:all_mods_loaded") && c.has("values")
                    && c.getAsJsonArray("values").toString().contains("\"" + mod + "\"")) {
                return Gate.WAITS;
            }
            JsonObject inner = c.has("value") && c.get("value").isJsonObject() ? c.getAsJsonObject("value") : null;
            if (type.equals("fabric:not") && inner != null && inner.has("condition")
                    && inner.get("condition").getAsString().equals("fabric:true")) {
                return Gate.OFF;
            }
        }
        return Gate.OTHER;
    }

    /** A recipe's type, ingredients and results: what a file written once per loader must share with its twin. */
    private static String twinKey(JsonObject recipe) {
        StringBuilder key = new StringBuilder();
        for (String field : List.of("type", "ingredients", "ingredient", "result", "results")) {
            key.append(field).append('=').append(recipe.get(field)).append(';');
        }
        return key.toString();
    }

    /**
     * Every recipe type of ours with fields has a KubeJS schema, and the schema names exactly the fields its
     * recipes are written with: without one a pack's scripts cannot rewrite our recipes, and a schema that
     * misses a field the format later gains would drop it from every recipe a script touches. A schema made
     * from vanilla's grid schemas names its parent instead.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyRecipeTypeOfOursHasAKubeJsSchema(GameTestHelper context) {
        Map<String, Set<String>> fields = new TreeMap<>();
        Map<String, JsonObject> schemas = new TreeMap<>();
        for (Map.Entry<String, Path> file : Sweep.modFiles().entrySet()) {
            String path = file.getKey();
            if (path.startsWith("data/" + Hempdustry.MOD_ID + "/kubejs/recipe_schema/") && path.endsWith(".json")) {
                String type = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
                schemas.put(Hempdustry.MOD_ID + ":" + type, Sweep.json(file.getValue()).getAsJsonObject());
            } else if (path.startsWith("data/") && path.contains("/recipe/") && path.endsWith(".json")) {
                JsonObject recipe = Sweep.json(file.getValue()).getAsJsonObject();
                String type = recipe.has("type") ? recipe.get("type").getAsString() : "";
                if (type.startsWith(Hempdustry.MOD_ID + ":")) {
                    Set<String> keys = fields.computeIfAbsent(type, t -> new TreeSet<>());
                    recipe.keySet().stream().filter(k -> !k.equals("type") && !k.contains("conditions")).forEach(keys::add);
                }
            }
        }
        Map<String, String> findings = new TreeMap<>();
        for (Map.Entry<String, Set<String>> type : fields.entrySet()) {
            if (type.getValue().isEmpty() || type.getValue().equals(Set.of("category"))) {
                continue; // a special recipe with nothing to rewrite (packing)
            }
            JsonObject schema = schemas.get(type.getKey());
            if (schema == null) {
                findings.put(type.getKey(), "no kubejs/recipe_schema file, though its recipes have " + type.getValue());
                continue;
            }
            if (!schema.has("keys")) {
                String parent = schema.has("parent") ? schema.get("parent").getAsString() : "";
                if (!parent.equals("minecraft:shaped") && !parent.equals("minecraft:shapeless")) {
                    findings.put(type.getKey(), "a schema with neither keys nor a vanilla grid parent: " + schema);
                }
                continue;
            }
            Set<String> named = new TreeSet<>();
            schema.getAsJsonArray("keys").forEach(k -> named.add(k.getAsJsonObject().get("name").getAsString()));
            named.removeIf(k -> k.startsWith("kubejs:") || k.equals("show_notification"));
            if (!named.containsAll(type.getValue())) {
                Set<String> missing = new TreeSet<>(type.getValue());
                missing.removeAll(named);
                findings.put(type.getKey(), "the schema does not name " + missing);
            }
        }
        Sweep.atLeast(context, "recipe types of ours", fields.size(), 7);
        Sweep.report(context, "every recipe type of ours has a KubeJS schema naming its fields", findings, Map.of());
        context.succeed();
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void growingAsksTheLoaderAndTellsIt(GameTestHelper context) {
        TestPlatform.CropGrowthProbe probe = TestPlatform.INSTANCE.cropGrowthProbe();
        if (probe == null) {
            context.succeed(); // no crop-growth events on this loader
            return;
        }
        context.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        context.setBlock(CROP.east(), Blocks.GLOWSTONE);
        context.setBlock(CROP, Defoliation.unworked(ModBlocks.INDICA_CROP.defaultBlockState()));
        // Lit next tick, once the light engine has run.
        context.runAfterDelay(5, () -> {
            ServerLevel world = context.getLevel();
            BlockPos pos = context.absolutePos(CROP);
            try {
                for (Block block : List.of(ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP)) {
                    String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
                    world.setBlock(pos, Defoliation.unworked(block.defaultBlockState()), Block.UPDATE_ALL);

                    probe.watch(pos, true);
                    world.getBlockState(pos).randomTick(world, pos, HIT);
                    int held = world.getBlockState(pos).getValue(BlockStateProperties.AGE_7);
                    List<String> heldHeard = probe.take();
                    context.assertTrue(held == 0, name + " grew to " + held + " although the loader's listener said not to");
                    context.assertTrue(heldHeard.equals(List.of("PRE")), name + " held back, heard " + heldHeard
                            + ", expected [PRE]");

                    probe.watch(pos, false);
                    world.getBlockState(pos).randomTick(world, pos, HIT);
                    int grown = world.getBlockState(pos).getValue(BlockStateProperties.AGE_7);
                    List<String> grownHeard = probe.take();
                    context.assertTrue(grown == 1, name + " did not grow on a landed roll: age " + grown);
                    context.assertTrue(grownHeard.equals(List.of("PRE", "POST")), name + " grew, heard " + grownHeard
                            + ", expected [PRE, POST]");
                }
            } finally {
                probe.stop();
            }
            context.succeed();
        });
    }

    /**
     * A plant held back by its headroom still rolls, and the loader is asked each time, as vanilla's crops ask
     * it; but it is told the plant grew only when the age really moved. A listener paid per growth (Ars
     * Nouveau's Agronomic Sourcelink) was paid for every roll of a roofed seedling that never grew.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aPlantWithNoRoomToGrowTellsNobodyItGrew(GameTestHelper context) {
        TestPlatform.CropGrowthProbe probe = TestPlatform.INSTANCE.cropGrowthProbe();
        if (probe == null) {
            context.succeed(); // no crop-growth events on this loader
            return;
        }
        context.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        context.setBlock(CROP.east(), Blocks.GLOWSTONE);
        // The last age that fits in one block: the next one needs the block above.
        Map<Block, Integer> lastLone = Map.of(
                ModBlocks.INDICA_CROP, IndicaCropBlock.DOUBLE_BLOCK_AGE - 1,
                ModBlocks.SATIVA_CROP, SativaCropBlock.TWO_TALL_AGE - 1);
        context.runAfterDelay(5, () -> {
            ServerLevel world = context.getLevel();
            BlockPos pos = context.absolutePos(CROP);
            try {
                for (Block block : List.of(ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP)) {
                    String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
                    int age = lastLone.get(block);
                    world.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    world.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                    world.setBlock(pos, Defoliation.unworked(block.defaultBlockState())
                            .setValue(BlockStateProperties.AGE_7, age), Block.UPDATE_ALL);

                    probe.watch(pos, false);
                    world.getBlockState(pos).randomTick(world, pos, HIT);
                    int held = world.getBlockState(pos).getValue(BlockStateProperties.AGE_7);
                    List<String> heldHeard = probe.take();
                    context.assertTrue(held == age, name + " grew to " + held + " under a roof");
                    context.assertTrue(heldHeard.equals(List.of("PRE")), name + " roofed at age " + age + ", heard "
                            + heldHeard + ", expected [PRE]");

                    world.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    probe.watch(pos, false);
                    world.getBlockState(pos).randomTick(world, pos, HIT);
                    int grown = world.getBlockState(pos).getValue(BlockStateProperties.AGE_7);
                    List<String> grownHeard = probe.take();
                    context.assertTrue(grown == age + 1, name + " did not grow once the roof was gone: age " + grown);
                    context.assertTrue(grownHeard.equals(List.of("PRE", "POST")), name + " grew, heard " + grownHeard
                            + ", expected [PRE, POST]");
                }
            } finally {
                probe.stop();
            }
            context.succeed();
        });
    }

    /**
     * Every seed of ours in vanilla's {@code #villager_plantable_seeds} stands on farmland. A farmer villager
     * plants the block's default state on farmland without asking whether it survives there, so a seed that
     * cannot (Beldía, a sand plant) would pop off at the next update and be planted again, for ever.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everySeedAVillagerPlantsStandsOnFarmland(GameTestHelper context) {
        context.setBlock(SOIL, Blocks.FARMLAND);
        Map<String, String> findings = new TreeMap<>();
        int seen = 0;
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.VILLAGER_PLANTABLE_SEEDS)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
            if (!Sweep.ours(id)) {
                continue;
            }
            seen++;
            if (!(holder.value() instanceof BlockItem seed)) {
                findings.put(id.toString(), "not a block item, and a villager plants only those");
                continue;
            }
            BlockState planted = seed.getBlock().defaultBlockState();
            if (!planted.canSurvive(context.getLevel(), context.absolutePos(CROP))) {
                findings.put(id.toString(), "a villager plants it on farmland, where " + planted + " cannot stand");
            }
        }
        Sweep.atLeast(context, "seeds of ours that villagers plant", seen, 2);
        Sweep.report(context, "every seed a villager plants stands on farmland", findings, Map.of());
        context.succeed();
    }

    /**
     * Nothing of ours that Create beats into pulp is an ingredient of a Create mixer recipe of ours. Create's
     * mixer runs the matching recipe with the most ingredients, and its pulp takes four of
     * {@code #create:pulpifiable}: an item in both would turn a basin loaded for our recipe into pulp the
     * moment it held four.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void nothingCreatePulpsIsOurMixersToo(GameTestHelper context) {
        Set<String> pulped = new TreeSet<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModTags.OtherMods.CREATE_PULPIFIABLE)) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
            if (Sweep.ours(id)) {
                pulped.add(id.toString());
            }
        }
        Sweep.atLeast(context, "items of ours Create pulps", pulped.size(), 1);
        Map<String, String> findings = new TreeMap<>();
        int mixing = 0;
        for (Map.Entry<String, Path> file : Sweep.modFiles().entrySet()) {
            String path = file.getKey();
            if (!path.startsWith("data/") || !path.contains("/recipe/") || !path.endsWith(".json")) {
                continue;
            }
            JsonObject recipe = Sweep.json(file.getValue()).getAsJsonObject();
            if (!recipe.has("type") || !recipe.get("type").getAsString().equals("create:mixing")) {
                continue;
            }
            mixing++;
            for (JsonElement ingredient : recipe.getAsJsonArray("ingredients")) {
                JsonObject entry = ingredient.getAsJsonObject();
                if (entry.has("item") && pulped.contains(entry.get("item").getAsString())) {
                    findings.put(path, entry.get("item").getAsString() + " is in #create:pulpifiable too");
                }
            }
        }
        Sweep.atLeast(context, "Create mixer recipes of ours", mixing, 1);
        Sweep.report(context, "nothing Create pulps is an ingredient of our mixer recipes", findings, Map.of());
        context.succeed();
    }
}
