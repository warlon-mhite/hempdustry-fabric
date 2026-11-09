package com.warlonmhite.hempdustry.test;

import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.advancement.HarvestHempCriterion;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.Mildew;
import com.warlonmhite.hempdustry.block.custom.PlantStress;
import com.warlonmhite.hempdustry.block.custom.WhiteWidowFlowerBlock;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.strain.Terpene;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModPlacedFeatures;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * White Widow, the pale garden's plant: Purple Kush's body, touchy about its light. A light at night
 * stresses it (the light leak), darkness at night never does, its wild flower keeps the eyeblossom's
 * hours, its seeds keep to the pale garden, and "Lights Out" is a harvest that kept its Grow Lamp.
 *
 * <p>Every guard here fails quietly in play: a leak that never fires looks like a lucky grower, one
 * that fires by day or on a dark lamp looks like a harsh one, and a flower stuck closed looks like a
 * flower.
 *
 * <p><b>Night</b> is the Overworld clock moved and put back within one test's own code ({@link #at}),
 * so no tick passes on it and no other test sees it; the environment's per-tick cache and the sky's
 * darkening are refreshed both ways.
 */
public final class WhiteWidowGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    /** Where a light over a two-tall plant hangs: two above its LOWER. */
    private static final BlockPos LIGHT = CROP.above(2);
    private static final long MIDNIGHT = 18000L;
    private static final long NOON = 6000L;

    /** A roll that never lands, so a step's record is the light's alone and never the ground's. */
    private static final RandomSource MISS = new SingleThreadedRandomSource(0L) {
        @Override
        public float nextFloat() {
            return 0.999F;
        }
    };

    // ----- the plant -----

    /** Purple Kush's ground, the mildew, the sheet, and a smoke of Health Boost, Hunger, Weaving and Bad Luck. */
    public static void whiteWidowGrowsAndSmokes(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos crop = context.absolutePos(CROP);
        BlockState seedling = ModBlocks.WHITE_WIDOW_CROP.defaultBlockState();
        for (Block ok : new Block[]{Blocks.FARMLAND, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(SOIL, ok);
            context.assertTrue(seedling.canSurvive(world, crop), "White Widow cannot stand on " + ok);
        }
        for (Block no : new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PALE_MOSS_BLOCK, Blocks.STONE}) {
            context.setBlock(SOIL, no);
            context.assertTrue(!seedling.canSurvive(world, crop), "White Widow can stand on " + no);
        }
        context.assertTrue(ModBlocks.WHITE_WIDOW_CROP instanceof Mildew.Host host && host.catchesMildew(),
                "White Widow does not catch Powdery Mildew");

        Holder<Strain> widow = strain(world, ModStrains.WHITE_WIDOW);
        context.assertTrue(widow.value().chemotype() == Chemotype.THC_DOMINANT, "White Widow is not THC-dominant");
        context.assertValueEqual(widow.value().terpene(), Optional.of(Terpene.NEROLIDOL), "White Widow's terpene");
        context.assertTrue(widow.value().wildFlower().filter(f -> f == ModBlocks.WHITE_WIDOW_FLOWER).isPresent(),
                "White Widow's wild flower is " + widow.value().wildFlower());

        ServerPlayer smoker = Players.survival(context);
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        pipe.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(widow, 1));
        pipe.set(ModComponents.CHARGES, DeviceType.PIPE.builtInBowl().hits());
        smoker.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        smoker.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(ModItems.WOODEN_PIPE));
        smoker.gameMode.useItem(smoker, world, pipe, InteractionHand.MAIN_HAND);
        for (Holder<MobEffect> effect : List.of(MobEffects.HEALTH_BOOST, MobEffects.HUNGER, MobEffects.WEAVING,
                MobEffects.UNLUCK)) {
            context.assertTrue(smoker.hasEffect(effect), "a White Widow pipe gave no " + effect.getRegisteredName());
        }
        context.succeed();
    }

    // ----- the light leak -----

    /**
     * A lit light at night stresses a White Widow from age 3; the same light by day, a dark lamp at
     * night, a torch beside it, or a younger plant never do; a lantern beside it does; Purple Kush never
     * leaks; and by day White Widow loses its light a stage early.
     */
    public static void whiteWidowLeaksAtNight(GameTestHelper context) {
        Block widow = ModBlocks.WHITE_WIDOW_CROP;
        Block kush = ModBlocks.INDICA_CROP;
        at(context, MIDNIGHT, () -> context.assertTrue(hours(context) == PlantStress.Hours.NIGHT,
                "midnight is not night to White Widow"));
        at(context, NOON, () -> context.assertTrue(hours(context) == PlantStress.Hours.DAY,
                "noon is not day to White Widow"));

        lamp(context, true);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, NOON), GrowLight.GROW_LAMP,
                "a White Widow under a lit Grow Lamp by day");
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.STRESSED,
                "a flowering White Widow under a lit Grow Lamp at night");
        expect(context, step(context, widow, 2, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.STRESSED,
                "a White Widow stepping to age 3 under a lit Grow Lamp at night");
        expect(context, step(context, widow, 1, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.GROW_LAMP,
                "a White Widow stepping to age 2 under a lit Grow Lamp at night");
        expect(context, step(context, kush, 4, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.GROW_LAMP,
                "a Purple Kush under a lit Grow Lamp at night");

        // The 12/12 timer: a dark lamp at night is never a lost light, and a first step at night under
        // one counts the lamp that is there.
        lamp(context, false);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.GROW_LAMP,
                "a flowering White Widow under a dark Grow Lamp at night");
        expect(context, step(context, widow, 0, GrowLight.NATURAL, MIDNIGHT), GrowLight.GROW_LAMP,
                "a White Widow's first step at night under a dark Grow Lamp");
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, NOON), GrowLight.STRESSED,
                "a flowering White Widow under a dark Grow Lamp by day");

        // Beside it, under the dark lamp: a torch (14) never leaks, a lantern (15) does, and only at
        // night (by day the lamp is lit, as a timer would have it). Each stands on stone, or the
        // plant's own updates would pop it.
        context.setBlock(SOIL.east(), Blocks.STONE);
        context.setBlock(CROP.east(), Blocks.TORCH);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.GROW_LAMP,
                "a White Widow with a torch beside it at night");
        context.setBlock(CROP.east(), Blocks.LANTERN);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.STRESSED,
                "a White Widow with a lantern beside it at night");
        lamp(context, true);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, NOON), GrowLight.GROW_LAMP,
                "a White Widow with a lantern beside it by day");
        lamp(context, false);
        context.setBlock(CROP.east(), Blocks.STONE);
        context.setBlock(CROP.above().east(), Blocks.LANTERN);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.STRESSED,
                "a White Widow with a lantern beside its top half at night");
        context.setBlock(CROP.above().east(), Blocks.AIR);
        context.setBlock(CROP.east(), Blocks.AIR);

        // By day a lost light stresses White Widow a stage early, Purple Kush at flowering as ever.
        lamp(context, null);
        expect(context, step(context, widow, 2, GrowLight.GROW_LAMP, NOON), GrowLight.STRESSED,
                "a White Widow losing its lamp by day on its way to age 3");
        expect(context, step(context, kush, 2, GrowLight.GROW_LAMP, NOON), GrowLight.NATURAL,
                "a Purple Kush losing its lamp by day on its way to age 3");
        // At night no lamp at all is no loss either: the night is dark anyway.
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, MIDNIGHT), GrowLight.GROW_LAMP,
                "a White Widow whose lamp was taken away at night");

        // In the twilight around dusk neither counts: a lamp still lit is no leak, a dark one no loss,
        // a lantern beside it no leak. That is the time a detector-driven lamp takes to follow the sky.
        long dusk = -1;
        for (long time = 12000L; time < 15000L && dusk < 0; time++) {
            long probe = time;
            boolean[] twilight = new boolean[1];
            at(context, probe, () -> twilight[0] = hours(context) == PlantStress.Hours.TWILIGHT);
            dusk = twilight[0] ? probe : -1;
        }
        context.assertTrue(dusk >= 0, "no twilight between 12000 and 15000");
        lamp(context, true);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, dusk), GrowLight.GROW_LAMP,
                "a White Widow under a lit Grow Lamp at dusk (" + dusk + ")");
        lamp(context, false);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, dusk), GrowLight.GROW_LAMP,
                "a White Widow under a dark Grow Lamp at dusk (" + dusk + ")");
        context.setBlock(SOIL.east(), Blocks.STONE);
        context.setBlock(CROP.east(), Blocks.LANTERN);
        expect(context, step(context, widow, 4, GrowLight.GROW_LAMP, dusk), GrowLight.GROW_LAMP,
                "a White Widow with a lantern beside it at dusk (" + dusk + ")");
        context.setBlock(CROP.east(), Blocks.AIR);
        context.succeed();
    }

    /**
     * A Grow Lamp on a bare daylight detector keeps White Widow's hours, in clear weather, rain and
     * thunder: whenever the plant reads night the detector has given no signal for longer than the lamp
     * takes to go dark, and whenever it reads day it has given one for longer than it takes to look at
     * the sky. Every tick of a whole day is read from a real detector, so the plant's night can never
     * drift from vanilla's block.
     */
    public static void aDaylightDetectorIsTheWidowsTimer(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos detector = new BlockPos(6, 1, 6);
        context.setBlock(new BlockPos(6, 8, 6), Blocks.AIR);
        context.setBlock(detector, Blocks.DAYLIGHT_DETECTOR);
        BlockPos pos = context.absolutePos(detector);
        context.assertTrue(world.getBrightness(LightLayer.SKY, pos) == 15,
                "the detector sees sky light " + world.getBrightness(LightLayer.SKY, pos) + ", not the open sky's 15");
        ServerPlayer hand = Players.survival(context);

        MinecraftServer server = world.getServer();
        Holder<WorldClock> clock = server.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        long was = server.clockManager().getInstance(clock).totalTicks();
        long day = Math.floorDiv(was, 24000L) * 24000L;
        float rainWas = world.getRainLevel(1.0F);
        float thunderWas = world.getThunderLevel(1.0F);
        int[] noonDarken = new int[3];
        String[] names = {"clear", "rain", "thunder"};
        float[][] weathers = {{0.0F, 0.0F}, {1.0F, 0.0F}, {1.0F, 1.0F}};
        try {
            for (int w = 0; w < weathers.length; w++) {
                world.setRainLevel(weathers[w][0]);
                world.setThunderLevel(weathers[w][1]);
                int[] power = new int[24000];
                PlantStress.Hours[] hours = new PlantStress.Hours[24000];
                for (int t = 0; t < 24000; t++) {
                    server.clockManager().setTotalTicks(clock, day + t);
                    world.environmentAttributes().invalidateTickCache();
                    world.updateSkyBrightness();
                    hours[t] = PlantStress.hours(world, pos);
                    power[t] = detectorPower(world, pos, hand);
                    if (t == 6000) {
                        noonDarken[w] = world.getSkyDarken();
                    }
                }
                int nights = 0;
                int twilights = 0;
                for (int t = 0; t < 24000; t++) {
                    if (hours[t] == PlantStress.Hours.NIGHT) {
                        nights++;
                        // The detector looks once a second and the lamp goes out four ticks after it.
                        for (int back = 0; back <= 24; back++) {
                            int s = Math.floorMod(t - back, 24000);
                            context.assertTrue(power[s] == 0, names[w] + ": the plant reads night at " + t
                                    + " but the detector gave " + power[s] + " at " + s + ", so its lamp may still be lit");
                        }
                    } else if (hours[t] == PlantStress.Hours.DAY) {
                        for (int back = 0; back <= 20; back++) {
                            int s = Math.floorMod(t - back, 24000);
                            context.assertTrue(power[s] > 0, names[w] + ": the plant reads day at " + t
                                    + " but the detector gave no signal at " + s + ", so its lamp may still be dark");
                        }
                    } else {
                        twilights++;
                    }
                }
                context.assertTrue(nights > 8000, names[w] + ": only " + nights + " ticks of night");
                context.assertTrue(twilights < 1200, names[w] + ": " + twilights + " ticks of twilight in a day");
            }
        } finally {
            world.setRainLevel(rainWas);
            world.setThunderLevel(thunderWas);
            server.clockManager().setTotalTicks(clock, was);
            world.environmentAttributes().invalidateTickCache();
            world.updateSkyBrightness();
        }
        context.assertTrue(noonDarken[2] > noonDarken[0],
                "thunder darkened noon no more than a clear sky (" + noonDarken[2] + "), so the weather never reached the test");
        context.succeed();
    }

    /** The leak is read on a natural growth step and a bee's, as on bone meal's. */
    public static void everyGrowthStepReadsTheLeak(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        CropBlock widow = (CropBlock) ModBlocks.WHITE_WIDOW_CROP;
        lamp(context, true);

        plant(context, widow, 4, GrowLight.GROW_LAMP);
        at(context, MIDNIGHT, () -> {
            BlockState bee = PlantStress.beeStep(world, pos, world.getBlockState(pos), widow.getStateForAge(5), MISS);
            expect(context, bee.getValue(GrowLight.PROPERTY), GrowLight.STRESSED, "a bee's step at night under a lit lamp");
        });
        at(context, NOON, () -> {
            BlockState bee = PlantStress.beeStep(world, pos, world.getBlockState(pos), widow.getStateForAge(5), MISS);
            expect(context, bee.getValue(GrowLight.PROPERTY), GrowLight.GROW_LAMP, "a bee's step by day under a lit lamp");
        });

        // A natural step: a random tick whose growth roll always lands. Moist farmland has no stress
        // odds of its own, so a roll of zero can only stress the plant through its light.
        RandomSource grows = new SingleThreadedRandomSource(0L) {
            @Override
            public float nextFloat() {
                return 0.0F;
            }
        };
        plant(context, widow, 4, GrowLight.GROW_LAMP);
        at(context, MIDNIGHT, () -> world.getBlockState(pos).randomTick(world, pos, grows));
        context.assertTrue(context.getBlockState(CROP).getValue(BlockStateProperties.AGE_7) == 5,
                "the random tick did not grow the plant, so this probe proves nothing");
        expect(context, context.getBlockState(CROP).getValue(GrowLight.PROPERTY), GrowLight.STRESSED,
                "a natural step at night under a lit lamp");
        context.succeed();
    }

    // ----- the wild flower -----

    /**
     * Wild White Widow grows in the pale garden only, as a flower; it opens at night and closes by day,
     * in a pot too, and drops the same either way.
     */
    public static void wildWhiteWidowKeepsTheEyeblossomsHours(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        var biomes = world.registryAccess().lookupOrThrow(Registries.BIOME);
        context.assertTrue(biomes.getOrThrow(Biomes.PALE_GARDEN).is(ModTags.Biomes.WHITE_WIDOW_GEN),
                "#hempdustry:white_widow_gen does not hold the pale garden");
        for (ResourceKey<Biome> not : List.of(Biomes.DARK_FOREST, Biomes.PLAINS, Biomes.CHERRY_GROVE)) {
            context.assertTrue(!biomes.getOrThrow(not).is(ModTags.Biomes.WHITE_WIDOW_GEN),
                    "#hempdustry:white_widow_gen holds " + not.identifier());
        }

        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                context.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlock(new BlockPos(x, 1, z), x < 5 ? Blocks.GRASS_BLOCK : Blocks.STONE);
                context.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }
        PlacedFeature wild = world.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(ModPlacedFeatures.WHITE_WIDOW_PLACED_KEY).value();
        int scatter = 0;
        while (!(wild.placement().get(scatter) instanceof OffsetPlacement)) {
            scatter++;
        }
        PlacedFeature patch = new PlacedFeature(wild.feature(), wild.placement().subList(scatter - 1, wild.placement().size()));
        RandomSource random = RandomSource.create(7L);
        var generator = world.getChunkSource().getGenerator();
        for (int i = 0; i < 20; i++) {
            patch.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 4)));
        }
        int placed = 0;
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                BlockState found = context.getBlockState(new BlockPos(x, 2, z));
                if (found.is(ModBlocks.WHITE_WIDOW_FLOWER)) {
                    placed++;
                    context.assertTrue(context.getBlockState(new BlockPos(x, 1, z)).is(Blocks.GRASS_BLOCK),
                            "wild White Widow grew off the grass at x " + x + " z " + z);
                }
                context.assertTrue(!found.is(ModBlocks.WHITE_WIDOW_CROP), "the wild patch placed a crop at x " + x + " z " + z);
            }
        }
        context.assertTrue(placed > 0, "twenty White Widow patches placed nothing");

        // Its hours, the flower and the pot alike: a random tick turns it to match the night.
        BlockPos flower = new BlockPos(2, 2, 2);
        BlockPos pot = new BlockPos(3, 2, 2);
        context.setBlock(flower, ModBlocks.WHITE_WIDOW_FLOWER);
        context.setBlock(pot, ModBlocks.POTTED_WHITE_WIDOW_FLOWER);
        for (BlockPos at : List.of(flower, pot)) {
            context.assertTrue(!context.getBlockState(at).getValue(WhiteWidowFlowerBlock.OPEN), "a new " + context.getBlockState(at).getBlock() + " stands open");
            context.assertTrue(context.getBlockState(at).isRandomlyTicking(), context.getBlockState(at).getBlock() + " never ticks");
            at(context, MIDNIGHT, () -> tick(context, at));
            context.assertTrue(context.getBlockState(at).getValue(WhiteWidowFlowerBlock.OPEN), context.getBlockState(at).getBlock() + " stayed shut at night");
            at(context, MIDNIGHT, () -> tick(context, at));
            context.assertTrue(context.getBlockState(at).getValue(WhiteWidowFlowerBlock.OPEN), context.getBlockState(at).getBlock() + " closed at night");
            at(context, NOON, () -> tick(context, at));
            context.assertTrue(!context.getBlockState(at).getValue(WhiteWidowFlowerBlock.OPEN), context.getBlockState(at).getBlock() + " stayed open by day");
        }

        // Its drops, as Wild Purple Kush's and the same at any hour: seeds by hand, itself with shears.
        LootTable table = world.getServer().reloadableRegistries().getLootTable(ModBlocks.WHITE_WIDOW_FLOWER.getLootTable().orElseThrow());
        for (boolean open : new boolean[]{false, true}) {
            for (ItemStack tool : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.SHEARS)}) {
                LootParams params = new LootParams.Builder(world)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(flower)))
                        .withParameter(LootContextParams.BLOCK_STATE,
                                ModBlocks.WHITE_WIDOW_FLOWER.defaultBlockState().setValue(WhiteWidowFlowerBlock.OPEN, open))
                        .withParameter(LootContextParams.TOOL, tool)
                        .create(LootContextParamSets.BLOCK);
                List<ItemStack> drops = table.getRandomItems(params, 11L);
                boolean shears = !tool.isEmpty();
                context.assertTrue(!drops.isEmpty() && drops.stream().allMatch(d -> d.is(shears
                                ? ModBlocks.WHITE_WIDOW_FLOWER.asItem() : ModItems.WHITE_WIDOW_SEEDS)),
                        "Wild White Widow, " + (open ? "open" : "closed") + ", broken " + (shears ? "with shears" : "by hand")
                                + " dropped " + drops);
            }
        }
        context.succeed();
    }

    // ----- the seeds -----

    /**
     * White Widow's seeds are in mansion chests and a pale-garden camp's common chest, at the farm
     * strains' chest chance, and nowhere the shared seeds are: not the grass, the dungeon, the village.
     * A camp anywhere else never holds them.
     */
    public static void whiteWidowSeedsStayInThePaleGarden(GameTestHelper context) {
        Predicate<ItemStack> widow = stack -> stack.is(ModItems.WHITE_WIDOW_SEEDS);
        BlockPos at = new BlockPos(2, 2, 2);
        fillBiome(context, at, "minecraft:plains");
        try {
            int mansion = rolls(context, BuiltInLootTables.WOODLAND_MANSION, at, widow);
            context.assertTrue(mansion >= 230 && mansion <= 370, "a mansion chest held White Widow seeds " + mansion + " times in 1000, expected about 300");
            for (ResourceKey<LootTable> none : List.of(BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.SHIPWRECK_SUPPLY,
                    BuiltInLootTables.VILLAGE_PLAINS_HOUSE, BuiltInLootTables.ABANDONED_MINESHAFT,
                    BuiltInLootTables.ABANDONED_CAMP_COMMON_CHEST, BuiltInLootTables.ABANDONED_CAMP_SECRET_CHEST)) {
                context.assertValueEqual(rolls(context, none, at, widow), 0, none.identifier() + " chests holding White Widow seeds outside a pale garden");
            }
            // The grass pool only fires on a real two-half break, so it is read, not rolled.
            String grass = LootTable.DIRECT_CODEC.encodeStart(context.getLevel().registryAccess()
                    .createSerializationContext(JsonOps.INSTANCE), context.getLevel().getServer().reloadableRegistries()
                    .getLootTable(Blocks.TALL_GRASS.getLootTable().orElseThrow())).getOrThrow().toString();
            context.assertTrue(grass.contains("\"hempdustry:indica_seeds\"") && !grass.contains("\"hempdustry:white_widow_seeds\""),
                    "tall grass names White Widow seeds, or no hemp seeds at all");
            fillBiome(context, at, "minecraft:pale_garden");
            int camp = rolls(context, BuiltInLootTables.ABANDONED_CAMP_COMMON_CHEST, at, widow);
            context.assertTrue(camp >= 230 && camp <= 370, "a pale-garden camp's common chest held White Widow seeds "
                    + camp + " times in 1000, expected about 300");
            context.assertValueEqual(rolls(context, BuiltInLootTables.ABANDONED_CAMP_SECRET_CHEST, at, widow), 0,
                    "a pale-garden camp's secret chest holding White Widow seeds");
        } finally {
            fillBiome(context, at, "minecraft:plains");
        }
        context.succeed();
    }

    // ----- the advancement -----

    /** "Lights Out": a White Widow harvested on its Grow Lamp record, and nothing else. */
    public static void lightsOut(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        AdvancementHolder lightsOut = world.getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("hempdustry", "lights_out"));
        context.assertTrue(lightsOut != null, "hempdustry:lights_out is not loaded");
        context.assertTrue(lightsOut.value().display().map(DisplayInfo::hidden).orElse(false), "Lights Out is not hidden");
        context.assertTrue(lightsOut.value().parent().equals(Optional.of(Identifier.fromNamespaceAndPath("hempdustry", "midnight_sun"))),
                "Lights Out does not hang under Midnight Sun");

        context.assertTrue(!harvest(context, ModBlocks.WHITE_WIDOW_CROP, GrowLight.STRESSED, lightsOut),
                "a stressed White Widow granted Lights Out");
        context.assertTrue(!harvest(context, ModBlocks.WHITE_WIDOW_CROP, GrowLight.LAMP, lightsOut),
                "a White Widow grown under a redstone lamp granted Lights Out");
        context.assertTrue(!harvest(context, ModBlocks.INDICA_CROP, GrowLight.GROW_LAMP, lightsOut),
                "a Purple Kush on its Grow Lamp record granted Lights Out");
        context.assertTrue(harvest(context, ModBlocks.WHITE_WIDOW_CROP, GrowLight.GROW_LAMP, lightsOut),
                "a White Widow on its Grow Lamp record did not grant Lights Out");

        // The End has no night, so a lamp left on all the time keeps the record there: no Lights Out.
        ServerLevel end = world.getServer().getLevel(Level.END);
        context.assertTrue(end != null, "the test server has no End");
        BlockState ripe = Defoliation.unworked(ModBlocks.WHITE_WIDOW_CROP.defaultBlockState())
                .setValue(BlockStateProperties.AGE_7, 7).setValue(GrowLight.PROPERTY, GrowLight.GROW_LAMP);
        ServerPlayer inTheEnd = Players.survival(context, end);
        HarvestHempCriterion.trigger(inTheEnd, end, BlockPos.ZERO.above(64), ripe);
        context.assertTrue(!inTheEnd.getAdvancements().getOrStartProgress(lightsOut).isDone(),
                "a White Widow on its Grow Lamp record harvested in the End granted Lights Out");
        ServerPlayer atHome = Players.survival(context);
        HarvestHempCriterion.trigger(atHome, world, context.absolutePos(CROP), ripe);
        context.assertTrue(atHome.getAdvancements().getOrStartProgress(lightsOut).isDone(),
                "the same harvest in the Overworld did not grant Lights Out, so the End probe proves nothing");
        context.succeed();
    }

    // ----- helpers -----

    /**
     * Runs {@code body} with the Overworld clock at {@code time} of the day, then puts the clock back as
     * it was. Nothing ticks in between, so no other test sees the change.
     */
    static void at(GameTestHelper context, long time, Runnable body) {
        ServerLevel world = context.getLevel();
        MinecraftServer server = world.getServer();
        Holder<WorldClock> clock = server.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        long was = server.clockManager().getInstance(clock).totalTicks();
        server.clockManager().setTotalTicks(clock, Math.floorDiv(was, 24000L) * 24000L + time);
        world.environmentAttributes().invalidateTickCache();
        world.updateSkyBrightness();
        try {
            body.run();
        } finally {
            server.clockManager().setTotalTicks(clock, was);
            world.environmentAttributes().invalidateTickCache();
            world.updateSkyBrightness();
        }
    }

    private static PlantStress.Hours hours(GameTestHelper context) {
        return PlantStress.hours(context.getLevel(), context.absolutePos(CROP));
    }

    /**
     * What the daylight detector at {@code pos} reads now, from vanilla's own code: a click turns it to
     * inverted and back, and each click makes it look at the sky.
     */
    private static int detectorPower(ServerLevel world, BlockPos pos, ServerPlayer hand) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        world.getBlockState(pos).useWithoutItem(world, hand, hit);
        world.getBlockState(pos).useWithoutItem(world, hand, hit);
        BlockState state = world.getBlockState(pos);
        if (state.getValue(DaylightDetectorBlock.INVERTED)) {
            throw new IllegalStateException("the detector at " + pos + " was left inverted");
        }
        return state.getValue(DaylightDetectorBlock.POWER);
    }

    /** A Grow Lamp over the plant: lit (powered), dark ({@code false}), or none ({@code null}). */
    private static void lamp(GameTestHelper context, Boolean lit) {
        context.setBlock(LIGHT.above(), lit == Boolean.TRUE ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
        context.setBlock(LIGHT, lit == null ? Blocks.AIR.defaultBlockState()
                : ModBlocks.GROW_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, lit));
    }

    /** A plant of {@code crop} at {@code age} on moist farmland, its segments grown in by a same-age reconcile. */
    private static void plant(GameTestHelper context, Block crop, int age, GrowLight record) {
        for (int y = 1; y >= 0; y--) {
            if (context.getBlockState(CROP.above(y)).getBlock() instanceof CropBlock) {
                context.setBlock(CROP.above(y), Blocks.AIR);
            }
        }
        context.setBlock(SOIL, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
        context.setBlock(CROP, Defoliation.unworked(crop.defaultBlockState())
                .setValue(BlockStateProperties.AGE_7, age).setValue(GrowLight.PROPERTY, record));
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        world.getBlockState(pos).randomTick(world, pos, MISS);
    }

    /** One bone meal step at {@code time} of day on a fresh plant, and the record it leaves. */
    private static GrowLight step(GameTestHelper context, Block crop, int age, GrowLight record, long time) {
        plant(context, crop, age, record);
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        at(context, time, () -> ((CropBlock) crop).performBonemeal(world, MISS, pos, world.getBlockState(pos), BonemealSource.INTERACTION));
        context.assertTrue(context.getBlockState(CROP).getValue(BlockStateProperties.AGE_7) == age + 1,
                "the step did not grow the plant, so this probe proves nothing");
        return context.getBlockState(CROP).getValue(GrowLight.PROPERTY);
    }

    private static void expect(GameTestHelper context, GrowLight actual, GrowLight expected, String what) {
        context.assertTrue(actual == expected, what + " left the plant " + actual + ", expected " + expected);
    }

    private static void tick(GameTestHelper context, BlockPos at) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(at);
        world.getBlockState(pos).randomTick(world, pos, MISS);
    }

    /** A ripe plant of {@code crop} keeping {@code record}, broken by a fresh player; whether they got {@code advancement}. */
    private static boolean harvest(GameTestHelper context, Block crop, GrowLight record, AdvancementHolder advancement) {
        plant(context, crop, 7, record);
        ServerPlayer player = Players.survival(context);
        player.gameMode.destroyBlock(context.absolutePos(CROP));
        context.assertTrue(context.getBlockState(CROP).isAir(), "the ripe " + crop + " was not harvested");
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static int rolls(GameTestHelper context, ResourceKey<LootTable> key, BlockPos at, Predicate<ItemStack> what) {
        ServerLevel world = context.getLevel();
        LootTable table = world.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(at)))
                .create(LootContextParamSets.CHEST);
        Random seeds = new Random(20261110L);
        int found = 0;
        for (int i = 0; i < 1000; i++) {
            if (table.getRandomItems(params, seeds.nextLong()).stream().anyMatch(what)) {
                found++;
            }
        }
        return found;
    }

    private static void fillBiome(GameTestHelper context, BlockPos at, String biome) {
        BlockPos a = context.absolutePos(at).offset(-4, -4, -4);
        BlockPos b = context.absolutePos(at).offset(4, 4, 4);
        var server = context.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                "fillbiome " + a.getX() + " " + a.getY() + " " + a.getZ() + " " + b.getX() + " " + b.getY() + " " + b.getZ() + " " + biome);
    }

    private static Holder<Strain> strain(ServerLevel world, ResourceKey<Strain> key) {
        return world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }
}
