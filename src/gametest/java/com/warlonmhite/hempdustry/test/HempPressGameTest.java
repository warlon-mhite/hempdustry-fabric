package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.HempPressBlock;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * The Hemp Press pours its output out of its back, into whatever stands there, and keeps it when
 * nothing does.
 *
 * <p>The press is heated from the block below it, and that is the only block a hopper pulls from —
 * so without the pour no press could ever be emptied by automation. Every failure here is quiet. A
 * pour out of the wrong face fills a barrel the player put there for something else; a pour onto
 * the floor looks like a press emptying itself; and no pour at all looks like a press nobody has
 * emptied yet.
 */
public final class HempPressGameTest {

    /** Facing east, so its back is west: a face no default facing lands on by accident. */
    private static final BlockPos PRESS = new BlockPos(3, 1, 3);
    private static final BlockPos BEHIND = PRESS.west();
    private static final List<BlockPos> NOT_BEHIND = List.of(PRESS.east(), PRESS.north(), PRESS.south());
    /** A press with nothing on any side of it. */
    private static final BlockPos LONE = new BlockPos(6, 1, 6);

    /** Facing south, each behind the one before it: bubble hash in the first, the barrel last. */
    private static final BlockPos FIRST = new BlockPos(2, 1, 4);
    private static final BlockPos SECOND = FIRST.north();
    private static final BlockPos LAST = SECOND.north();

    /** A press on a lit campfire, the way one stands in a world. */
    private static HempPressBlockEntity heatedPress(TestContext context, BlockPos pos, Direction facing) {
        context.setBlockState(pos.down(), Blocks.CAMPFIRE);
        context.setBlockState(pos, ModBlocks.HEMP_PRESS.getDefaultState().with(HempPressBlock.FACING, facing));
        return context.getBlockEntity(pos, HempPressBlockEntity.class);
    }

    private static int rosinIn(TestContext context, BlockPos barrel) {
        return context.getBlockEntity(barrel, BarrelBlockEntity.class).count(ModItems.ROSIN);
    }

    private static int rosinOnTheFloor(TestContext context) {
        int total = 0;
        for (ItemEntity entity : context.getEntities(EntityType.ITEM)) {
            if (entity.getStack().isOf(ModItems.ROSIN)) {
                total += entity.getStack().getCount();
            }
        }
        return total;
    }

    /**
     * Barrels on all four sides of a heated press: only the one behind it fills, one item at a time
     * until the slot is empty. A second press with nothing round it keeps its rosin and drops none.
     */
    public static void thePressPoursOutOfItsBack(TestContext context) {
        HempPressBlockEntity press = heatedPress(context, PRESS, Direction.EAST);
        context.setBlockState(BEHIND, Blocks.BARREL);
        NOT_BEHIND.forEach(pos -> context.setBlockState(pos, Blocks.BARREL));
        press.setStack(HempPressBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.ROSIN, 3));

        HempPressBlockEntity lone = heatedPress(context, LONE, Direction.EAST);
        lone.setStack(HempPressBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.ROSIN, 3));

        // One item every eight ticks from the first: all three are out by tick 17, and by 40 the lone
        // press has looked behind itself twice.
        context.runAtTick(40, () -> {
            context.assertTrue(press.isHeated(), "the campfire under the press does not heat it, so this rig tests nothing");
            context.assertEquals(3, rosinIn(context, BEHIND), "the rosin in the barrel behind the press");
            context.assertEquals(0, NOT_BEHIND.stream().mapToInt(pos -> rosinIn(context, pos)).sum(),
                    "the rosin poured out of the front or a side");
            context.assertTrue(press.getStack(HempPressBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "the press still holds " + press.getStack(HempPressBlockEntity.OUTPUT_SLOT));
            context.assertEquals(3, lone.getStack(HempPressBlockEntity.OUTPUT_SLOT).getCount(),
                    "the rosin kept by a press with nothing behind it");
            context.assertEquals(0, rosinOnTheFloor(context), "the rosin thrown on the floor");
            context.complete();
        });
    }

    /**
     * Chaining is a layout: a press set behind another takes the filtered hashish it pours as input,
     * presses it into rosin and pours that into the barrel behind itself. Bubble hash to rosin with
     * nobody touching either press.
     */
    public static void aPressBehindAnotherPressesWhatItPours(TestContext context) {
        HempPressBlockEntity first = heatedPress(context, FIRST, Direction.SOUTH);
        heatedPress(context, SECOND, Direction.SOUTH);
        context.setBlockState(LAST, Blocks.BARREL);
        first.setStack(HempPressBlockEntity.INPUT_SLOT, new ItemStack(ModItems.BUBBLE_HASH));

        // Retried every tick until it holds; Yarn's addFinalTask is the one that must hold on tick 0.
        context.addInstantFinalTask(() -> context.assertEquals(1, rosinIn(context, LAST),
                "the rosin in the barrel behind the second press"));
    }
}
