package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.HempPressBlock;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

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
    private static HempPressBlockEntity heatedPress(GameTestHelper context, BlockPos pos, Direction facing) {
        context.setBlock(pos.below(), Blocks.CAMPFIRE);
        context.setBlock(pos, ModBlocks.HEMP_PRESS.defaultBlockState().setValue(HempPressBlock.FACING, facing));
        return context.getBlockEntity(pos, HempPressBlockEntity.class);
    }

    private static int rosinIn(GameTestHelper context, BlockPos barrel) {
        return context.getBlockEntity(barrel, BarrelBlockEntity.class).countItem(ModItems.ROSIN);
    }

    private static int rosinOnTheFloor(GameTestHelper context) {
        int total = 0;
        for (ItemEntity entity : context.getEntities(EntityTypes.ITEM)) {
            if (entity.getItem().is(ModItems.ROSIN)) {
                total += entity.getItem().getCount();
            }
        }
        return total;
    }

    /**
     * Barrels on all four sides of a heated press: only the one behind it fills, one item at a time
     * until the slot is empty. A second press with nothing round it keeps its rosin and drops none.
     */
    public static void thePressPoursOutOfItsBack(GameTestHelper context) {
        HempPressBlockEntity press = heatedPress(context, PRESS, Direction.EAST);
        context.setBlock(BEHIND, Blocks.BARREL);
        NOT_BEHIND.forEach(pos -> context.setBlock(pos, Blocks.BARREL));
        press.setItem(HempPressBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.ROSIN, 3));

        HempPressBlockEntity lone = heatedPress(context, LONE, Direction.EAST);
        lone.setItem(HempPressBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.ROSIN, 3));

        // One item every eight ticks from the first: all three are out by tick 17, and by 40 the lone
        // press has looked behind itself twice.
        context.runAtTickTime(40, () -> {
            context.assertTrue(press.isHeated(), "the campfire under the press does not heat it, so this rig tests nothing");
            context.assertValueEqual(3, rosinIn(context, BEHIND), "the rosin in the barrel behind the press");
            context.assertValueEqual(0, NOT_BEHIND.stream().mapToInt(pos -> rosinIn(context, pos)).sum(),
                    "the rosin poured out of the front or a side");
            context.assertTrue(press.getItem(HempPressBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "the press still holds " + press.getItem(HempPressBlockEntity.OUTPUT_SLOT));
            context.assertValueEqual(3, lone.getItem(HempPressBlockEntity.OUTPUT_SLOT).getCount(),
                    "the rosin kept by a press with nothing behind it");
            context.assertValueEqual(0, rosinOnTheFloor(context), "the rosin thrown on the floor");
            context.succeed();
        });
    }

    /**
     * Chaining is a layout: a press set behind another takes the filtered hashish it pours as input,
     * presses it into rosin and pours that into the barrel behind itself. Bubble hash to rosin with
     * nobody touching either press.
     */
    public static void aPressBehindAnotherPressesWhatItPours(GameTestHelper context) {
        HempPressBlockEntity first = heatedPress(context, FIRST, Direction.SOUTH);
        heatedPress(context, SECOND, Direction.SOUTH);
        context.setBlock(LAST, Blocks.BARREL);
        first.setItem(HempPressBlockEntity.INPUT_SLOT, new ItemStack(ModItems.BUBBLE_HASH));

        // Retried every tick until it holds; succeedIf is the one that must hold on tick 0.
        context.succeedWhen(() -> context.assertValueEqual(1, rosinIn(context, LAST),
                "the rosin in the barrel behind the second press"));
    }
}
