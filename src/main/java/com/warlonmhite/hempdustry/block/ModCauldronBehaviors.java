package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ItemActionResult;
import net.minecraft.world.event.GameEvent;

/**
 * The wash step of the cannabutter chain, done with a plain water cauldron and no new block.
 *
 * <p>Rinsing decarboxylated hemp strips the chlorophyll and tannins that make an edible taste of
 * lawn clippings. It costs nothing in potency: cannabinoids are fat-soluble and simply don't come
 * out in water, which is the whole reason the next step is butter and not tea.
 *
 * <p><b>Water is charged by the amount washed, not per click.</b> One level rinses
 * {@value #ITEMS_PER_LEVEL} items, so a full cauldron (three levels, i.e. one bucket) covers a
 * whole 64 stack with a little to spare. A cauldron that can't afford the whole stack washes what
 * it can and leaves the rest in the player's hand, rather than refusing or silently overcharging.
 *
 * <p>This deliberately can't be automated: cauldrons aren't {@code Inventory}-based, so no hopper
 * can feed one. That matches vanilla keeping its own finishing actions — banner washing, armour
 * de-dyeing — manual even in otherwise fully automated bases.
 *
 * <p>Registered by mutating {@link CauldronBehavior#WATER_CAULDRON_BEHAVIOR}'s map, which is the
 * supported extension point; no mixin is involved.
 */
public final class ModCauldronBehaviors {
    /** Items one level of water will rinse. Three levels (a full bucket) covers a 64 stack. */
    public static final int ITEMS_PER_LEVEL = 22;

    private ModCauldronBehaviors() {
    }

    public static void registerCauldronBehaviors() {
        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map()
                .put(ModItems.DECARBOXYLATED_HEMP, ModCauldronBehaviors::washHemp);
    }

    private static ItemActionResult washHemp(net.minecraft.block.BlockState state,
                                             net.minecraft.world.World world,
                                             net.minecraft.util.math.BlockPos pos,
                                             net.minecraft.entity.player.PlayerEntity player,
                                             net.minecraft.util.Hand hand,
                                             ItemStack stack) {
        int levelsAvailable = state.get(LeveledCauldronBlock.LEVEL);
        if (levelsAvailable <= 0) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int washable = levelsAvailable * ITEMS_PER_LEVEL;
        int toWash = Math.min(stack.getCount(), washable);
        if (toWash <= 0) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int levelsUsed = Math.min(levelsAvailable, ceilDiv(toWash, ITEMS_PER_LEVEL));

        if (!world.isClient) {
            if (!player.getAbilities().creativeMode) {
                stack.decrement(toWash);
            }
            player.getInventory().offerOrDrop(new ItemStack(ModItems.WASHED_DECARBOXYLATED_HEMP, toWash));
            player.incrementStat(Stats.USE_CAULDRON);
            player.incrementStat(Stats.USED.getOrCreateStat(ModItems.DECARBOXYLATED_HEMP));

            for (int i = 0; i < levelsUsed; i++) {
                LeveledCauldronBlock.decrementFluidLevel(world.getBlockState(pos), world, pos);
            }

            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return ItemActionResult.success(world.isClient);
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
