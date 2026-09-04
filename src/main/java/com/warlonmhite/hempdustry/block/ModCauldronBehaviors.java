package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Everything the mod does with a plain water cauldron. Two jobs, no new block and no mixin.
 *
 * <p><b>Retting</b> — {@code hemp_stem} → {@code retted_hemp_stem}, 1:1. Soaking is the first of the
 * four traditional steps — <i>rouissage</i> (retting), <i>broyage</i> (breaking), <i>teillage</i>
 * (scutching), <i>peignage</i> (hackling) — and it produces <b>no fibre by itself</b>: all it does is
 * let microbes rot away the pectin gluing the long bast fibres to the woody core. The breaking and
 * scutching are the crafting grid's job, and that is where the retted stalk's 6 fibre come from
 * against a dry stalk's 4 (`ModRecipeProvider`).
 *
 * <p><b>That second step is what makes retting a sidegrade rather than a free upgrade.</b> While the
 * cauldron handed back finished fibre, it was strictly better than the crafting recipe for the price
 * of one click and some water, and water is free. Paying for the extra yield with an extra step is
 * the shape vanilla uses for the stonecutter. The knowledge is well enough regarded that France added
 * <i>les savoir-faire du chanvre textile</i> to its intangible cultural heritage inventory in 2020.
 *
 * <p><b>Washing</b> — {@code decarboxylated_hemp} → {@code washed_decarboxylated_hemp}, the middle
 * step of the cannabutter chain. Rinsing strips the chlorophyll and tannins that make an edible
 * taste of lawn clippings. It costs nothing in potency: cannabinoids are fat-soluble and simply
 * don't come out in water, which is the whole reason the next step is butter and not tea.
 *
 * <p><b>Water is charged by the amount processed, not per click.</b> Each behaviour declares how
 * many items one level covers; a cauldron that can't afford the whole stack does what it can and
 * leaves the rest in the player's hand, rather than refusing or silently overcharging.
 *
 * <p><b>Both behaviours are 1:1.</b> A cauldron changes what an item <em>is</em>; it never multiplies
 * one. Anything that pays out more than it took in belongs on a recipe, where a recipe viewer can
 * show it.
 *
 * <p>Neither can be automated: cauldrons aren't {@code Inventory}-based, so no hopper can feed one.
 * That matches vanilla keeping its own finishing actions — banner washing, armour de-dyeing — manual
 * even in otherwise fully automated bases, and it is what keeps the dry crafting route worth having:
 * retting is strictly hand work.
 *
 * <p>Registered by mutating {@link CauldronBehavior#WATER_CAULDRON_BEHAVIOR}'s map, which is the
 * supported extension point.
 */
public final class ModCauldronBehaviors {
    /** Decarboxylated hemp one level of water will rinse. Three levels (a bucket) covers a 64 stack. */
    public static final int WASH_PER_LEVEL = 22;

    /** Stems one level of water will ret. Fewer than a rinse — soaking a stalk takes more than a wash. */
    public static final int RET_PER_LEVEL = 16;

    private ModCauldronBehaviors() {
    }

    public static void registerCauldronBehaviors() {
        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map()
                .put(ModItems.HEMP_STEM, (state, world, pos, player, hand, stack) ->
                        soak(state, world, pos, player, stack,
                                ModItems.RETTED_HEMP_STEM, RET_PER_LEVEL));
        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map()
                .put(ModItems.DECARBOXYLATED_HEMP, (state, world, pos, player, hand, stack) ->
                        soak(state, world, pos, player, stack,
                                ModItems.WASHED_DECARBOXYLATED_HEMP, WASH_PER_LEVEL));
    }

    /**
     * Turns as much of {@code stack} into {@code output} as the cauldron's water can cover, one for
     * one, and spends the water levels used.
     */
    private static ActionResult soak(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                         ItemStack stack, Item output, int itemsPerLevel) {
        int levelsAvailable = state.get(LeveledCauldronBlock.LEVEL);
        if (levelsAvailable <= 0) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }

        int toProcess = Math.min(stack.getCount(), levelsAvailable * itemsPerLevel);
        if (toProcess <= 0) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }
        int levelsUsed = Math.min(levelsAvailable, ceilDiv(toProcess, itemsPerLevel));

        if (!world.isClient()) {
            Item input = stack.getItem();
            if (player.getAbilities().creativeMode) {
                // Vanilla's creative rule for an exchange at a cauldron (ItemUsage#exchangeStack,
                // creativeOverride): the input is never spent, so the output is only handed over
                // when the player has none — otherwise every click mints another stack out of
                // nothing. One item, not the batch: nothing was consumed to pay for a batch.
                ItemStack one = new ItemStack(output);
                if (!player.getInventory().contains(one)) {
                    player.getInventory().insertStack(one);
                }
            } else {
                stack.decrement(toProcess);
                // toProcess is capped by the input stack, so this never exceeds one legal stack.
                player.getInventory().offerOrDrop(new ItemStack(output, toProcess));
            }
            player.incrementStat(Stats.USE_CAULDRON);
            player.incrementStat(Stats.USED.getOrCreateStat(input));

            for (int i = 0; i < levelsUsed; i++) {
                LeveledCauldronBlock.decrementFluidLevel(world.getBlockState(pos), world, pos);
            }

            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return ActionResult.SUCCESS;
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
