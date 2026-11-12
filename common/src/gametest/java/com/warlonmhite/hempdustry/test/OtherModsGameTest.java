package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.compat.ReplacedRecipes;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TallHemp;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * A farm's harvester built on Productive Lib calls a {@code CropBlock} ripe when it refuses bone meal, and cuts
     * it; any harvester asking the same question would. So every block of a growing plant answers bone meal as its
     * root does, as vanilla's pitcher crop answers from its lower half, and no block of a ripe plant takes it. The
     * plant is grown by bone meal on its root, stage by stage, and every block it stands on is asked at each stage.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyBlockOfAPlantAnswersBoneMealAsItsRoot(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Map<String, String> findings = new TreeMap<>();
        int crops = 0;
        int seen = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState plain = block.defaultBlockState();
            if (!(block instanceof CropBlock crop) || !Sweep.ours(BuiltInRegistries.BLOCK.getKey(block))
                    || !(plain.hasProperty(SativaCropBlock.SEGMENT)
                    || plain.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))) {
                continue;
            }
            crops++;
            for (int up = 3; up >= 0; up--) {
                context.setBlock(CROP.above(up), Blocks.AIR);
            }
            // A pot of ours: every tall crop stands in one, Beldía's water rule included.
            context.setBlock(SOIL, ModBlocks.GROW_POT);
            context.setBlock(CROP.east(), Blocks.GLOWSTONE);
            context.setBlock(CROP, plain);
            for (int age = 0; ; age++) {
                BlockState root = context.getBlockState(CROP);
                if (!root.is(block) || root.getValue(BlockStateProperties.AGE_7) != age) {
                    findings.put(BuiltInRegistries.BLOCK.getKey(block) + " at age " + age,
                            "bone meal on the root left " + root);
                    break;
                }
                for (int up = 0; up < 3; up++) {
                    BlockState state = context.getBlockState(CROP.above(up));
                    if (!state.is(block)) {
                        break;
                    }
                    boolean takes = crop.isValidBonemealTarget(world, context.absolutePos(CROP.above(up)), state, false);
                    if (takes != age < crop.getMaxAge()) {
                        findings.put(state.toString(), "block " + up + " of the plant "
                                + (takes ? "takes" : "refuses") + " bone meal at age " + age);
                    }
                    seen++;
                }
                if (age >= crop.getMaxAge()) {
                    break;
                }
                crop.performBonemeal(world, HIT, context.absolutePos(CROP), root);
            }
        }
        Sweep.atLeast(context, "tall crops", crops, 3);
        Sweep.atLeast(context, "blocks of growing and ripe plants", seen, 30);
        Sweep.report(context, "every block of a plant answers bone meal as its root", findings, Map.of());
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
     * Our seeds and fibre carry the common tags other mods' machines and pack scripts read on 1.20.1, in both
     * namespaces a pack there may use: every hemp seed in {@code forge:seeds} and {@code c:seeds} (the seed-oil
     * presses compact any seed in {@code #forge:seeds}), hemp fibre in {@code forge:fiber_hemp} and
     * {@code c:fiber_hemp} (Immersive Engineering's name for it). And none of ours in Immersive Engineering's
     * {@code seeds/hemp} (its own industrial hemp seed, which a pack or a unifier asking for it means) or
     * {@code fabric_hemp} (its fabric takes eight fibre and a stick; our canvas takes four and turns back into
     * them, so in that tag it would halve the cost of everything made from the fabric).
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void ourSeedsAndFibreCarryTheCommonTags(GameTestHelper context) {
        Map<String, String> findings = new TreeMap<>();
        List<Item> seeds = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(ModTags.Items.HEMP_SEEDS).forEach(holder -> seeds.add(holder.value()));
        Sweep.atLeast(context, "hemp seeds of ours", seeds.size(), 3);
        for (String ns : List.of("forge", "c")) {
            for (Item seed : seeds) {
                expectIn(findings, seed, tag(ns, "seeds"), true);
                expectIn(findings, seed, tag(ns, "seeds/hemp"), false);
            }
            expectIn(findings, ModItems.HEMP_FIBER, tag(ns, "fiber_hemp"), true);
            expectIn(findings, ModItems.HEMP_CANVAS, tag(ns, "fabric_hemp"), false);
        }
        Sweep.report(context, "our seeds and fibre carry the common tags, and only those", findings, Map.of());
        context.succeed();
    }

    /**
     * How Create 6 finds a log at runtime: an id ending in {@code _log}, {@code _wood}, {@code _stem},
     * {@code _hyphae} or {@code _block}, not stripped. Beside a {@code <name>_planks} of the same mod it makes
     * itself a saw recipe turning one into six planks, at
     * {@code create:cutting/runtime_generated/compat/create/tag_runtime_generated/compat/<mod>/<name>_to_<name>_planks},
     * in a pack of its own that a new world stacks above every mod's data.
     */
    private static final Pattern CREATE_LOG = Pattern.compile(
            "^(?!stripped_)([a-z_]+)(_log|_wood|_stem|_hyphae|(?<!bioshroom)_block)(([a-z_]+)(?<!_stripped))?$");

    /**
     * Create's saw would turn a hemp stem into six planks: a plank costs twelve stems our way, and a stem is
     * fibre, not wood. A file of ours at Create's id loses to Create's pack, so every item of ours that Create
     * takes for a log beside our planks has Create's runtime id in {@link ReplacedRecipes#REPLACED}, and the
     * recipe of ours it names is a saw recipe that strips the stalk into fibre and never makes planks.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void createsSawLeavesOurStemsAsFibre(GameTestHelper context) {
        Map<String, String> findings = new TreeMap<>();
        Map<String, Path> files = Sweep.modFiles();
        int seen = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            Matcher log = CREATE_LOG.matcher(id.getPath());
            if (!Sweep.ours(id) || !log.matches()) {
                continue;
            }
            String planks = log.group(1) + "_planks";
            if (!BuiltInRegistries.ITEM.containsKey(new ResourceLocation(id.getNamespace(), planks))) {
                continue;
            }
            seen++;
            ResourceLocation creates = new ResourceLocation("create",
                    "cutting/runtime_generated/compat/create/tag_runtime_generated/compat/"
                            + id.getNamespace() + "/" + log.group(1) + "_to_" + planks);
            ResourceLocation ours = ReplacedRecipes.REPLACED.get(creates);
            if (ours == null) {
                findings.put(id.toString(), "Create saws it into planks at " + creates + ", which nothing drops");
                continue;
            }
            String path = "data/" + ours.getNamespace() + "/recipes/" + ours.getPath() + ".json";
            Path file = files.get(path);
            if (!Sweep.ours(ours) || file == null) {
                findings.put(id.toString(), creates + " is dropped for " + ours + ", which we do not ship at " + path);
                continue;
            }
            JsonObject recipe = Sweep.json(file).getAsJsonObject();
            String results = String.valueOf(recipe.get("results"));
            String ingredients = String.valueOf(recipe.get("ingredients"));
            if (!"create:cutting".equals(recipe.has("type") ? recipe.get("type").getAsString() : null)) {
                findings.put(path, "not a create:cutting recipe, so the saw does nothing with it: " + recipe.get("type"));
            } else if (!ingredients.contains("\"" + id + "\"")) {
                findings.put(path, "does not take " + id + ": " + ingredients);
            } else if (results.contains("_planks\"")
                    || !results.contains("\"" + BuiltInRegistries.ITEM.getKey(ModItems.HEMP_FIBER) + "\"")) {
                findings.put(path, "does not strip the stalk into fibre: " + results);
            }
        }
        Sweep.atLeast(context, "items of ours Create takes for a log beside our planks", seen, 1);
        Sweep.report(context, "Create's saw strips our stems into fibre, never planks", findings, Map.of());
        context.succeed();
    }

    /**
     * The recipe manager itself drops another mod's runtime recipe that one of ours replaces, before any
     * recipe is read, and only that one: fed a stand-in at Create's id beside ours and an unrelated recipe,
     * it loads ours and the unrelated one, and not Create's. Without ours among the files, Create's stays.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void theRecipeManagerDropsWhatOursReplace(GameTestHelper context) {
        Map.Entry<ResourceLocation, ResourceLocation> replaced = ReplacedRecipes.REPLACED.entrySet().iterator().next();
        ResourceLocation theirs = replaced.getKey();
        ResourceLocation ours = replaced.getValue();
        ResourceLocation unrelated = new ResourceLocation("create", "cutting/stand_in_unrelated");
        String stem = BuiltInRegistries.ITEM.getKey(ModItems.HEMP_STEM).toString();
        JsonElement planks = standIn(stem, BuiltInRegistries.ITEM.getKey(ModBlocks.HEMP_PLANKS.asItem()).toString(), 6);
        JsonElement fibre = standIn(stem, BuiltInRegistries.ITEM.getKey(ModItems.HEMP_FIBER).toString(), 4);

        Map<ResourceLocation, JsonElement> both = new HashMap<>();
        both.put(theirs, planks);
        both.put(ours, fibre);
        both.put(unrelated, standIn("minecraft:oak_log", "minecraft:oak_planks", 6));
        RecipeProbe loaded = RecipeProbe.load(both);
        context.assertFalse(loaded.byKey(theirs).isPresent(),
                theirs + " loaded beside " + ours + ", which replaces it: the saw would still make planks");
        context.assertTrue(loaded.byKey(ours).isPresent(), ours + " did not load");
        context.assertTrue(loaded.byKey(unrelated).isPresent(), "an unrelated recipe was dropped too");

        Map<ResourceLocation, JsonElement> alone = new HashMap<>();
        alone.put(theirs, planks);
        context.assertTrue(RecipeProbe.load(alone).byKey(theirs).isPresent(),
                theirs + " was dropped with nothing of ours to replace it");
        context.succeed();
    }

    /** A shapeless crafting recipe standing in for another mod's: one item in, a count of another out. */
    private static JsonElement standIn(String in, String out, int count) {
        return JsonParser.parseString("{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\""
                + in + "\"}],\"result\":{\"item\":\"" + out + "\",\"count\":" + count + "}}");
    }

    /** A recipe manager of its own, loaded from the given files the way a server start or a reload loads one. */
    private static final class RecipeProbe extends RecipeManager {
        static RecipeProbe load(Map<ResourceLocation, JsonElement> files) {
            RecipeProbe probe = new RecipeProbe();
            probe.apply(files, ResourceManager.Empty.INSTANCE, InactiveProfiler.INSTANCE);
            return probe;
        }
    }

    /**
     * Every recipe we ship for another mod's machine (or that makes another mod's item) names that mod in each
     * loader's conditions -- Forge's {@code conditions} and Fabric's {@code fabric:load_conditions} -- so without
     * it the file is never parsed (its recipe type would not exist, and the log gate would fail on the error),
     * and a loader never loads it on the other's key alone.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyRecipeForAnotherModWaitsForIt(GameTestHelper context) {
        Map<String, String> findings = new TreeMap<>();
        int seen = 0;
        for (Map.Entry<String, Path> file : Sweep.modFiles().entrySet()) {
            String path = file.getKey();
            if (!path.startsWith("data/") || !path.contains("/recipes/") || !path.endsWith(".json")) {
                continue;
            }
            JsonObject recipe = Sweep.json(file.getValue()).getAsJsonObject();
            String type = recipe.has("type") ? recipe.get("type").getAsString() : "";
            String mod = type.contains(":") ? type.substring(0, type.indexOf(':')) : "minecraft";
            if (mod.equals("minecraft") && recipe.has("result") && recipe.get("result").isJsonObject()
                    && recipe.getAsJsonObject("result").has("item")) {
                String result = recipe.getAsJsonObject("result").get("item").getAsString();
                mod = result.contains(":") ? result.substring(0, result.indexOf(':')) : "minecraft";
            }
            if (mod.equals("minecraft") || mod.equals(Hempdustry.MOD_ID)) {
                continue;
            }
            seen++;
            if (!waitsOnForge(recipe.get("conditions"), mod)) {
                findings.put(path + " (Forge)", "conditions do not wait for " + mod + ": " + recipe.get("conditions"));
            }
            if (!waitsOnFabric(recipe.get("fabric:load_conditions"), mod)) {
                findings.put(path + " (Fabric)", "fabric:load_conditions do not wait for " + mod + ": "
                        + recipe.get("fabric:load_conditions"));
            }
        }
        Sweep.atLeast(context, "recipes for other mods' machines", seen, 1);
        Sweep.report(context, "every recipe for another mod's machine loads only with that mod", findings, Map.of());
        context.succeed();
    }

    private static boolean waitsOnForge(JsonElement conditions, String mod) {
        if (conditions == null || !conditions.isJsonArray()) {
            return false;
        }
        for (JsonElement condition : conditions.getAsJsonArray()) {
            JsonObject c = condition.getAsJsonObject();
            if (c.has("type") && c.get("type").getAsString().equals("forge:mod_loaded")
                    && c.has("modid") && c.get("modid").getAsString().equals(mod)) {
                return true;
            }
        }
        return false;
    }

    private static boolean waitsOnFabric(JsonElement conditions, String mod) {
        if (conditions == null || !conditions.isJsonArray()) {
            return false;
        }
        for (JsonElement condition : conditions.getAsJsonArray()) {
            JsonObject c = condition.getAsJsonObject();
            if (c.has("condition") && c.get("condition").getAsString().equals("fabric:all_mods_loaded")
                    && c.has("values") && c.getAsJsonArray("values").toString().contains("\"" + mod + "\"")) {
                return true;
            }
        }
        return false;
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(namespace, path));
    }

    private static void expectIn(Map<String, String> findings, Item item, TagKey<Item> tag, boolean wanted) {
        if (item.builtInRegistryHolder().is(tag) != wanted) {
            findings.put(BuiltInRegistries.ITEM.getKey(item) + " in #" + tag.location(),
                    wanted ? "missing from the tag" : "in the tag, and should not be");
        }
    }

}
