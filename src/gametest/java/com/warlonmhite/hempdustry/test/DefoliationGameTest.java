package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The two defoliation traps, both of which fail silently in game.
 *
 * <p>A boolean blockstate property left out of {@code setDefaultState} defaults to <b>true</b>,
 * because {@code BooleanProperty}'s value set is {@code ImmutableSet.of(true, false)} — so a seed
 * plants itself pre-trimmed and hands over the two-cut payout for free. And every growth tick runs
 * through {@code setAge}, which rebuilds the state from {@code getDefaultState()} and wipes any
 * property not carried across by {@link Defoliation#carryOver}.
 *
 * <p>Neither shows up as an error. The first only reads as "the harvest is generous"; the second as
 * "my trim didn't count", one random tick after the player did the work.
 */
public final class DefoliationGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.up();
    private static final BlockPos UPPER = CROP.up();

    /** Trap #1: the default state must be untrimmed. */
    public static void freshlyPlantedCropIsUntrimmed(TestContext context) {
        context.setBlockState(SOIL, Blocks.FARMLAND);
        context.setBlockState(CROP, ModBlocks.INDICA_CROP.getDefaultState());

        BlockState planted = context.getBlockState(CROP);
        context.assertTrue(!planted.get(Defoliation.TRIMMED_EARLY),
                "a freshly planted crop is already early-trimmed");
        context.assertTrue(!planted.get(Defoliation.TRIMMED_LATE),
                "a freshly planted crop is already late-trimmed");
        context.complete();
    }

    /** Trap #2: growth must not wipe the flags — including the tick that sprouts the upper half. */
    public static void trimSurvivesGrowthToMaturity(TestContext context) {
        context.setBlockState(SOIL, Blocks.FARMLAND);
        context.setBlockState(CROP, ModBlocks.INDICA_CROP.getDefaultState()
                .with(IndicaCropBlock.AGE, Defoliation.EARLY_MIN_AGE)
                .with(Defoliation.TRIMMED_EARLY, true));

        CropBlock crop = (CropBlock) ModBlocks.INDICA_CROP;
        BlockPos cropPos = context.getAbsolutePos(CROP);
        // One stage per call (getGrowthAmount is a flat 1), so this walks every intermediate age
        // rather than jumping the one that matters — the sprout at DOUBLE_BLOCK_AGE.
        for (int age = Defoliation.EARLY_MIN_AGE; age < IndicaCropBlock.MAX_AGE; age++) {
            crop.applyGrowth(context.getWorld(), cropPos, context.getWorld().getBlockState(cropPos));
            BlockState grown = context.getBlockState(CROP);
            context.assertTrue(grown.get(IndicaCropBlock.AGE) == age + 1,
                    "growth stalled at age " + grown.get(IndicaCropBlock.AGE));
            context.assertTrue(grown.get(Defoliation.TRIMMED_EARLY),
                    "growing to age " + (age + 1) + " wiped the early trim flag");
            context.assertTrue(!grown.get(Defoliation.TRIMMED_LATE),
                    "growing to age " + (age + 1) + " invented a late trim flag");
        }

        context.expectBlock(ModBlocks.INDICA_CROP, UPPER);
        context.assertTrue(context.getBlockState(UPPER).get(IndicaCropBlock.HALF) == DoubleBlockHalf.UPPER,
                "the second block of a mature plant is not its upper half");
        context.complete();
    }

    /** Fixed rolls per flag combination; the counts are deterministic, so this is plenty. */
    private static final int ROLLS = 50;
    /** Seeds drawn from a seeded Random, never a counter -- see TallPlantLootGameTest. */
    private static final long SEED_SOURCE = 0x4C454146L; // "LEAF"

    /**
     * Every leaf taken off a growing plant comes off its harvest, one for one, and each cut moves
     * one bud the other way.
     *
     * <p>Rolled against the real loot tables with the <b>same seed</b> for all four trim
     * combinations. Every pool consumes the same random draws whichever bucket it lands in (the
     * Fortune binomial's trial count does not depend on the base count), so the only difference
     * between two rolls on one seed is the base counts — which is exactly what is asserted.
     *
     * <p>Fails in both directions: a trim whose leaf is not taken off the harvest is a free leaf a
     * plant, and a bucket that stopped matching (a plant falling through to no leaves, or through
     * the bud pool to a single seed) takes the player's harvest away. Lemon Haze trimmed twice
     * lands on a low base, so it also proves a small base still takes its Fortune bonus rather than
     * vanishing.
     */
    public static void everyLeafTakenComesOffTheHarvest(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d origin = Vec3d.ofCenter(context.getAbsolutePos(CROP));
        checkHarvest(context, world, origin, ModBlocks.INDICA_CROP.getDefaultState()
                .with(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                .with(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE), ModItems.INDICA_BUDS);
        checkHarvest(context, world, origin, ModBlocks.SATIVA_CROP.getDefaultState()
                .with(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                .with(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE), ModItems.SATIVA_BUDS);
        context.complete();
    }

    private static void checkHarvest(TestContext context, ServerWorld world, Vec3d origin,
                                     BlockState mature, Item buds) {
        LootTable table = world.getServer().getReloadableRegistries()
                .getLootTable(mature.getBlock().getLootTableKey().orElseThrow());
        String crop = mature.getBlock().getTranslationKey();
        java.util.Random seeds = new java.util.Random(SEED_SOURCE);
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            int[] untouched = harvest(table, world, origin, Defoliation.untrimmed(mature), seed, buds);
            context.assertTrue(untouched[1] > 0, crop + ": an untouched mature plant dropped no buds");
            for (int flags = 1; flags < 4; flags++) {
                boolean early = (flags & 1) != 0;
                boolean late = (flags & 2) != 0;
                int[] worked = harvest(table, world, origin, mature
                        .with(Defoliation.TRIMMED_EARLY, early)
                        .with(Defoliation.TRIMMED_LATE, late), seed, buds);
                String which = crop + " (early=" + early + ", late=" + late + ")";
                int cuts = Integer.bitCount(flags);
                context.assertTrue(worked[0] + cuts == untouched[0], which + ": harvested "
                        + worked[0] + " leaves after " + cuts + " were taken, against "
                        + untouched[0] + " untouched — a leaf taken is not coming off the harvest");
                context.assertTrue(worked[1] == untouched[1] + cuts, which + ": harvested "
                        + worked[1] + " buds against " + untouched[1] + " untouched — a trim"
                        + " moves one bud per cut");
            }
        }
    }

    /** {leaves, buds} from one roll of a crop's loot table on a given seed. */
    private static int[] harvest(LootTable table, ServerWorld world, Vec3d origin, BlockState state,
                                 long seed, Item buds) {
        LootWorldContext params = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, origin)
                .add(LootContextParameters.BLOCK_STATE, state)
                .add(LootContextParameters.TOOL, ItemStack.EMPTY)
                .build(LootContextTypes.BLOCK);
        int[] counts = new int[2];
        for (ItemStack stack : table.generateLoot(params, seed)) {
            if (stack.isOf(ModItems.HEMP_LEAF)) {
                counts[0] += stack.getCount();
            } else if (stack.isOf(buds)) {
                counts[1] += stack.getCount();
            }
        }
        return counts;
    }
}
