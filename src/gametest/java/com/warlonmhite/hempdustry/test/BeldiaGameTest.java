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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * Beldía: a sand-and-water crop, buds that sift double, a Mirage that lands in two halves, and a
 * plant found only in the desert. Every guard here fails without a sound — a seed that planted on
 * dry sand, a plant that died when its water went, a Blindness that arrived with the hit, or desert
 * seeds leaking into meadow grass would all look like working content.
 */
public final class BeldiaGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos CROP = SOIL.up();
    private static final int ROLLS = 40;
    private static final long SEED_SOURCE = 0xBE1D1AL;
    /** The pipe's exhale: {@code Smoking.EXHALE_DELAY_TICKS}, with no sound delay. */
    private static final int EXHALE_TICKS = 38;

    public static void beldiaGrowsOnWateredSand(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos soil = context.getAbsolutePos(SOIL);
        BlockPos crop = context.getAbsolutePos(CROP);
        BlockState seedling = ModBlocks.BELDIA_CROP.getDefaultState();
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 6; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlockState(new BlockPos(x, 1, z), Blocks.SAND);
            }
        }

        // The reach is farmland's: four out, level with the sand or one up. Five is too far.
        context.assertTrue(!BeldiaCropBlock.isWatered(world, soil), "dry sand reads as watered");
        context.setBlockState(SOIL.east(5), Blocks.WATER);
        context.assertTrue(!BeldiaCropBlock.isWatered(world, soil), "water five blocks away waters Beldía");
        context.setBlockState(SOIL.east(5), Blocks.SAND);
        context.setBlockState(SOIL.east(4).up(2), Blocks.WATER);
        context.assertTrue(!BeldiaCropBlock.isWatered(world, soil), "water two blocks above the sand waters Beldía");
        context.setBlockState(SOIL.east(4).up(2), Blocks.AIR);
        context.setBlockState(SOIL.east(4), Blocks.WATER);
        context.assertTrue(BeldiaCropBlock.isWatered(world, soil), "water four blocks away does not water Beldía");

        // Ground: sand of any colour, or a bed of ours. Never farmland, never suspicious sand.
        context.assertTrue(seedling.canPlaceAt(world, crop), "Beldía cannot stand on sand");
        for (Block ok : new Block[]{Blocks.RED_SAND, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlockState(SOIL, ok);
            context.assertTrue(seedling.canPlaceAt(world, crop), "Beldía cannot stand on " + ok);
        }
        for (Block no : new Block[]{Blocks.FARMLAND, Blocks.SUSPICIOUS_SAND, Blocks.SANDSTONE, Blocks.DIRT}) {
            context.setBlockState(SOIL, no);
            context.assertTrue(!seedling.canPlaceAt(world, crop), "Beldía can stand on " + no);
        }

        // Planting through the real click: a seed goes down on watered sand, and dry sand refuses it
        // without eating the seed.
        context.setBlockState(SOIL, Blocks.SAND);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        ItemStack seeds = new ItemStack(ModItems.BELDIA_SEEDS, 4);
        plant(player, world, soil, seeds);
        context.assertTrue(world.getBlockState(crop).isOf(ModBlocks.BELDIA_CROP), "a seed would not go down on watered sand");
        context.assertTrue(seeds.getCount() == 3, "planting did not take exactly one seed");
        context.setBlockState(CROP, Blocks.AIR);
        context.setBlockState(SOIL.east(4), Blocks.SAND);
        plant(player, world, soil, seeds);
        context.assertTrue(world.getBlockState(crop).isAir(), "a seed went down on dry sand");
        context.assertTrue(seeds.getCount() == 3, "dry sand ate the seed it refused");
        // A Grow Pot is always watered, with no water anywhere near it.
        context.setBlockState(SOIL, ModBlocks.GROW_POT);
        plant(player, world, soil, seeds);
        context.assertTrue(world.getBlockState(crop).isOf(ModBlocks.BELDIA_CROP), "a seed would not go down in a dry-room Grow Pot");
        context.setBlockState(CROP, Blocks.AIR);
        context.setBlockState(SOIL, Blocks.SAND);

        // Water taken away: the plant stops growing, refuses bone meal without eating it, and does not
        // die. Water back: it grows again.
        context.setBlockState(SOIL.east(4), Blocks.WATER);
        context.setBlockState(CROP, seedling);
        context.setBlockState(SOIL.east(4), Blocks.SAND);
        for (int i = 0; i < 1000; i++) {
            world.getBlockState(crop).randomTick(world, crop, world.getRandom());
        }
        BlockState dry = world.getBlockState(crop);
        context.assertTrue(dry.isOf(ModBlocks.BELDIA_CROP), "a Beldía whose water was taken away died");
        context.assertTrue(dry.get(IndicaCropBlock.AGE) == 0, "a dry Beldía grew to age " + dry.get(IndicaCropBlock.AGE));
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
        context.assertTrue(!BoneMealItem.useOnFertilizable(meal, world, crop), "a dry Beldía took bone meal");
        context.assertTrue(meal.getCount() == 4, "a dry Beldía ate the bone meal it refused");
        context.setBlockState(SOIL.east(4), Blocks.WATER);
        for (int i = 0; i < 1000 && world.getBlockState(crop).get(IndicaCropBlock.AGE) == 0; i++) {
            world.getBlockState(crop).randomTick(world, crop, world.getRandom());
        }
        context.assertTrue(world.getBlockState(crop).get(IndicaCropBlock.AGE) > 0,
                "a Beldía given its water back never grew in 1000 random ticks");
        context.complete();
    }

    public static void beldiaHarvestsLeafyAndSiftsDouble(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d origin = Vec3d.ofCenter(context.getAbsolutePos(CROP));
        context.setBlockState(SOIL, Blocks.SAND);

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
        context.assertTrue(new ItemStack(ModItems.BELDIA_SEEDS).isIn(ModTags.Items.HEMP_SEEDS), "Beldía seeds are not hemp seeds");

        // The Sifting Box: two levels a bud, so four fill the screen and three do not — and the buds
        // sit in their own tag, not in siftable/flower, where the viewer would count them at seven.
        context.assertTrue(new ItemStack(ModItems.BELDIA_BUDS).isIn(ModTags.Items.SIFTABLE_RESINOUS)
                        && !new ItemStack(ModItems.BELDIA_BUDS).isIn(ModTags.Items.SIFTABLE_FLOWER),
                "Beldía buds are in the wrong siftable tag");
        BlockPos boxRel = new BlockPos(5, 1, 5);
        BlockPos box = context.getAbsolutePos(boxRel);
        context.setBlockState(boxRel, ModBlocks.SIFTING_BOX.getDefaultState());
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(box), Direction.UP, box, false);
        ItemStack buds = new ItemStack(ModItems.BELDIA_BUDS, 64);
        for (int i = 0; i < 3; i++) {
            world.getBlockState(box).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        }
        context.assertTrue(context.getBlockState(boxRel).get(SiftingBoxBlock.LEVEL) == 6,
                "three Beldía buds moved the screen to " + context.getBlockState(boxRel).get(SiftingBoxBlock.LEVEL) + ", expected 6");
        // A Purple Kush bud still moves it one, and shares the screen: both are plant matter.
        world.getBlockState(box).onUseWithItem(new ItemStack(ModItems.INDICA_BUDS, 4), world, player, Hand.MAIN_HAND, hit);
        context.assertTrue(context.getBlockState(boxRel).get(SiftingBoxBlock.LEVEL) == SiftingBoxBlock.FULL_LEVEL,
                "a Purple Kush bud on top of three Beldía did not fill the screen");
        context.setBlockState(boxRel, ModBlocks.SIFTING_BOX.getDefaultState());
        for (int i = 0; i < 4; i++) {
            world.getBlockState(box).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        }
        context.assertTrue(context.getBlockState(boxRel).get(SiftingBoxBlock.LEVEL) == SiftingBoxBlock.FULL_LEVEL,
                "four Beldía buds did not fill the screen");
        context.assertTrue(buds.getCount() == 64 - 7, "the screen took " + (64 - buds.getCount()) + " buds, expected 7");
        context.complete();
    }

    /**
     * Mirage, through a real pipe hit: Invisibility and Hunger on the hit for the pipe's full 700
     * ticks, Blindness on the exhale for a quarter of that — and never on a player who died in the
     * window and came back.
     */
    public static void beldiaSmokesAMirage(TestContext context) {
        ServerWorld world = context.getWorld();
        RegistryEntry<Strain> beldia = world.getRegistryManager().getOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.BELDIA);
        context.assertTrue(beldia.value().coughFactor() == 0.5F, "Beldía does not cough at twice the odds");
        int length = DeviceType.PIPE.durationTicks();

        ServerPlayerEntity smoker = context.createMockCreativeServerPlayerInWorld();
        smoker.changeGameMode(GameMode.SURVIVAL);
        smoke(smoker, world, beldia);
        StatusEffectInstance invisible = smoker.getStatusEffect(StatusEffects.INVISIBILITY);
        context.assertTrue(invisible != null && invisible.getDuration() == length,
                "the hit did not give " + length + " ticks of Invisibility: " + invisible);
        context.assertTrue(smoker.hasStatusEffect(StatusEffects.HUNGER), "the hit did not give Hunger");
        context.assertTrue(!smoker.hasStatusEffect(StatusEffects.BLINDNESS), "Blindness landed with the hit, not the exhale");

        ServerPlayerEntity dies = context.createMockCreativeServerPlayerInWorld();
        dies.changeGameMode(GameMode.SURVIVAL);
        smoke(dies, world, beldia);
        dies.kill(world);
        ServerPlayerEntity respawned = world.getServer().getPlayerManager().respawnPlayer(dies, false, Entity.RemovalReason.KILLED);

        context.runAtTick(EXHALE_TICKS + 5, () -> {
            StatusEffectInstance blind = smoker.getStatusEffect(StatusEffects.BLINDNESS);
            int expected = Math.round(length * 0.25F);
            context.assertTrue(blind != null, "no Blindness on the exhale");
            context.assertTrue(blind.getDuration() <= expected && blind.getDuration() > expected - 10,
                    "the exhale's Blindness lasts " + blind.getDuration() + ", expected a quarter of the hit (" + expected + ")");
            context.assertTrue(!respawned.hasStatusEffect(StatusEffects.BLINDNESS),
                    "a player who died before the exhale came back blind");
            context.complete();
        });
    }

    /** Where the seeds turn up: the desert's temple and its sand, never grass or the other chests. */
    public static void beldiaStaysInTheDesert(TestContext context) {
        ServerWorld world = context.getWorld();
        String seeds = "hempdustry:beldia_seeds";
        expect(context, world, Blocks.TALL_GRASS.getLootTableKey().orElseThrow(), seeds, false, "hempdustry:sativa_seeds");
        expect(context, world, LootTables.SIMPLE_DUNGEON_CHEST, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, LootTables.SHIPWRECK_SUPPLY_CHEST, seeds, false, "hempdustry:indica_seeds");
        // The temple chest, unless the Villager Trade Rebalance experiment is on -- Fabric's game-test
        // world turns every experiment on. That experiment replaces chests/desert_pyramid from a feature
        // pack, Fabric reports a feature pack as DATA_PACK, and the mod never injects into a datapack's
        // table: so in such a world the temple holds no Beldía, exactly as the mineshaft and outpost
        // hold no hemp seed. Known and documented (crops.md, Beldía), not asserted away.
        if (!world.getEnabledFeatures().contains(net.minecraft.resource.featuretoggle.FeatureFlags.TRADE_REBALANCE)) {
            expect(context, world, LootTables.DESERT_PYRAMID_CHEST, seeds, true, "minecraft:enchanted_golden_apple");
            expect(context, world, LootTables.DESERT_PYRAMID_CHEST, "hempdustry:indica_seeds", false, null);
        }
        expect(context, world, LootTables.DESERT_WELL_ARCHAEOLOGY, seeds, true, "minecraft:arms_up_pottery_sherd");
        expect(context, world, LootTables.DESERT_PYRAMID_ARCHAEOLOGY, seeds, true, "minecraft:prize_pottery_sherd");
        // A brushed block keeps one item, so the seeds must be inside vanilla's pool, not beside it.
        for (RegistryKey<LootTable> key : new RegistryKey[]{LootTables.DESERT_WELL_ARCHAEOLOGY, LootTables.DESERT_PYRAMID_ARCHAEOLOGY}) {
            JsonObject json = encode(world, key).getAsJsonObject();
            context.assertTrue(json.getAsJsonArray("pools").size() == 1,
                    key.getValue() + " has " + json.getAsJsonArray("pools").size() + " pools — brushing would drop only the first");
        }
        context.complete();
    }

    /**
     * The wild plant is the crop, ripe and whole, on sand right beside water only — and only where
     * the plant itself stands in a Beldía biome. Biomes are set with /fillbiome.
     */
    public static void wildBeldiaGrowsRipe(TestContext context) {
        ServerWorld world = context.getWorld();
        fillBiome(context, new BlockPos(0, 0, 0), new BlockPos(8, 4, 8), "minecraft:plains");
        // Sand on the left, sandstone on the right, a channel of water across the middle (z 4).
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                context.setBlockState(new BlockPos(x, 0, z), Blocks.STONE);
                boolean channel = z == 4 && x >= 1 && x <= 7;
                context.setBlockState(new BlockPos(x, 1, z), channel ? Blocks.WATER : x < 5 ? Blocks.SAND : Blocks.SANDSTONE);
                context.setBlockState(new BlockPos(x, 2, z), Blocks.AIR);
                context.setBlockState(new BlockPos(x, 3, z), Blocks.AIR);
            }
        }
        var feature = world.getRegistryManager().getOrThrow(RegistryKeys.CONFIGURED_FEATURE)
                .getOrThrow(ModConfiguredFeatures.BELDIA_KEY).value();
        net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create(11L);
        var plant = ((net.minecraft.world.gen.feature.RandomPatchFeatureConfig) feature.config()).feature().value();
        var generator = world.getChunkManager().getChunkGenerator();

        // Outside a Beldía biome, even the perfect spot is refused.
        context.assertTrue(!plant.generateUnregistered(world, generator, random, context.getAbsolutePos(new BlockPos(2, 2, 3))),
                "the wild Beldía feature placed a plant outside a desert");
        fillBiome(context, new BlockPos(0, 0, 0), new BlockPos(8, 4, 8), "minecraft:desert");

        // The filter, asked directly: a live world's shape updates would pop a bad plant and hide it.
        context.assertTrue(!plant.generateUnregistered(world, generator, random, context.getAbsolutePos(new BlockPos(2, 2, 0))),
                "the wild Beldía feature accepted sand with no water beside it");
        context.assertTrue(!plant.generateUnregistered(world, generator, random, context.getAbsolutePos(new BlockPos(6, 2, 3))),
                "the wild Beldía feature accepted sandstone beside water");
        context.assertTrue(plant.generateUnregistered(world, generator, random, context.getAbsolutePos(new BlockPos(2, 2, 3))),
                "the wild Beldía feature refused sand right beside water");
        context.setBlockState(new BlockPos(2, 2, 3), Blocks.AIR);
        context.setBlockState(new BlockPos(2, 3, 3), Blocks.AIR);

        for (int i = 0; i < 20; i++) {
            feature.generate(world, generator, random, context.getAbsolutePos(new BlockPos(4, 2, 4)));
        }
        int placed = 0;
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                BlockState lower = context.getBlockState(new BlockPos(x, 2, z));
                if (!lower.isOf(ModBlocks.BELDIA_CROP)) {
                    continue;
                }
                placed++;
                BlockState upper = context.getBlockState(new BlockPos(x, 3, z));
                BlockPos floor = new BlockPos(x, 1, z);
                boolean besideWater = false;
                for (Direction side : Direction.Type.HORIZONTAL) {
                    besideWater |= context.getBlockState(floor.offset(side)).isOf(Blocks.WATER);
                }
                context.assertTrue(context.getBlockState(floor).isOf(Blocks.SAND) && besideWater,
                        "wild Beldía grew away from sand beside water at x " + x + " z " + z);
                context.assertTrue(lower.get(IndicaCropBlock.HALF) == DoubleBlockHalf.LOWER
                        && lower.get(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE, "wild Beldía was placed unripe: " + lower);
                context.assertTrue(upper.isOf(ModBlocks.BELDIA_CROP) && upper.get(IndicaCropBlock.HALF) == DoubleBlockHalf.UPPER,
                        "wild Beldía has no top half: " + upper);
            }
        }
        context.assertTrue(placed > 0, "twenty Beldía patches beside water placed nothing");
        context.complete();
    }

    // ----- helpers -----

    private static void fillBiome(TestContext context, BlockPos from, BlockPos to, String biome) {
        BlockPos a = context.getAbsolutePos(from);
        BlockPos b = context.getAbsolutePos(to);
        var server = context.getWorld().getServer();
        server.getCommandManager().parseAndExecute(server.getCommandSource().withSilent(),
                "fillbiome " + a.getX() + " " + a.getY() + " " + a.getZ() + " " + b.getX() + " " + b.getY() + " " + b.getZ() + " " + biome);
    }

    private static void plant(ServerPlayerEntity player, ServerWorld world, BlockPos soil, ItemStack seeds) {
        player.setStackInHand(Hand.MAIN_HAND, seeds);
        player.interactionManager.interactBlock(player, world, seeds, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(soil).add(0, 0.5, 0), Direction.UP, soil, false));
    }

    private static void smoke(ServerPlayerEntity player, ServerWorld world, RegistryEntry<Strain> strain) {
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        pipe.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(strain, 1));
        pipe.set(ModComponents.CHARGES, DeviceType.PIPE.bowlSize());
        player.setStackInHand(Hand.MAIN_HAND, pipe);
        player.getItemCooldownManager().remove(Registries.ITEM.getId(ModItems.WOODEN_PIPE));
        player.interactionManager.interactItem(player, world, pipe, Hand.MAIN_HAND);
    }

    private static BlockState ripe(Block crop) {
        return Defoliation.unworked(crop.getDefaultState())
                .with(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .with(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
    }

    private static LootTable table(ServerWorld world, Block block) {
        return world.getServer().getReloadableRegistries().getLootTable(block.getLootTableKey().orElseThrow());
    }

    /** {buds, leaves, seeds, stems} from one roll. */
    private static int[] harvest(LootTable table, ServerWorld world, Vec3d origin, BlockState state, long seed,
                                 Item buds, Item seeds) {
        LootWorldContext params = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, origin)
                .add(LootContextParameters.BLOCK_STATE, state)
                .add(LootContextParameters.TOOL, ItemStack.EMPTY)
                .build(LootContextTypes.BLOCK);
        int[] counts = new int[4];
        for (ItemStack stack : table.generateLoot(params, seed)) {
            if (stack.isOf(buds)) {
                counts[0] += stack.getCount();
            } else if (stack.isOf(ModItems.HEMP_LEAF)) {
                counts[1] += stack.getCount();
            } else if (stack.isOf(seeds)) {
                counts[2] += stack.getCount();
            } else if (stack.isOf(ModItems.HEMP_STEM)) {
                counts[3] += stack.getCount();
            }
        }
        return counts;
    }

    private static com.google.gson.JsonElement encode(ServerWorld world, RegistryKey<LootTable> key) {
        LootTable table = world.getServer().getReloadableRegistries().getLootTable(key);
        return LootTable.CODEC.encodeStart(world.getRegistryManager().getOps(JsonOps.INSTANCE), table).getOrThrow();
    }

    /** As in WarpedKushGameTest: the encoded table names {@code item}, or does not, beside a control that it must. */
    private static void expect(TestContext context, ServerWorld world, RegistryKey<LootTable> key, String item,
                               boolean present, String control) {
        String json = encode(world, key).toString();
        context.assertTrue(json.contains("\"" + item + "\"") == present,
                key.getValue() + (present ? " does not name " : " names ") + item);
        if (control != null) {
            context.assertTrue(json.contains("\"" + control + "\""), key.getValue() + " does not name its control " + control);
        }
    }
}
