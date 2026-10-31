package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.Mildew;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.AABB;

/**
 * Powdery mildew ({@link Mildew}): where it starts, how it spreads, what clears it. Every roll is
 * decided ({@link #HIT} lands under any odds above zero, {@link #MISS} under none), so each probe
 * either certainly catches or certainly does not, and what is asserted is the wiring and the
 * agreed numbers, never a measured rate.
 */
public final class MildewGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    /** A pane of glass over the plant: a roof that lets the light in, as a greenhouse's does. */
    private static final BlockPos ROOF = CROP.above(3);

    private static final RandomSource HIT = decided(0.0F, 0);
    private static final RandomSource MISS = decided(0.999F, 0);

    public static void mildewStartsOnlyUnderARoof(GameTestHelper context) {
        context.assertTrue(Mildew.ONSET == 1.0F / 150 && Mildew.SPREAD == 1.0F / 2,
                "the mildew odds are not 1 in 150 a step under a roof and 1 in 2 a spread");

        openSky(context, CROP);
        expect(context, step(context, ModBlocks.INDICA_CROP, 2, Blocks.AIR, HIT), Mildew.CLEAN,
                "a field plant under open sky");
        expect(context, step(context, ModBlocks.INDICA_CROP, 2, Blocks.OAK_LEAVES, HIT), Mildew.CLEAN,
                "a plant under a tree's leaves");
        expect(context, step(context, ModBlocks.INDICA_CROP, 2, Blocks.GLASS, MISS), Mildew.CLEAN,
                "a missed roll under a glass roof");
        expect(context, step(context, ModBlocks.INDICA_CROP, 4, Blocks.GLASS, HIT), Mildew.MILDEWED,
                "a plant under a glass roof");
        context.assertTrue(context.getBlockState(CROP.above()).getValue(Mildew.PROPERTY) == Mildew.MILDEWED,
                "a mildewed plant is drawn clean at the top: its upper half reads "
                        + context.getBlockState(CROP.above()).getValue(Mildew.PROPERTY));
        expect(context, step(context, ModBlocks.SATIVA_CROP, 5, Blocks.STONE, HIT), Mildew.MILDEWED,
                "a Lemon Haze plant under a stone roof");
        expect(context, step(context, ModBlocks.BELDIA_CROP, 1, Blocks.GLASS, HIT), Mildew.MILDEWED,
                "a Beldía seedling under a glass roof");

        // Nothing of ours claims to resist it; the tag is a datapack's to fill.
        context.assertTrue(BuiltInRegistries.BLOCK.get(ModTags.Blocks.MILDEW_RESISTANT).map(tag -> tag.size() == 0).orElse(true),
                "#hempdustry:mildew_resistant names a crop of ours, which the mod never claims");

        // Switched off: nothing catches it, and a plant that has it is clean on its next tick.
        ConfigGameTest.withConfig("{ \"world\": { \"mildew\": false } }", () -> {
            expect(context, step(context, ModBlocks.INDICA_CROP, 4, Blocks.GLASS, HIT), Mildew.CLEAN,
                    "a plant under a roof with world.mildew=false");
            plant(context, ModBlocks.INDICA_CROP, 4, Mildew.MILDEWED);
            Mildew.randomTick(context.getLevel(), context.absolutePos(CROP), MISS);
            expect(context, context.getBlockState(CROP.above()).getValue(Mildew.PROPERTY), Mildew.CLEAN,
                    "a mildewed plant's random tick with world.mildew=false");
        });
        context.succeed();
    }

    public static void mildewSpreadsOnePlantAtATime(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos sick = new BlockPos(2, 2, 2);
        BlockPos east = sick.east();
        BlockPos twoAway = sick.east(2).south(2);
        roof(context);
        for (BlockPos pos : List.of(sick, east, twoAway, sick.west())) {
            sow(context, pos, ModBlocks.INDICA_CROP, 4, Mildew.CLEAN);
        }
        sow(context, sick, ModBlocks.INDICA_CROP, 4, Mildew.MILDEWED);
        // The ripe neighbour is past it.
        sow(context, sick.west(), ModBlocks.INDICA_CROP, 7, Mildew.CLEAN);

        // The neighbour it picks, on a hit; and nothing on a miss.
        Mildew.randomTick(world, context.absolutePos(sick), decided(0.999F, EAST));
        expect(context, context.getBlockState(east).getValue(Mildew.PROPERTY), Mildew.CLEAN, "a missed spread");
        Mildew.randomTick(world, context.absolutePos(sick), decided(0.0F, EAST));
        expect(context, context.getBlockState(east).getValue(Mildew.PROPERTY), Mildew.MILDEWED, "the east neighbour, picked and hit");
        expect(context, context.getBlockState(east.above()).getValue(Mildew.PROPERTY), Mildew.MILDEWED,
                "the east neighbour's upper half");
        Mildew.randomTick(world, context.absolutePos(sick), decided(0.0F, WEST));
        expect(context, context.getBlockState(sick.west()).getValue(Mildew.PROPERTY), Mildew.CLEAN, "a ripe neighbour");

        // Every pick, all hits: a plant two blocks away is never reached — a gap stops it.
        for (int pick = 0; pick < 8; pick++) {
            Mildew.randomTick(world, context.absolutePos(sick), decided(0.0F, pick));
        }
        expect(context, context.getBlockState(twoAway).getValue(Mildew.PROPERTY), Mildew.CLEAN,
                "a plant two blocks from the mildewed one");

        // Out from under the roof, a neighbour does not catch it.
        context.setBlock(east.above(3), Blocks.AIR);
        openSky(context, east);
        sow(context, east, ModBlocks.INDICA_CROP, 4, Mildew.CLEAN);
        Mildew.randomTick(world, context.absolutePos(sick), decided(0.0F, EAST));
        expect(context, context.getBlockState(east).getValue(Mildew.PROPERTY), Mildew.CLEAN, "a neighbour under open sky");
        context.succeed();
    }

    /**
     * Vanilla's sulfur gas: a potent sulfur block under a water source gives off gas at the surface,
     * and every plant within three blocks of it with nothing solid between is in it.
     */
    public static void sulfurGasClearsAndPreventsMildew(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        // The pool: potent sulfur at y 1, one water source over it, the gas rising at y 3.
        BlockPos sulfur = new BlockPos(1, 1, 4);
        // The sulfur first: the water landing on it is the neighbour update that wets it.
        context.setBlock(sulfur, Blocks.POTENT_SULFUR);
        context.setBlock(sulfur.above(), Blocks.WATER);
        PotentSulfurState gas = context.getBlockState(sulfur).getValue(PotentSulfurBlock.STATE);
        context.assertTrue(gas == PotentSulfurState.WET || gas == PotentSulfurState.DORMANT,
                "potent sulfur under water is " + gas + ", not giving off gas, so this probe proves nothing");
        BlockPos near = new BlockPos(2, 2, 3);
        roof(context);

        BlockPos lower = context.absolutePos(near);
        sow(context, near, ModBlocks.INDICA_CROP, 2, Mildew.MILDEWED);
        Mildew.randomTick(world, lower, HIT);
        expect(context, context.getBlockState(near).getValue(Mildew.PROPERTY), Mildew.CLEAN, "a young mildewed plant in the gas");
        sow(context, near, ModBlocks.INDICA_CROP, 5, Mildew.MILDEWED);
        Mildew.randomTick(world, lower, HIT);
        expect(context, context.getBlockState(near).getValue(Mildew.PROPERTY), Mildew.SULFURED, "a flowering mildewed plant in the gas");
        expect(context, context.getBlockState(near.above()).getValue(Mildew.PROPERTY), Mildew.SULFURED,
                "the upper half of a flowering plant in the gas");
        sow(context, near, ModBlocks.INDICA_CROP, 4, Mildew.CLEAN);
        Mildew.randomTick(world, lower, HIT);
        expect(context, context.getBlockState(near).getValue(Mildew.PROPERTY), Mildew.SULFURED, "a clean flowering plant in the gas");
        sow(context, near, ModBlocks.INDICA_CROP, 2, Mildew.CLEAN);
        Mildew.randomTick(world, lower, HIT);
        expect(context, context.getBlockState(near).getValue(Mildew.PROPERTY), Mildew.CLEAN, "a clean young plant in the gas");
        sow(context, near, ModBlocks.INDICA_CROP, 7, Mildew.CLEAN);
        Mildew.randomTick(world, lower, HIT);
        expect(context, context.getBlockState(near).getValue(Mildew.PROPERTY), Mildew.CLEAN, "a ripe plant in the gas");

        // No onset in the gas, whatever the roll.
        sow(context, near, ModBlocks.INDICA_CROP, 2, Mildew.CLEAN);
        ((CropBlock) ModBlocks.INDICA_CROP).performBonemeal(world, HIT, lower, world.getBlockState(lower));
        expect(context, context.getBlockState(near).getValue(Mildew.PROPERTY), Mildew.CLEAN, "a growth step in the gas");

        // A wall between the pool and the plant keeps the gas off it.
        BlockPos walled = new BlockPos(1, 2, 2);
        context.setBlock(walled.south(), Blocks.STONE);
        context.setBlock(walled.south().above(), Blocks.STONE);
        sow(context, walled, ModBlocks.INDICA_CROP, 5, Mildew.MILDEWED);
        Mildew.randomTick(world, context.absolutePos(walled), decided(0.999F, 0));
        expect(context, context.getBlockState(walled).getValue(Mildew.PROPERTY), Mildew.MILDEWED, "a plant walled off from the gas");

        // Too far: four blocks out from the surface.
        BlockPos far = new BlockPos(5, 2, 4);
        sow(context, far, ModBlocks.INDICA_CROP, 5, Mildew.MILDEWED);
        Mildew.randomTick(world, context.absolutePos(far), decided(0.999F, 0));
        expect(context, context.getBlockState(far).getValue(Mildew.PROPERTY), Mildew.MILDEWED, "a plant four blocks from the gas");

        // Drain the pool and the gas is gone.
        context.setBlock(sulfur.above(), Blocks.AIR);
        context.assertTrue(context.getBlockState(sulfur).getValue(PotentSulfurBlock.STATE) == PotentSulfurState.DRY,
                "potent sulfur with its water gone is " + context.getBlockState(sulfur).getValue(PotentSulfurBlock.STATE));
        sow(context, near, ModBlocks.INDICA_CROP, 5, Mildew.MILDEWED);
        Mildew.randomTick(world, lower, decided(0.999F, 0));
        expect(context, context.getBlockState(near).getValue(Mildew.PROPERTY), Mildew.MILDEWED, "a plant beside a drained pool");
        context.succeed();
    }

    /** A trim takes the diseased leaves away: the mildew goes, the window closes, and no leaf comes off. */
    public static void shearsClearMildewInATrimWindow(GameTestHelper context) {
        ServerPlayer player = Players.survival(context);
        roof(context);
        sow(context, CROP, ModBlocks.INDICA_CROP, 3, Mildew.MILDEWED);
        context.assertTrue(Players.click(context, player, CROP, new ItemStack(Items.SHEARS)).consumesAction(),
                "shears on a mildewed plant in its early window were turned down");
        expect(context, context.getBlockState(CROP).getValue(Mildew.PROPERTY), Mildew.CLEAN, "a trimmed mildewed plant");
        context.assertTrue(context.getBlockState(CROP).getValue(Defoliation.TRIMMED_EARLY),
                "the cure did not count as the early trim");
        context.assertTrue(leavesDropped(context) == 0, "a trim of a mildewed plant handed over "
                + leavesDropped(context) + " leaves; the diseased ones are thrown away");

        // The rub at 6-7 is no trim: it leaves the mildew where it is.
        sow(context, CROP, ModBlocks.INDICA_CROP, 6, Mildew.MILDEWED);
        Players.click(context, player, CROP, new ItemStack(Items.SHEARS));
        expect(context, context.getBlockState(CROP).getValue(Mildew.PROPERTY), Mildew.MILDEWED, "a rubbed mildewed plant");
        context.succeed();
    }

    // ----- helpers -----

    private static final int WEST = 3;
    private static final int EAST = 4;

    private static RandomSource decided(float value, int pick) {
        return new SingleThreadedRandomSource(0L) {
            @Override
            public float nextFloat() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return pick;
            }
        };
    }

    private static void expect(GameTestHelper context, Mildew actual, Mildew expected, String what) {
        context.assertTrue(actual == expected, what + " left the plant " + actual + ", expected " + expected);
    }

    /** One bone meal step on a fresh plant at {@code age} with {@code roof} over it, and the mildew it leaves. */
    private static Mildew step(GameTestHelper context, Block crop, int age, Block roof, RandomSource roll) {
        context.setBlock(ROOF, roof);
        plant(context, crop, age, Mildew.CLEAN);
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        ((CropBlock) crop).performBonemeal(world, roll, pos, world.getBlockState(pos));
        context.assertTrue(context.getBlockState(CROP).getValue(BlockStateProperties.AGE_7) == age + 1,
                "the step did not grow the plant, so this probe proves nothing");
        return context.getBlockState(CROP).getValue(Mildew.PROPERTY);
    }

    private static void plant(GameTestHelper context, Block crop, int age, Mildew mildew) {
        sow(context, CROP, crop, age, mildew);
    }

    /** A plant of {@code crop} at {@code age} on moist farmland at {@code pos}, its segments grown in and carrying {@code mildew}. */
    private static void sow(GameTestHelper context, BlockPos pos, Block crop, int age, Mildew mildew) {
        for (int y = 2; y >= 0; y--) {
            if (context.getBlockState(pos.above(y)).getBlock() instanceof CropBlock) {
                context.setBlock(pos.above(y), Blocks.AIR);
            }
        }
        context.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
        context.setBlock(pos, Defoliation.unworked(crop.defaultBlockState()).setValue(BlockStateProperties.AGE_7, age));
        // A random tick reconciles the segments, and MISS keeps it from growing. Sown clean, so that
        // tick cannot spread anything; whatever the gas did on it, the mildew is written after.
        ServerLevel world = context.getLevel();
        BlockPos at = context.absolutePos(pos);
        world.getBlockState(at).randomTick(world, at, MISS);
        Mildew.write(world, at, mildew);
        context.assertTrue(context.getBlockState(pos).getValue(Mildew.PROPERTY) == mildew
                        && context.getBlockState(pos).getValue(BlockStateProperties.AGE_7) == age,
                "the plant at " + pos + " did not come up as sown");
    }

    /**
     * Takes the test's own ceiling off the column over {@code plant}: the framework walls every test
     * in with barriers, one above the structure, and a barrier is a roof like any other block.
     */
    private static void openSky(GameTestHelper context, BlockPos plant) {
        context.setBlock(new BlockPos(plant.getX(), 8, plant.getZ()), Blocks.AIR);
    }

    /** A glass roof over the whole structure, three above the plants' beds. */
    private static void roof(GameTestHelper context) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                context.setBlock(new BlockPos(x, 5, z), Blocks.GLASS);
            }
        }
    }

    private static int leavesDropped(GameTestHelper context) {
        AABB box = new AABB(context.absolutePos(BlockPos.ZERO)).inflate(8);
        return context.getLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.getItem().is(ModItems.HEMP_LEAF))
                .stream().mapToInt(item -> item.getItem().getCount()).sum();
    }
}
