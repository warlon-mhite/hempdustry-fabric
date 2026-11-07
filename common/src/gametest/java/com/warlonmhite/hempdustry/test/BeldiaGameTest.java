package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.registry.DataRegistries;
import java.util.Random;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import net.minecraft.world.level.storage.loot.LootTables;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.BeldiaCropBlock;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.warlonmhite.hempdustry.registry.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Beldía: a sand-and-water crop, buds that sift double, a Mirage that lands in two halves, and a
 * plant found only in the desert. Every guard here fails without a sound — a seed that planted on
 * dry sand, a plant that died when its water went, a Blindness that arrived with the hit, or desert
 * seeds leaking into meadow grass would all look like working content.
 */
public final class BeldiaGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos CROP = SOIL.above();
    private static final int ROLLS = 40;
    private static final long SEED_SOURCE = 0xBE1D1AL;
    /** The pipe's exhale: {@code Smoking.EXHALE_DELAY_TICKS}, with no sound delay. */
    private static final int EXHALE_TICKS = 38;

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void beldiaGrowsOnWateredSand(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos soil = context.absolutePos(SOIL);
        BlockPos crop = context.absolutePos(CROP);
        BlockState seedling = ModBlocks.BELDIA_CROP.defaultBlockState();
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlock(new BlockPos(x, 1, z), Blocks.SAND);
            }
        }

        // The reach is farmland's: four out, level with the sand or one up. Five is too far.
        Asserts.assertTrue(context, !BeldiaCropBlock.isWatered(world, soil), "dry sand reads as watered");
        context.setBlock(SOIL.east(5), Blocks.WATER);
        Asserts.assertTrue(context, !BeldiaCropBlock.isWatered(world, soil), "water five blocks away waters Beldía");
        context.setBlock(SOIL.east(5), Blocks.SAND);
        context.setBlock(SOIL.east(4).above(2), Blocks.WATER);
        Asserts.assertTrue(context, !BeldiaCropBlock.isWatered(world, soil), "water two blocks above the sand waters Beldía");
        context.setBlock(SOIL.east(4).above(2), Blocks.AIR);
        context.setBlock(SOIL.east(4), Blocks.WATER);
        Asserts.assertTrue(context, BeldiaCropBlock.isWatered(world, soil), "water four blocks away does not water Beldía");

        // Ground: sand of any colour, or a bed of ours. Never farmland. (1.18.2 has no suspicious sand.)
        Asserts.assertTrue(context, seedling.canSurvive(world, crop), "Beldía cannot stand on sand");
        for (Block ok : new Block[]{Blocks.RED_SAND, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(SOIL, ok);
            Asserts.assertTrue(context, seedling.canSurvive(world, crop), "Beldía cannot stand on " + ok);
        }
        for (Block no : new Block[]{Blocks.FARMLAND, Blocks.SANDSTONE, Blocks.DIRT}) {
            context.setBlock(SOIL, no);
            Asserts.assertTrue(context, !seedling.canSurvive(world, crop), "Beldía can stand on " + no);
        }

        // Planting through the real click: a seed goes down on watered sand, and dry sand refuses it
        // without eating the seed.
        context.setBlock(SOIL, Blocks.SAND);
        ServerPlayer player = Players.mock(context);
        player.setGameMode(GameType.SURVIVAL);
        ItemStack seeds = new ItemStack(ModItems.BELDIA_SEEDS, 4);
        plant(player, world, soil, seeds);
        Asserts.assertTrue(context, world.getBlockState(crop).is(ModBlocks.BELDIA_CROP), "a seed would not go down on watered sand");
        Asserts.assertTrue(context, seeds.getCount() == 3, "planting did not take exactly one seed");
        context.setBlock(CROP, Blocks.AIR);
        context.setBlock(SOIL.east(4), Blocks.SAND);
        plant(player, world, soil, seeds);
        Asserts.assertTrue(context, world.getBlockState(crop).isAir(), "a seed went down on dry sand");
        Asserts.assertTrue(context, seeds.getCount() == 3, "dry sand ate the seed it refused");
        // A bed answers for itself: a seed goes down in a Grow Pot with no water anywhere near it.
        context.setBlock(SOIL, ModBlocks.GROW_POT);
        plant(player, world, soil, seeds);
        Asserts.assertTrue(context, world.getBlockState(crop).is(ModBlocks.BELDIA_CROP), "a seed would not go down in a dry-room Grow Pot");
        context.setBlock(CROP, Blocks.AIR);
        context.setBlock(SOIL, Blocks.SAND);

        // Water taken away: the plant stops growing, refuses bone meal without eating it, and does not
        // die. Water back: it grows again.
        context.setBlock(SOIL.east(4), Blocks.WATER);
        context.setBlock(CROP, seedling);
        context.setBlock(SOIL.east(4), Blocks.SAND);
        for (int i = 0; i < 1000; i++) {
            world.getBlockState(crop).randomTick(world, crop, world.getRandom());
        }
        BlockState dry = world.getBlockState(crop);
        Asserts.assertTrue(context, dry.is(ModBlocks.BELDIA_CROP), "a Beldía whose water was taken away died");
        Asserts.assertTrue(context, dry.getValue(IndicaCropBlock.AGE) == 0, "a dry Beldía grew to age " + dry.getValue(IndicaCropBlock.AGE));
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
        Asserts.assertTrue(context, !BoneMealItem.growCrop(meal, world, crop), "a dry Beldía took bone meal");
        Asserts.assertTrue(context, meal.getCount() == 4, "a dry Beldía ate the bone meal it refused");
        context.setBlock(SOIL.east(4), Blocks.WATER);
        for (int i = 0; i < 1000 && world.getBlockState(crop).getValue(IndicaCropBlock.AGE) == 0; i++) {
            world.getBlockState(crop).randomTick(world, crop, world.getRandom());
        }
        Asserts.assertTrue(context, world.getBlockState(crop).getValue(IndicaCropBlock.AGE) > 0,
                "a Beldía given its water back never grew in 1000 random ticks");
        context.succeed();
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void beldiaHarvestsLeafyAndSiftsDouble(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(CROP));
        context.setBlock(SOIL, Blocks.SAND);

        // On matched seeds, a ripe Beldía pays Purple Kush's harvest with one stem fewer.
        LootTable beldia = table(world, ModBlocks.BELDIA_CROP);
        LootTable purple = table(world, ModBlocks.INDICA_CROP);
        java.util.Random seeds = new java.util.Random(SEED_SOURCE);
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            int[] b = harvest(beldia, world, origin, ripe(ModBlocks.BELDIA_CROP), seed, ModItems.BELDIA_BUDS, ModItems.BELDIA_SEEDS);
            int[] p = harvest(purple, world, origin, ripe(ModBlocks.INDICA_CROP), seed, ModItems.INDICA_BUDS, ModItems.INDICA_SEEDS);
            Asserts.assertTrue(context, b[0] == p[0] && b[1] == p[1] && b[2] == p[2] && b[3] == p[3] - 1,
                    "Beldía paid buds/leaves/seeds/stems " + b[0] + "/" + b[1] + "/" + b[2] + "/" + b[3]
                            + " against Purple Kush's " + p[0] + "/" + p[1] + "/" + p[2] + "/" + p[3]
                            + " — expected the same with one stem fewer");
        }
        Asserts.assertTrue(context, new ItemStack(ModItems.BELDIA_SEEDS).is(ModTags.Items.HEMP_SEEDS), "Beldía seeds are not hemp seeds");

        // The Sifting Box: two levels a bud, so four fill the screen and three do not — and the buds
        // sit in their own tag, not in siftable/flower, where the viewer would count them at seven.
        Asserts.assertTrue(context, new ItemStack(ModItems.BELDIA_BUDS).is(ModTags.Items.SIFTABLE_RESINOUS)
                        && !new ItemStack(ModItems.BELDIA_BUDS).is(ModTags.Items.SIFTABLE_FLOWER),
                "Beldía buds are in the wrong siftable tag");
        BlockPos boxRel = new BlockPos(5, 1, 5);
        BlockPos box = context.absolutePos(boxRel);
        context.setBlock(boxRel, ModBlocks.SIFTING_BOX.defaultBlockState());
        Player player = Line.mockPlayer(context, GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(box), Direction.UP, box, false);
        ItemStack buds = new ItemStack(ModItems.BELDIA_BUDS, 64);
        for (int i = 0; i < 3; i++) {
            Line.useItemOn(world.getBlockState(box),buds, world, player, InteractionHand.MAIN_HAND, hit);
        }
        Asserts.assertTrue(context, context.getBlockState(boxRel).getValue(SiftingBoxBlock.LEVEL) == 6,
                "three Beldía buds moved the screen to " + context.getBlockState(boxRel).getValue(SiftingBoxBlock.LEVEL) + ", expected 6");
        // A Purple Kush bud still moves it one, and shares the screen: both are plant matter.
        Line.useItemOn(world.getBlockState(box),new ItemStack(ModItems.INDICA_BUDS, 4), world, player, InteractionHand.MAIN_HAND, hit);
        Asserts.assertTrue(context, context.getBlockState(boxRel).getValue(SiftingBoxBlock.LEVEL) == SiftingBoxBlock.FULL_LEVEL,
                "a Purple Kush bud on top of three Beldía did not fill the screen");
        context.setBlock(boxRel, ModBlocks.SIFTING_BOX.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            Line.useItemOn(world.getBlockState(box),buds, world, player, InteractionHand.MAIN_HAND, hit);
        }
        Asserts.assertTrue(context, context.getBlockState(boxRel).getValue(SiftingBoxBlock.LEVEL) == SiftingBoxBlock.FULL_LEVEL,
                "four Beldía buds did not fill the screen");
        Asserts.assertTrue(context, buds.getCount() == 64 - 7, "the screen took " + (64 - buds.getCount()) + " buds, expected 7");
        context.succeed();
    }

    /**
     * Mirage, through a real pipe hit: Invisibility and Hunger on the hit for the pipe's full 700
     * ticks, Blindness on the exhale for a quarter of that — and never on a player who died in the
     * window and came back.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void beldiaSmokesAMirage(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> beldia = DataRegistries.of(world).lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.BELDIA);
        Asserts.assertTrue(context, beldia.value().coughFactor() == 0.5F, "Beldía does not cough at twice the odds");
        int length = DeviceType.PIPE.builtInStats().durationTicks();

        ServerPlayer smoker = Players.mock(context);
        smoker.setGameMode(GameType.SURVIVAL);
        smoke(smoker, world, beldia);
        MobEffectInstance invisible = smoker.getEffect(MobEffects.INVISIBILITY);
        Asserts.assertTrue(context, invisible != null && invisible.getDuration() == length,
                "the hit did not give " + length + " ticks of Invisibility: " + invisible);
        Asserts.assertTrue(context, smoker.hasEffect(MobEffects.HUNGER), "the hit did not give Hunger");
        Asserts.assertTrue(context, !smoker.hasEffect(MobEffects.BLINDNESS), "Blindness landed with the hit, not the exhale");

        ServerPlayer dies = Players.mock(context);
        dies.setGameMode(GameType.SURVIVAL);
        smoke(dies, world, beldia);
        dies.kill();
        ServerPlayer respawned = world.getServer().getPlayerList().respawn(dies, false);

        context.runAtTickTime(EXHALE_TICKS + 5, () -> {
            MobEffectInstance blind = smoker.getEffect(MobEffects.BLINDNESS);
            int expected = Math.round(length * 0.25F);
            Asserts.assertTrue(context, blind != null, "no Blindness on the exhale");
            Asserts.assertTrue(context, blind.getDuration() <= expected && blind.getDuration() > expected - 10,
                    "the exhale's Blindness lasts " + blind.getDuration() + ", expected a quarter of the hit (" + expected + ")");
            Asserts.assertTrue(context, !respawned.hasEffect(MobEffects.BLINDNESS),
                    "a player who died before the exhale came back blind");
            context.succeed();
        });
    }

    /** Where the seeds turn up: the desert's temple and its sand, never grass or the other chests. */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void beldiaStaysInTheDesert(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        String seeds = "hempdustry:beldia_seeds";
        expect(context, world, Blocks.TALL_GRASS.getLootTable(), seeds, false, "hempdustry:sativa_seeds");
        expect(context, world, BuiltInLootTables.SIMPLE_DUNGEON, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.SHIPWRECK_SUPPLY, seeds, false, "hempdustry:indica_seeds");
        // The temple chest; 1.18.2 has no experiment pack replacing it.
        expect(context, world, BuiltInLootTables.DESERT_PYRAMID, seeds, true, "minecraft:enchanted_golden_apple");
        expect(context, world, BuiltInLootTables.DESERT_PYRAMID, "hempdustry:indica_seeds", false, null);
        // No desert well or temple archaeology: 1.18.2 has no suspicious sand to brush.
        context.succeed();
    }

    /**
     * The wild plant is the crop, ripe and whole, on sand right beside water only — and only where
     * the plant itself stands in a Beldía biome. Biomes are set with /fillbiome.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void wildBeldiaGrowsRipe(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        fillBiome(context, new BlockPos(0, 0, 0), new BlockPos(8, 4, 8), "minecraft:plains");
        // Sand on the left, sandstone on the right, a channel of water across the middle (z 4).
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                boolean channel = z == 4 && x >= 1 && x <= 7;
                context.setBlock(new BlockPos(x, 1, z), channel ? Blocks.WATER : x < 5 ? Blocks.SAND : Blocks.SANDSTONE);
                context.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                context.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
            }
        }
        var feature = world.registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY)
                .getOrThrow(ModConfiguredFeatures.BELDIA_KEY);
        java.util.Random random = new java.util.Random(11L);
        var plant = ((net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration) feature.config()).feature().value();
        var generator = world.getChunkSource().getGenerator();

        // Outside a Beldía biome, even the perfect spot is refused.
        Asserts.assertTrue(context, !plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 3))),
                "the wild Beldía feature placed a plant outside a desert");
        fillBiome(context, new BlockPos(0, 0, 0), new BlockPos(8, 4, 8), "minecraft:desert");

        // The filter, asked directly: a live world's shape updates would pop a bad plant and hide it.
        Asserts.assertTrue(context, !plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 0))),
                "the wild Beldía feature accepted sand with no water beside it");
        Asserts.assertTrue(context, !plant.place(world, generator, random, context.absolutePos(new BlockPos(6, 2, 3))),
                "the wild Beldía feature accepted sandstone beside water");
        Asserts.assertTrue(context, plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 3))),
                "the wild Beldía feature refused sand right beside water");
        context.setBlock(new BlockPos(2, 2, 3), Blocks.AIR);
        context.setBlock(new BlockPos(2, 3, 3), Blocks.AIR);

        // One ground for the plant and the wild patch alike: #hempdustry:beldia_soil, which holds sand
        // of both colours and never suspicious sand, and which both of them read, whatever the tag
        // ends up holding in a pack.
        Asserts.assertTrue(context, Blocks.SAND.defaultBlockState().is(ModTags.Blocks.BELDIA_SOIL)
                        && Blocks.RED_SAND.defaultBlockState().is(ModTags.Blocks.BELDIA_SOIL),
                "#hempdustry:beldia_soil lost sand or red sand");
        BlockState seedling = ModBlocks.BELDIA_CROP.defaultBlockState();
        for (Block ground : new Block[]{Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE,
                Blocks.GRAVEL, Blocks.DIRT, Blocks.FARMLAND}) {
            boolean soil = ground.defaultBlockState().is(ModTags.Blocks.BELDIA_SOIL);
            context.setBlock(new BlockPos(2, 1, 3), ground);
            Asserts.assertTrue(context, seedling.canSurvive(world, context.absolutePos(new BlockPos(2, 2, 3))) == soil,
                    "Beldía " + (soil ? "refuses " : "stands on ") + ground + ", which #hempdustry:beldia_soil "
                            + (soil ? "holds" : "does not hold"));
            Asserts.assertTrue(context, plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 3))) == soil,
                    "the wild Beldía feature " + (soil ? "refused " : "accepted ") + ground + " beside water, which "
                            + "#hempdustry:beldia_soil " + (soil ? "holds" : "does not hold"));
            context.setBlock(new BlockPos(2, 2, 3), Blocks.AIR);
            context.setBlock(new BlockPos(2, 3, 3), Blocks.AIR);
        }
        context.setBlock(new BlockPos(2, 1, 3), Blocks.SAND);

        for (int i = 0; i < 20; i++) {
            feature.place(world, generator, random, context.absolutePos(new BlockPos(4, 2, 4)));
        }
        int placed = 0;
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                BlockState lower = context.getBlockState(new BlockPos(x, 2, z));
                if (!lower.is(ModBlocks.BELDIA_CROP)) {
                    continue;
                }
                placed++;
                BlockState upper = context.getBlockState(new BlockPos(x, 3, z));
                BlockPos floor = new BlockPos(x, 1, z);
                boolean besideWater = false;
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    besideWater |= context.getBlockState(floor.relative(side)).is(Blocks.WATER);
                }
                Asserts.assertTrue(context, context.getBlockState(floor).is(Blocks.SAND) && besideWater,
                        "wild Beldía grew away from sand beside water at x " + x + " z " + z);
                Asserts.assertTrue(context, lower.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.LOWER
                        && lower.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE, "wild Beldía was placed unripe: " + lower);
                Asserts.assertTrue(context, upper.is(ModBlocks.BELDIA_CROP) && upper.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.UPPER,
                        "wild Beldía has no top half: " + upper);
            }
        }
        Asserts.assertTrue(context, placed > 0, "twenty Beldía patches beside water placed nothing");
        context.succeed();
    }

    // ----- helpers -----

    private static void fillBiome(GameTestHelper context, BlockPos from, BlockPos to, String biome) {
        BlockPos a = context.absolutePos(from);
        BlockPos b = context.absolutePos(to);
        // 1.18.2 has no /fillbiome (1.19.3): each 4x4x4 cell is written into its chunk section, as the
        // command does, and the chunk marked for saving.
        ServerLevel world = context.getLevel();
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> holder = world.registryAccess()
                .registryOrThrow(Registry.BIOME_REGISTRY)
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(biome)));
        for (int x = a.getX() >> 2; x <= b.getX() >> 2; x++) {
            for (int y = a.getY() >> 2; y <= b.getY() >> 2; y++) {
                for (int z = a.getZ() >> 2; z <= b.getZ() >> 2; z++) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = world.getChunk(x >> 2, z >> 2);
                    chunk.getSection(chunk.getSectionIndex(y << 2)).getBiomes().getAndSetUnchecked(x & 3, y & 3, z & 3, holder);
                    chunk.setUnsaved(true);
                }
            }
        }
    }

    private static void plant(ServerPlayer player, ServerLevel world, BlockPos soil, ItemStack seeds) {
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        player.gameMode.useItemOn(player, world, seeds, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false));
    }

    private static void smoke(ServerPlayer player, ServerLevel world, Holder<Strain> strain) {
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        ModComponents.SMOKE_CONTENTS.set(pipe, SmokeContents.of(strain, 1));
        ModComponents.CHARGES.set(pipe, DeviceType.PIPE.builtInBowl().hits());
        player.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        player.getCooldowns().removeCooldown(ModItems.WOODEN_PIPE);
        player.gameMode.useItem(player, world, pipe, InteractionHand.MAIN_HAND);
    }

    private static BlockState ripe(Block crop) {
        return Defoliation.unworked(crop.defaultBlockState())
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
    }

    private static LootTable table(ServerLevel world, Block block) {
        return world.getServer().getLootTables().get(block.getLootTable());
    }

    /** {buds, leaves, seeds, stems} from one roll. */
    private static int[] harvest(LootTable table, ServerLevel world, Vec3 origin, BlockState state, long seed,
                                 Item buds, Item seeds) {
        LootContext params = new LootContext.Builder(world)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withRandom(new java.util.Random(seed))
                .create(LootContextParamSets.BLOCK);
        int[] counts = new int[4];
        for (ItemStack stack : table.getRandomItems(params)) {
            if (stack.is(buds)) {
                counts[0] += stack.getCount();
            } else if (stack.is(ModItems.HEMP_LEAF)) {
                counts[1] += stack.getCount();
            } else if (stack.is(seeds)) {
                counts[2] += stack.getCount();
            } else if (stack.is(ModItems.HEMP_STEM)) {
                counts[3] += stack.getCount();
            }
        }
        return counts;
    }

    private static com.google.gson.JsonElement encode(ServerLevel world, ResourceLocation key) {
        LootTable table = world.getServer().getLootTables().get(key);
        return LootTables.serialize(table);
    }

    /** As in WarpedKushGameTest: the encoded table names {@code item}, or does not, beside a control that it must. */
    private static void expect(GameTestHelper context, ServerLevel world, ResourceLocation key, String item,
                               boolean present, String control) {
        String json = encode(world, key).toString();
        Asserts.assertTrue(context, json.contains("\"" + item + "\"") == present,
                key + (present ? " does not name " : " names ") + item);
        if (control != null) {
            Asserts.assertTrue(context, json.contains("\"" + control + "\""), key + " does not name its control " + control);
        }
    }
}
