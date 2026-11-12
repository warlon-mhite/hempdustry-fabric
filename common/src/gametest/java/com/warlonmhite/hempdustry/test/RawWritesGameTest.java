package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.GrowPotBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * What a plant keeps when another mod writes it raw.
 *
 * <p>Harvesters that replant, instant fertilisers and growth soils in other mods change any {@code CropBlock}
 * with {@code level.setBlock(pos, crop.getStateForAge(age))}. That state is the crop's default with a new age:
 * no trims, no rub, no light record, a root wherever it lands, and only the one block written. Each test writes
 * one of those patterns onto a whole plant exactly as such a mod does, and asks that the plant comes out as its
 * own growth step, or a fresh planting, would have left it.
 */
public final class RawWritesGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    /** The tallest plant is three blocks; one more is checked to be empty. */
    private static final int COLUMN = 4;

    private static final List<Block> CROPS = List.of(ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP);

    /** A random whose every roll misses, so no step of ours can stress the plant by chance. */
    private static final RandomSource MISS = new SingleThreadedRandomSource(0L) {
        @Override
        public float nextFloat() {
            return 0.99F;
        }
    };

    /** A growth soil or a bee from another mod: one stage up, raw, on a plant that has been worked. */
    public static void aRawGrowthStepKeepsWhatThePlantWentThrough(GameTestHelper context) {
        for (Block crop : CROPS) {
            ground(context, wetFarmland());
            place(context, crop, root(crop, 3, GrowLight.STRESSED, true, false, false));
            raw(context, crop, CROP, 4);
            expect(context, crop, root(crop, 4, GrowLight.STRESSED, true, false, false),
                    "grown one stage raw from a trimmed, stressed age 3");
        }
        context.succeed();
    }

    /**
     * A ground that harvests what ripens on it (Just Dire Things' goo soil): it takes the plant inside the very
     * write that ripened it, and plants a seed in its place. What stands afterwards is that seedling alone,
     * never a ripe top over it, whether the plant ripened by a step of ours (bone meal) or by the soil's own
     * raw write, which is how the goo soil grows a crop.
     */
    public static void aPlantTakenAsItRipensLeavesOnlyTheReplanting(GameTestHelper context) {
        BlockPos soil = context.absolutePos(SOIL);
        for (boolean ours : new boolean[]{true, false}) {
            String how = ours ? "by bone meal" : "by a raw write";
            for (Block crop : CROPS) {
                ground(context, wetFarmland());
                place(context, crop, root(crop, IndicaCropBlock.MAX_AGE - 1, GrowLight.NATURAL, false, false, false));
                HarvestingGround.watch(soil);
                try {
                    if (ours) {
                        ((BonemealableBlock) crop).performBonemeal(context.getLevel(), MISS,
                                context.absolutePos(CROP), context.getBlockState(CROP), BonemealSource.INTERACTION);
                    } else {
                        raw(context, crop, CROP, IndicaCropBlock.MAX_AGE);
                    }
                } finally {
                    HarvestingGround.forget(soil);
                }
                context.assertTrue(!HarvestingGround.taken().isEmpty(),
                        name(crop) + " ripened " + how + " on a harvesting ground and the ground took nothing");
                expect(context, crop, crop.defaultBlockState(), "ripened " + how + " and harvested by its ground");
            }
        }
        context.succeed();
    }

    /** An instant fertiliser: straight to ripe, raw, on a potted plant, and on one with no room to grow. */
    public static void aRawRipeningGrowsTheWholePlantAndEatsFromItsPot(GameTestHelper context) {
        BlockState fedPot = ModBlocks.GROW_POT.defaultBlockState()
                .setValue(GrowPotBlock.FERTILITY, 3).setValue(GrowPotBlock.WATERED, true);
        for (Block crop : CROPS) {
            String name = name(crop);
            ground(context, fedPot);
            place(context, crop, root(crop, 3, GrowLight.STRESSED, true, false, false));
            raw(context, crop, CROP, IndicaCropBlock.MAX_AGE);
            expect(context, crop, root(crop, IndicaCropBlock.MAX_AGE, GrowLight.STRESSED, true, false, false),
                    "ripened raw from a trimmed, stressed age 3 in a Grow Pot");
            BlockState pot = context.getBlockState(SOIL);
            context.assertTrue(pot.getValue(GrowPotBlock.FERTILITY) == 2 && !pot.getValue(GrowPotBlock.WATERED),
                    name + " ripened raw in a fed, watered pot and left it " + pot + ": a plant that ripens eats once and drinks");

            // Boxed in: the same ceiling as bone meal and the random tick, one stage short of needing the block above.
            ground(context, wetFarmland());
            place(context, crop, root(crop, 1, GrowLight.NATURAL, false, false, false));
            context.setBlock(CROP.above(), Blocks.STONE);
            raw(context, crop, CROP, IndicaCropBlock.MAX_AGE);
            BlockState boxed = context.getBlockState(CROP);
            context.assertTrue(boxed == root(crop, 3, GrowLight.NATURAL, false, false, false),
                    name + " ripened raw under a block of stone came out " + boxed + ", not an age-3 stalk");
            context.assertTrue(context.getBlockState(CROP.above()).is(Blocks.STONE), name + " grew through stone");
        }
        context.succeed();
    }

    /** A harvester that replants: the root goes back to a seedling, so the rest of the plant has to go with it. */
    public static void aRawReplantStartsANewPlant(GameTestHelper context) {
        for (Block crop : CROPS) {
            ground(context, wetFarmland());
            place(context, crop, root(crop, IndicaCropBlock.MAX_AGE, GrowLight.GROW_LAMP, true, true, true));
            raw(context, crop, CROP, 0);
            // A new plant: none of the old one's trims, rub or light, and nothing standing above it.
            expect(context, crop, ((CropBlock) crop).getStateForAge(0), "replanted raw from a worked, ripe plant");
        }
        context.succeed();
    }

    /** A harvester or a scythe that takes a ripe top for a crop of its own and replants it. */
    public static void aRawWriteAboveTheRootLeavesThePlantWhole(GameTestHelper context) {
        for (Block crop : CROPS) {
            BlockState ripe = root(crop, IndicaCropBlock.MAX_AGE, GrowLight.GROW_LAMP, true, true, false);
            int height = wholePlant(crop, ripe).size();
            for (int up = 1; up < height; up++) {
                ground(context, wetFarmland());
                place(context, crop, ripe);
                raw(context, crop, CROP.above(up), 0);
                expect(context, crop, ripe, "replanted raw " + up + " block(s) above its root");
            }
        }
        context.succeed();
    }

    /**
     * The same click on the root: the harvest is taken and the age set back, every other property kept. What
     * stands is a new plant, and none of the old one's trims or rub, which a seedling could not have earned,
     * may move its harvest.
     */
    public static void aReplantingAgeWrittenOnTheRootStartsANewPlant(GameTestHelper context) {
        for (Block crop : CROPS) {
            ground(context, wetFarmland());
            place(context, crop, root(crop, IndicaCropBlock.MAX_AGE, GrowLight.GROW_LAMP, true, true, true));
            BlockPos at = context.absolutePos(CROP);
            ServerLevel world = context.getLevel();
            world.setBlockAndUpdate(at, world.getBlockState(at).setValue(BlockStateProperties.AGE_7, 0));
            expect(context, crop, root(crop, 0, GrowLight.GROW_LAMP, false, false, false),
                    "set back to age 0 on its root from a worked, ripe plant");
        }
        context.succeed();
    }

    /**
     * A right-click harvest that replants a ripe crop by setting its age back on the block clicked, keeping
     * every other property. On a block above the root that pays nothing, since the plant's loot is the root's,
     * and must not leave a seedling segment inside a ripe plant.
     */
    public static void aReplantingAgeWrittenAboveTheRootLeavesThePlantWhole(GameTestHelper context) {
        for (Block crop : CROPS) {
            BlockState ripe = root(crop, IndicaCropBlock.MAX_AGE, GrowLight.GROW_LAMP, true, true, false);
            int height = wholePlant(crop, ripe).size();
            for (int up = 1; up < height; up++) {
                ground(context, wetFarmland());
                place(context, crop, ripe);
                BlockPos at = context.absolutePos(CROP.above(up));
                ServerLevel world = context.getLevel();
                world.setBlockAndUpdate(at, world.getBlockState(at).setValue(BlockStateProperties.AGE_7, 0));
                expect(context, crop, ripe, "set back to age 0 " + up + " block(s) above its root");
            }
        }
        context.succeed();
    }

    /**
     * An instant fertiliser clicked on a block above the root: Mystical Agriculture's writes
     * {@code getStateForAge(max)} on the block clicked, and another mod may set the age alone there. Either way the
     * plant ripens from its root, as one step of its own: whole, with its trims and its light record.
     */
    public static void aRaisedAgeWrittenAboveTheRootGrowsThePlantFromItsRoot(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        for (Block crop : CROPS) {
            for (int age : tallAges(crop)) {
                BlockState start = root(crop, age, GrowLight.STRESSED, true, false, false);
                int height = wholePlant(crop, start).size();
                for (int up = 1; up < height; up++) {
                    for (boolean raw : new boolean[]{true, false}) {
                        ground(context, wetFarmland());
                        place(context, crop, start);
                        if (raw) {
                            raw(context, crop, CROP.above(up), IndicaCropBlock.MAX_AGE);
                        } else {
                            BlockPos at = context.absolutePos(CROP.above(up));
                            world.setBlockAndUpdate(at, world.getBlockState(at)
                                    .setValue(BlockStateProperties.AGE_7, IndicaCropBlock.MAX_AGE));
                        }
                        expect(context, crop, root(crop, IndicaCropBlock.MAX_AGE, GrowLight.STRESSED, true, false, false),
                                (raw ? "ripened raw " : "set to ripe ") + up + " block(s) above the root of a trimmed,"
                                        + " stressed age " + age);
                    }
                }
            }
        }
        context.succeed();
    }

    /**
     * Bone meal clicked on any block of a growing plant, as a player clicks it: one item spent per click, and the
     * plant grows one stage from its root, whole, or not at all when the crop's own roll fails (one in three).
     */
    public static void boneMealOnAnyBlockGrowsThePlantFromItsRoot(GameTestHelper context) {
        ServerPlayer player = Players.survival(context);
        for (Block crop : CROPS) {
            for (int age : tallAges(crop)) {
                BlockState start = root(crop, age, GrowLight.NATURAL, false, false, false);
                int height = wholePlant(crop, start).size();
                for (int up = 1; up < height; up++) {
                    ground(context, wetFarmland());
                    place(context, crop, start);
                    ItemStack meal = new ItemStack(Items.BONE_MEAL, 16);
                    int clicks = 0;
                    String where = name(crop) + " at age " + age + " clicked " + up + " block(s) above its root";
                    while (context.getBlockState(CROP).getValue(BlockStateProperties.AGE_7) == age && clicks < 12) {
                        Players.clickLikeAClient(context, player, CROP.above(up), meal);
                        clicks++;
                        context.assertTrue(meal.getCount() == 16 - clicks, where + ": " + (16 - meal.getCount())
                                + " bone meal spent in " + clicks + " click(s)");
                        expect(context, crop, context.getBlockState(CROP), "with bone meal, " + where);
                    }
                    int grown = context.getBlockState(CROP).getValue(BlockStateProperties.AGE_7);
                    context.assertTrue(grown == age + 1, where + ": the plant is at age " + grown + " after "
                            + clicks + " click(s), not one stage up");
                }
            }
        }
        context.succeed();
    }

    /** The ages a crop is two tall and, for the sativa, three tall at: every shape a block above the root has. */
    private static int[] tallAges(Block crop) {
        return crop instanceof SativaCropBlock
                ? new int[]{SativaCropBlock.TWO_TALL_AGE, SativaCropBlock.THREE_TALL_AGE}
                : new int[]{IndicaCropBlock.DOUBLE_BLOCK_AGE};
    }

    /**
     * Our own step writes a state that can look exactly like a raw write (an untrimmed plant with a natural
     * record), and must still count once: a potted plant that ripens under bone meal eats one charge, not two.
     */
    public static void ourOwnStepIsNotTakenForARawOne(GameTestHelper context) {
        BlockState fedPot = ModBlocks.GROW_POT.defaultBlockState()
                .setValue(GrowPotBlock.FERTILITY, 3).setValue(GrowPotBlock.WATERED, true);
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        for (Block crop : CROPS) {
            String name = name(crop);
            ground(context, fedPot);
            place(context, crop, root(crop, IndicaCropBlock.MAX_AGE - 1, GrowLight.NATURAL, false, false, false));
            ((CropBlock) crop).performBonemeal(world, MISS, pos, world.getBlockState(pos), BonemealSource.INTERACTION);
            BlockState ripe = root(crop, IndicaCropBlock.MAX_AGE, GrowLight.NATURAL, false, false, false);
            context.assertTrue(ripe == ((CropBlock) crop).getStateForAge(IndicaCropBlock.MAX_AGE),
                    "this probe needs our own step to write the very state a raw write would, and it does not");
            expect(context, crop, ripe, "ripened by bone meal");
            int fertility = context.getBlockState(SOIL).getValue(GrowPotBlock.FERTILITY);
            context.assertTrue(fertility == 2, name + " ripened by bone meal in a pot fed 3 left it at " + fertility
                    + ": its own step was counted again as a raw one");
        }
        context.succeed();
    }

    // ----- helpers -----

    private static String name(Block crop) {
        return BuiltInRegistries.BLOCK.getKey(crop).getPath();
    }

    private static BlockState wetFarmland() {
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
    }

    /**
     * Empties the column top-down, so no plant left from the last case pops or is written over, then lays the
     * ground. Glowstone beside the root keeps it lit whatever the hour, since a root updated by a neighbour
     * breaks in the dark; beside it, not over it, it is no light the plant's record counts.
     */
    private static void ground(GameTestHelper context, BlockState soil) {
        for (int up = COLUMN - 1; up >= 0; up--) {
            context.setBlock(CROP.above(up), Blocks.AIR);
        }
        context.setBlock(SOIL, soil);
        context.setBlock(CROP.east(), Blocks.GLOWSTONE);
    }

    private static BlockState root(Block crop, int age, GrowLight light, boolean early, boolean late, boolean rubbed) {
        return Defoliation.unworked(crop.defaultBlockState()).setValue(BlockStateProperties.AGE_7, age)
                .setValue(GrowLight.PROPERTY, light).setValue(Defoliation.TRIMMED_EARLY, early)
                .setValue(Defoliation.TRIMMED_LATE, late).setValue(Defoliation.RUBBED, rubbed);
    }

    /**
     * Every block of a settled plant whose root is {@code root}, bottom up: the segments its age calls for, each
     * at the root's age and with a copy of its light record, as the crop's own growth writes them.
     */
    private static List<BlockState> wholePlant(Block crop, BlockState root) {
        int age = root.getValue(BlockStateProperties.AGE_7);
        BlockState segment = crop.defaultBlockState().setValue(BlockStateProperties.AGE_7, age)
                .setValue(GrowLight.PROPERTY, root.getValue(GrowLight.PROPERTY));
        List<BlockState> blocks = new ArrayList<>(List.of(root));
        if (crop instanceof SativaCropBlock) {
            if (age >= SativaCropBlock.THREE_TALL_AGE) {
                blocks.add(segment.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.MIDDLE));
            }
            if (age >= SativaCropBlock.TWO_TALL_AGE) {
                blocks.add(segment.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.UPPER));
            }
        } else if (age >= IndicaCropBlock.DOUBLE_BLOCK_AGE) {
            blocks.add(segment.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER));
        }
        return blocks;
    }

    private static void place(GameTestHelper context, Block crop, BlockState root) {
        List<BlockState> blocks = wholePlant(crop, root);
        for (int up = 0; up < blocks.size(); up++) {
            context.setBlock(CROP.above(up), blocks.get(up));
        }
    }

    /** Exactly what a harvester, a fertiliser or a soil in another mod does: the crop's state for an age, written with updates. */
    private static void raw(GameTestHelper context, Block crop, BlockPos at, int age) {
        context.getLevel().setBlockAndUpdate(context.absolutePos(at), ((CropBlock) crop).getStateForAge(age));
    }

    private static void expect(GameTestHelper context, Block crop, BlockState root, String what) {
        List<BlockState> blocks = wholePlant(crop, root);
        for (int up = 0; up < COLUMN; up++) {
            BlockState want = up < blocks.size() ? blocks.get(up) : Blocks.AIR.defaultBlockState();
            BlockState got = context.getBlockState(CROP.above(up));
            context.assertTrue(got == want, name(crop) + " " + what + ": block " + up + " above the soil is " + got
                    + ", expected " + want);
        }
    }
}
