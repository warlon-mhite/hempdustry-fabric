package com.warlonmhite.hempdustry.test;

import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.HempGrowth;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.WarpedKushCropBlock;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Warped Kush: a cannabis plant that plays by nether wart's rules. Nearly every guard fails in the
 * generous direction and without a sound — a crop that took bone meal, grew fast in a meadow,
 * dropped its seeds from tall grass or kept a lamp record would all look like working content.
 */
public final class WarpedKushGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos CROP = SOIL.above();
    private static final int ROLLS = 40;
    private static final long SEED_SOURCE = 0xB10EBE22L;

    public static void warpedKushGrowsByNetherRules(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerLevel nether = world.getServer().getLevel(Level.NETHER);
        context.assertTrue(nether != null, "this server has no Nether, so this proves nothing");
        WarpedKushCropBlock kush = (WarpedKushCropBlock) ModBlocks.WARPED_KUSH_CROP;
        BlockPos crop = context.absolutePos(CROP);
        BlockState seedling = kush.defaultBlockState();

        // Soil: warped nylium, and nothing else.
        context.setBlock(SOIL, Blocks.WARPED_NYLIUM);
        context.assertTrue(seedling.canSurvive(world, crop), "Warped Kush cannot be planted on warped nylium");
        for (var soil : new net.minecraft.world.level.block.Block[]{Blocks.FARMLAND, Blocks.CRIMSON_NYLIUM, Blocks.NETHERRACK,
                Blocks.SOUL_SAND, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(SOIL, soil);
            context.assertTrue(!seedling.canSurvive(world, crop), "Warped Kush can be planted on " + soil);
        }

        // Speed: 1-in-5 where water boils away, 1-in-20 anywhere else. Nothing crowds it.
        float here = HempGrowth.chance(kush.growthResistance(world, crop), kush.fieldMoisture(world, crop), 1.0);
        float there = HempGrowth.chance(kush.growthResistance(nether, new BlockPos(0, 64, 0)), 1.0F, 1.0);
        context.assertTrue(Math.abs(there - 0.2F) < 1e-6, "Warped Kush grows at " + there + " a tick in the Nether, expected 1-in-5");
        context.assertTrue(Math.abs(here - 0.05F) < 1e-6, "Warped Kush grows at " + here + " a tick in the overworld, expected 1-in-20");

        // Bone meal: refused, and not eaten.
        context.setBlock(SOIL, Blocks.WARPED_NYLIUM);
        context.setBlock(CROP, seedling);
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
        context.assertTrue(!BoneMealItem.growCrop(meal, world, crop), "Warped Kush took bone meal");
        context.assertTrue(meal.getCount() == 4, "Warped Kush ate the bone meal it refused");

        // No light record: a lit Grow Lamp over it, grown to ripe, and the record is still natural.
        context.setBlock(CROP.above(2), ModBlocks.GROW_LAMP.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        context.setBlock(CROP.above(3), Blocks.REDSTONE_BLOCK);
        for (int i = 0; i < IndicaCropBlock.MAX_AGE; i++) {
            BlockState state = world.getBlockState(crop);
            ((CropBlock) state.getBlock()).growCrops(world, crop, state);
        }
        BlockState ripe = world.getBlockState(crop);
        context.assertTrue(ripe.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE, "Warped Kush did not ripen");
        context.assertTrue(ripe.getValue(GrowLight.PROPERTY) == GrowLight.NATURAL,
                "Warped Kush kept a light record: " + ripe.getValue(GrowLight.PROPERTY));

        // Charas: 1-in-2 on it, whatever is overhead; the other crops keep theirs.
        context.assertTrue(Defoliation.charasOneIn(ripe) == 2, "Warped Kush rubs at 1-in-" + Defoliation.charasOneIn(ripe));
        context.assertTrue(Defoliation.charasOneIn(ModBlocks.INDICA_CROP.defaultBlockState()) == Defoliation.CHARAS_CHANCE_ONE_IN,
                "Purple Kush lost its own charas odds");

        // Darkness: a sealed stone box, so no light reaches the plant. Warped Kush lives and grows in
        // it; Purple Kush on farmland in the same box cannot even be planted (the control). The light
        // engine catches up over a few ticks, so the check waits for it.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 3; dy++) {
                    context.setBlock(CROP.offset(dx, dy, dz), Blocks.STONE);
                }
            }
        }
        context.setBlock(CROP, Blocks.AIR);
        context.setBlock(CROP.above(), Blocks.AIR);
        context.setBlock(SOIL, Blocks.FARMLAND);
        context.runAtTickTime(20, () -> {
            context.assertTrue(world.getRawBrightness(crop, 0) < 8, "the sealed box is lit at " + world.getRawBrightness(crop, 0));
            context.assertTrue(!ModBlocks.INDICA_CROP.defaultBlockState().canSurvive(world, crop),
                    "Purple Kush can be planted in the dark — the control proves nothing");
            context.setBlock(SOIL, Blocks.WARPED_NYLIUM);
            context.assertTrue(seedling.canSurvive(world, crop), "Warped Kush cannot live in the dark");
            context.setBlock(CROP, seedling);
            for (int i = 0; i < 2000 && world.getBlockState(crop).getValue(IndicaCropBlock.AGE) == 0; i++) {
                world.getBlockState(crop).randomTick(world, crop, world.getRandom());
            }
            context.assertTrue(world.getBlockState(crop).getValue(IndicaCropBlock.AGE) > 0,
                    "Warped Kush never grew in 2000 random ticks in the dark");
            context.succeed();
        });
    }

    public static void warpedKushHarvestAndItsMobs(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(CROP));
        context.setBlock(SOIL, Blocks.WARPED_NYLIUM);

        // On matched seeds, a ripe Warped Kush pays Purple Kush's harvest with one leaf fewer: every
        // pool draws the same randoms, so the difference is the base counts and nothing else.
        LootTable kush = table(world, ModBlocks.WARPED_KUSH_CROP);
        LootTable purple = table(world, ModBlocks.INDICA_CROP);
        java.util.Random seeds = new java.util.Random(SEED_SOURCE);
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            int[] k = harvest(kush, world, origin, ripe(ModBlocks.WARPED_KUSH_CROP), seed,
                    ModItems.WARPED_KUSH_BUDS, ModItems.WARPED_KUSH_SEEDS);
            int[] p = harvest(purple, world, origin, ripe(ModBlocks.INDICA_CROP), seed,
                    ModItems.INDICA_BUDS, ModItems.INDICA_SEEDS);
            context.assertTrue(k[0] == p[0] && k[1] == p[1] - 1 && k[2] == p[2] && k[3] == p[3],
                    "Warped Kush paid buds/leaves/seeds/stems " + k[0] + "/" + k[1] + "/" + k[2] + "/" + k[3]
                            + " against Purple Kush's " + p[0] + "/" + p[1] + "/" + p[2] + "/" + p[3]
                            + " — expected the same with one leaf fewer");
        }

        // The mobs, each through the tag vanilla reads.
        context.assertTrue(ripe(ModBlocks.WARPED_KUSH_CROP).is(BlockTags.HOGLIN_REPELLENTS), "hoglins do not avoid Warped Kush");
        Strider strider = EntityType.STRIDER.create(world, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        context.assertTrue(strider != null && strider.isFood(new ItemStack(ModItems.WARPED_KUSH_SEEDS)),
                "a strider will not breed on Warped Kush seeds");
        context.assertTrue(new ItemStack(ModItems.WARPED_KUSH_BUDS).is(ItemTags.PIGLIN_LOVED), "piglins do not covet Warped Kush buds");
        context.assertTrue(!new ItemStack(ModItems.WARPED_KUSH_SEEDS).is(ItemTags.PIGLIN_LOVED), "piglins covet the seeds too");
        context.assertTrue(new ItemStack(ModItems.WARPED_KUSH_SEEDS).is(ModTags.Items.HEMP_SEEDS), "Warped Kush seeds are not hemp seeds");
        context.succeed();
    }

    /** Where the seeds and buds turn up: the Nether's chests and piglins, never the overworld's. */
    public static void warpedKushStaysInTheNether(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        String seeds = "hempdustry:warped_kush_seeds";
        String buds = "hempdustry:warped_kush_buds";
        expect(context, world, Blocks.TALL_GRASS.getLootTable().orElseThrow(), seeds, false, "hempdustry:sativa_seeds");
        expect(context, world, BuiltInLootTables.SIMPLE_DUNGEON, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.SHIPWRECK_SUPPLY, seeds, false, "hempdustry:indica_seeds");
        expect(context, world, BuiltInLootTables.NETHER_BRIDGE, seeds, true, null);
        expect(context, world, BuiltInLootTables.BASTION_OTHER, seeds, true, null);
        expect(context, world, BuiltInLootTables.BASTION_TREASURE, buds, true, null);
        expect(context, world, BuiltInLootTables.PIGLIN_BARTERING, seeds, true, "minecraft:soul_sand");
        expect(context, world, BuiltInLootTables.NETHER_BRIDGE, "hempdustry:indica_seeds", false, null);
        context.succeed();
    }

    /** The wild plant is the crop, generated ripe and whole, on warped nylium only. */
    public static void wildWarpedKushGrowsRipe(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                context.setBlock(new BlockPos(x, 1, z), x < 5 ? Blocks.WARPED_NYLIUM : Blocks.NETHERRACK);
                context.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                context.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
            }
        }
        var feature = world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModConfiguredFeatures.WARPED_KUSH_KEY).value();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7L);

        // The nylium filter, asked directly. A live world's shape updates pop a crop off netherrack the
        // moment its top half lands, so counting blocks cannot see this filter -- but real chunk
        // generation runs no shape updates, and there the filter is the only thing between a warped
        // forest and ripe plants standing on bare netherrack.
        var plant = ((net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration) feature.config()).feature().value();
        var generator = world.getChunkSource().getGenerator();
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(7, 2, 8))),
                "the wild Warped Kush feature accepted a spot on netherrack");
        context.assertTrue(plant.place(world, generator, random, context.absolutePos(new BlockPos(1, 2, 8))),
                "the wild Warped Kush feature refused a clear spot on warped nylium");
        context.setBlock(new BlockPos(1, 2, 8), Blocks.AIR);
        context.setBlock(new BlockPos(1, 3, 8), Blocks.AIR);

        for (int i = 0; i < 20; i++) {
            feature.place(world, world.getChunkSource().getGenerator(), random, context.absolutePos(new BlockPos(4, 2, 4)));
        }
        int placed = 0;
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                BlockState lower = context.getBlockState(new BlockPos(x, 2, z));
                if (!lower.is(ModBlocks.WARPED_KUSH_CROP)) {
                    continue;
                }
                placed++;
                BlockState upper = context.getBlockState(new BlockPos(x, 3, z));
                context.assertTrue(x < 5, "wild Warped Kush grew on netherrack at x " + x);
                context.assertTrue(lower.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.LOWER
                                && lower.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE,
                        "wild Warped Kush was placed unripe or upside down: " + lower);
                context.assertTrue(upper.is(ModBlocks.WARPED_KUSH_CROP) && upper.getValue(IndicaCropBlock.HALF) == DoubleBlockHalf.UPPER
                        && upper.getValue(IndicaCropBlock.AGE) == IndicaCropBlock.MAX_AGE, "wild Warped Kush has no ripe top half: " + upper);
            }
        }
        context.assertTrue(placed > 0, "twenty Warped Kush patches on warped nylium placed nothing");
        context.succeed();
    }

    // ----- helpers -----

    private static BlockState ripe(net.minecraft.world.level.block.Block crop) {
        return Defoliation.unworked(crop.defaultBlockState())
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
    }

    private static LootTable table(ServerLevel world, net.minecraft.world.level.block.Block block) {
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

    /**
     * Whether the loaded table names {@code item} — read off its encoded JSON, since an injected pool
     * is invisible any other way short of rolling it thousands of times. {@code control} must be in
     * it, so a table that failed to encode or load cannot pass an "absent" check by being empty.
     */
    private static void expect(GameTestHelper context, ServerLevel world, ResourceKey<LootTable> key, String item,
                               boolean present, String control) {
        LootTable table = world.getServer().reloadableRegistries().getLootTable(key);
        String json = LootTable.DIRECT_CODEC.encodeStart(world.registryAccess().createSerializationContext(JsonOps.INSTANCE), table)
                .getOrThrow().toString();
        context.assertTrue(json.contains("\"" + item + "\"") == present,
                key.identifier() + (present ? " does not name " : " names ") + item);
        if (control != null) {
            context.assertTrue(json.contains("\"" + control + "\""), key.identifier() + " does not name its control " + control);
        }
    }
}
