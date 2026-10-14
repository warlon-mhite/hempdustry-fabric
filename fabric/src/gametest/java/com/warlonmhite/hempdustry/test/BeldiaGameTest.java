package com.warlonmhite.hempdustry.test;

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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
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

    public static void beldiaGrowsOnWateredSand(GameTestHelper context) {
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
        context.assertTrue(!BeldiaCropBlock.isWatered(world, soil), "dry sand reads as watered");
        context.setBlock(SOIL.east(5), Blocks.WATER);
        context.assertTrue(!BeldiaCropBlock.isWatered(world, soil), "water five blocks away waters Beldía");
        context.setBlock(SOIL.east(5), Blocks.SAND);
        context.setBlock(SOIL.east(4).above(2), Blocks.WATER);
        context.assertTrue(!BeldiaCropBlock.isWatered(world, soil), "water two blocks above the sand waters Beldía");
        context.setBlock(SOIL.east(4).above(2), Blocks.AIR);
        context.setBlock(SOIL.east(4), Blocks.WATER);
        context.assertTrue(BeldiaCropBlock.isWatered(world, soil), "water four blocks away does not water Beldía");

        // Ground: sand of any colour, or a bed of ours. Never farmland, never suspicious sand.
        context.assertTrue(seedling.canSurvive(world, crop), "Beldía cannot stand on sand");
        for (Block ok : new Block[]{Blocks.RED_SAND, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(SOIL, ok);
            context.assertTrue(seedling.canSurvive(world, crop), "Beldía cannot stand on " + ok);
        }
        for (Block no : new Block[]{Blocks.FARMLAND, Blocks.SUSPICIOUS_SAND, Blocks.SANDSTONE, Blocks.DIRT}) {
            context.setBlock(SOIL, no);
            context.assertTrue(!seedling.canSurvive(world, crop), "Beldía can stand on " + no);
        }

        // Planting through the real click: a seed goes down on watered sand, and dry sand refuses it
        // without eating the seed.
        context.setBlock(SOIL, Blocks.SAND);
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack seeds = new ItemStack(ModItems.BELDIA_SEEDS, 4);
        plant(player, world, soil, seeds);
        context.assertTrue(world.getBlockState(crop).is(ModBlocks.BELDIA_CROP), "a seed would not go down on watered sand");
        context.assertTrue(seeds.getCount() == 3, "planting did not take exactly one seed");
        context.setBlock(CROP, Blocks.AIR);
        context.setBlock(SOIL.east(4), Blocks.SAND);
        plant(player, world, soil, seeds);
        context.assertTrue(world.getBlockState(crop).isAir(), "a seed went down on dry sand");
        context.assertTrue(seeds.getCount() == 3, "dry sand ate the seed it refused");
        // A bed answers for itself: a seed goes down in a Grow Pot with no water anywhere near it.
        context.setBlock(SOIL, ModBlocks.GROW_POT);
        plant(player, world, soil, seeds);
        context.assertTrue(world.getBlockState(crop).is(ModBlocks.BELDIA_CROP), "a seed would not go down in a dry-room Grow Pot");
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
        context.assertTrue(dry.is(ModBlocks.BELDIA_CROP), "a Beldía whose water was taken away died");
        context.assertTrue(dry.getValue(IndicaCropBlock.AGE) == 0, "a dry Beldía grew to age " + dry.getValue(IndicaCropBlock.AGE));
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
        context.assertTrue(!BoneMealItem.growCrop(meal, world, crop), "a dry Beldía took bone meal");
        context.assertTrue(meal.getCount() == 4, "a dry Beldía ate the bone meal it refused");
        context.setBlock(SOIL.east(4), Blocks.WATER);
        for (int i = 0; i < 1000 && world.getBlockState(crop).getValue(IndicaCropBlock.AGE) == 0; i++) {
            world.getBlockState(crop).randomTick(world, crop, world.getRandom());
        }
        context.assertTrue(world.getBlockState(crop).getValue(IndicaCropBlock.AGE) > 0,
                "a Beldía given its water back never grew in 1000 random ticks");
        context.succeed();
    }

    public static void beldiaHarvestsLeafyAndSiftsDouble(GameTestHelper context) {
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
            context.assertTrue(b[0] == p[0] && b[1] == p[1] && b[2] == p[2] && b[3] == p[3] - 1,
                    "Beldía paid buds/leaves/seeds/stems " + b[0] + "/" + b[1] + "/" + b[2] + "/" + b[3]
                            + " against Purple Kush's " + p[0] + "/" + p[1] + "/" + p[2] + "/" + p[3]
                            + " — expected the same with one stem fewer");
        }
        context.assertTrue(new ItemStack(ModItems.BELDIA_SEEDS).is(ModTags.Items.HEMP_SEEDS), "Beldía seeds are not hemp seeds");

        // The Sifting Box: two levels a bud, so four fill the screen and three do not — and the buds
        // sit in their own tag, not in siftable/flower, where the viewer would count them at seven.
        context.assertTrue(new ItemStack(ModItems.BELDIA_BUDS).is(ModTags.Items.SIFTABLE_RESINOUS)
                        && !new ItemStack(ModItems.BELDIA_BUDS).is(ModTags.Items.SIFTABLE_FLOWER),
                "Beldía buds are in the wrong siftable tag");
        BlockPos boxRel = new BlockPos(5, 1, 5);
        BlockPos box = context.absolutePos(boxRel);
        context.setBlock(boxRel, ModBlocks.SIFTING_BOX.defaultBlockState());
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(box), Direction.UP, box, false);
        ItemStack buds = new ItemStack(ModItems.BELDIA_BUDS, 64);
        for (int i = 0; i < 3; i++) {
            world.getBlockState(box).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        }
        context.assertTrue(context.getBlockState(boxRel).getValue(SiftingBoxBlock.LEVEL) == 6,
                "three Beldía buds moved the screen to " + context.getBlockState(boxRel).getValue(SiftingBoxBlock.LEVEL) + ", expected 6");
        // A Purple Kush bud still moves it one, and shares the screen: both are plant matter.
        world.getBlockState(box).useItemOn(new ItemStack(ModItems.INDICA_BUDS, 4), world, player, InteractionHand.MAIN_HAND, hit);
        context.assertTrue(context.getBlockState(boxRel).getValue(SiftingBoxBlock.LEVEL) == SiftingBoxBlock.FULL_LEVEL,
                "a Purple Kush bud on top of three Beldía did not fill the screen");
        context.setBlock(boxRel, ModBlocks.SIFTING_BOX.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            world.getBlockState(box).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        }
        context.assertTrue(context.getBlockState(boxRel).getValue(SiftingBoxBlock.LEVEL) == SiftingBoxBlock.FULL_LEVEL,
                "four Beldía buds did not fill the screen");
        context.assertTrue(buds.getCount() == 64 - 7, "the screen took " + (64 - buds.getCount()) + " buds, expected 7");
        context.succeed();
    }

    /**
     * Mirage, through a real pipe hit: Invisibility and Hunger on the hit for the pipe's full 700
     * ticks, Blindness on the exhale for a quarter of that — and never on a player who died in the
     * window and came back.
     */
    public static void beldiaSmokesAMirage(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> beldia = world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.BELDIA);
        context.assertTrue(beldia.value().coughFactor() == 0.5F, "Beldía does not cough at twice the odds");
        int length = DeviceType.PIPE.builtInStats().durationTicks();

        ServerPlayer smoker = context.makeMockServerPlayerInLevel();
        smoker.setGameMode(GameType.SURVIVAL);
        smoke(smoker, world, beldia);
        MobEffectInstance invisible = smoker.getEffect(MobEffects.INVISIBILITY);
        context.assertTrue(invisible != null && invisible.getDuration() == length,
                "the hit did not give " + length + " ticks of Invisibility: " + invisible);
        context.assertTrue(smoker.hasEffect(MobEffects.HUNGER), "the hit did not give Hunger");
        context.assertTrue(!smoker.hasEffect(MobEffects.BLINDNESS), "Blindness landed with the hit, not the exhale");

        ServerPlayer dies = context.makeMockServerPlayerInLevel();
        dies.setGameMode(GameType.SURVIVAL);
        smoke(dies, world, beldia);
        dies.kill(world);
        ServerPlayer respawned = world.getServer().getPlayerList().respawn(dies, false, Entity.RemovalReason.KILLED);

        context.runAtTickTime(EXHALE_TICKS + 5, () -> {
            MobEffectInstance blind = smoker.getEffect(MobEffects.BLINDNESS);
            int expected = Math.round(length * 0.25F);
            context.assertTrue(blind != null, "no Blindness on the exhale");
            context.assertTrue(blind.getDuration() <= expected && blind.getDuration() > expected - 10,
                    "the exhale's Blindness lasts " + blind.getDuration() + ", expected a quarter of the hit (" + expected + ")");
            context.assertTrue(!respawned.hasEffect(MobEffects.BLINDNESS),
                    "a player who died before the exhale came back blind");
            context.succeed();
        });
    }

    /** Where the seeds turn up: the desert's temple and its sand, never grass or the other chests. */
    public static void beldiaStaysInTheDesert(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        String seeds = "hempdustry:beldia_seeds";
        expect(context, world, Blocks.TALL_GRASS.getLootTable().orElseThrow(), seeds, false, "hempdustry:sativa_seeds");
        expect(context, world, BuiltInLootTables.SIMPLE_DUNGEON, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.SHIPWRECK_SUPPLY, seeds, false, "hempdustry:indica_seeds");
        // The temple chest, unless the Villager Trade Rebalance experiment is on -- Fabric's game-test
        // world turns every experiment on. That experiment replaces chests/desert_pyramid from a feature
        // pack, Fabric reports a feature pack as DATA_PACK, and the mod never injects into a datapack's
        // table: so in such a world the temple holds no Beldía, exactly as the mineshaft and outpost
        // hold no hemp seed. Known and documented (crops.md, Beldía), not asserted away.
        if (!world.enabledFeatures().contains(net.minecraft.world.flag.FeatureFlags.TRADE_REBALANCE)) {
            expect(context, world, BuiltInLootTables.DESERT_PYRAMID, seeds, true, "minecraft:enchanted_golden_apple");
            expect(context, world, BuiltInLootTables.DESERT_PYRAMID, "hempdustry:indica_seeds", false, null);
        }
        expect(context, world, BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, seeds, true, "minecraft:arms_up_pottery_sherd");
        expect(context, world, BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, seeds, true, "minecraft:prize_pottery_sherd");
        // A brushed block keeps one item, so the seeds must be inside vanilla's pool, not beside it.
        for (ResourceKey<LootTable> key : new ResourceKey[]{BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY, BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY}) {
            JsonObject json = encode(world, key).getAsJsonObject();
            context.assertTrue(json.getAsJsonArray("pools").size() == 1,
                    key.identifier() + " has " + json.getAsJsonArray("pools").size() + " pools — brushing would drop only the first");
        }
        context.succeed();
    }

    /**
     * The wild plant is the crop, ripe and whole, on sand right beside water only — and only where
     * the plant itself stands in a Beldía biome. Biomes are set with /fillbiome.
     */
    public static void wildBeldiaGrowsRipe(GameTestHelper context) {
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
        var feature = world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModConfiguredFeatures.BELDIA_KEY).value();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(11L);
        var plant = ((net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration) feature.config()).feature().value();
        var generator = world.getChunkSource().getGenerator();

        // Outside a Beldía biome, even the perfect spot is refused.
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 3))),
                "the wild Beldía feature placed a plant outside a desert");
        fillBiome(context, new BlockPos(0, 0, 0), new BlockPos(8, 4, 8), "minecraft:desert");

        // The filter, asked directly: a live world's shape updates would pop a bad plant and hide it.
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 0))),
                "the wild Beldía feature accepted sand with no water beside it");
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(6, 2, 3))),
                "the wild Beldía feature accepted sandstone beside water");
        context.assertTrue(plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 3))),
                "the wild Beldía feature refused sand right beside water");
        context.setBlock(new BlockPos(2, 2, 3), Blocks.AIR);
        context.setBlock(new BlockPos(2, 3, 3), Blocks.AIR);

        // One ground for the plant and the wild patch alike: #hempdustry:beldia_soil, which holds sand
        // of both colours and never suspicious sand, and which both of them read, whatever the tag
        // ends up holding in a pack.
        context.assertTrue(Blocks.SAND.defaultBlockState().is(ModTags.Blocks.BELDIA_SOIL)
                        && Blocks.RED_SAND.defaultBlockState().is(ModTags.Blocks.BELDIA_SOIL),
                "#hempdustry:beldia_soil lost sand or red sand");
        context.assertTrue(!Blocks.SUSPICIOUS_SAND.defaultBlockState().is(ModTags.Blocks.BELDIA_SOIL),
                "#hempdustry:beldia_soil holds suspicious sand");
        BlockState seedling = ModBlocks.BELDIA_CROP.defaultBlockState();
        for (Block ground : new Block[]{Blocks.SAND, Blocks.RED_SAND, Blocks.SUSPICIOUS_SAND, Blocks.SANDSTONE,
                Blocks.GRAVEL, Blocks.DIRT, Blocks.FARMLAND}) {
            boolean soil = ground.defaultBlockState().is(ModTags.Blocks.BELDIA_SOIL);
            context.setBlock(new BlockPos(2, 1, 3), ground);
            context.assertTrue(seedling.canSurvive(world, context.absolutePos(new BlockPos(2, 2, 3))) == soil,
                    "Beldía " + (soil ? "refuses " : "stands on ") + ground + ", which #hempdustry:beldia_soil "
                            + (soil ? "holds" : "does not hold"));
            context.assertTrue(plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 3))) == soil,
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
                context.assertTrue(context.getBlockState(floor).is(Blocks.SAND) && besideWater,
                        "wild Beldía grew away from sand beside water at x " + x + " z " + z);
                context.assertTrue(lower.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.LOWER
                        && lower.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE, "wild Beldía was placed unripe: " + lower);
                context.assertTrue(upper.is(ModBlocks.BELDIA_CROP) && upper.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.UPPER,
                        "wild Beldía has no top half: " + upper);
            }
        }
        context.assertTrue(placed > 0, "twenty Beldía patches beside water placed nothing");
        context.succeed();
    }

    // ----- helpers -----

    private static void fillBiome(GameTestHelper context, BlockPos from, BlockPos to, String biome) {
        BlockPos a = context.absolutePos(from);
        BlockPos b = context.absolutePos(to);
        var server = context.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                "fillbiome " + a.getX() + " " + a.getY() + " " + a.getZ() + " " + b.getX() + " " + b.getY() + " " + b.getZ() + " " + biome);
    }

    private static void plant(ServerPlayer player, ServerLevel world, BlockPos soil, ItemStack seeds) {
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        player.gameMode.useItemOn(player, world, seeds, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false));
    }

    private static void smoke(ServerPlayer player, ServerLevel world, Holder<Strain> strain) {
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        pipe.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, 1));
        pipe.set(ModComponents.CHARGES, DeviceType.PIPE.builtInBowl().hits());
        player.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        player.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(ModItems.WOODEN_PIPE));
        player.gameMode.useItem(player, world, pipe, InteractionHand.MAIN_HAND);
    }

    private static BlockState ripe(Block crop) {
        return Defoliation.unworked(crop.defaultBlockState())
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
    }

    private static LootTable table(ServerLevel world, Block block) {
        return world.getServer().reloadableRegistries().getLootTable(block.getLootTable().orElseThrow());
    }

    /** {buds, leaves, seeds, stems} from one roll. */
    private static int[] harvest(LootTable table, ServerLevel world, Vec3 origin, BlockState state, long seed,
                                 Item buds, Item seeds) {
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .create(LootContextParamSets.BLOCK);
        int[] counts = new int[4];
        for (ItemStack stack : table.getRandomItems(params, seed)) {
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

    private static com.google.gson.JsonElement encode(ServerLevel world, ResourceKey<LootTable> key) {
        LootTable table = world.getServer().reloadableRegistries().getLootTable(key);
        return LootTable.DIRECT_CODEC.encodeStart(world.registryAccess().createSerializationContext(JsonOps.INSTANCE), table).getOrThrow();
    }

    /** As in WarpedKushGameTest: the encoded table names {@code item}, or does not, beside a control that it must. */
    private static void expect(GameTestHelper context, ServerLevel world, ResourceKey<LootTable> key, String item,
                               boolean present, String control) {
        String json = encode(world, key).toString();
        context.assertTrue(json.contains("\"" + item + "\"") == present,
                key.identifier() + (present ? " does not name " : " names ") + item);
        if (control != null) {
            context.assertTrue(json.contains("\"" + control + "\""), key.identifier() + " does not name its control " + control);
        }
    }
}
