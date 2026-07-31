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
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Everything the mod does with a plain water cauldron. Two jobs, no new block and no mixin.
 *
 * <p><b>Retting</b> — {@code hemp_stem} → {@code hemp_fiber}. This is the real first step of fibre
 * processing: stalks are soaked so microbes rot away the pectin gluing the long bast fibres to the
 * woody core, before breaking, scutching and hackling separate them out. It is the reason retting
 * yields <em>more</em> fibre here ({@value #FIBER_PER_RETTED_STEM}) than the crafting recipe's 4 —
 * the crafting route is shredding a dry stalk and taking what comes loose, which is exactly as
 * wasteful as it sounds. The knowledge is well enough regarded that France added
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
 * <p>Neither can be automated: cauldrons aren't {@code Inventory}-based, so no hopper can feed one.
 * That matches vanilla keeping its own finishing actions — banner washing, armour de-dyeing — manual
 * even in otherwise fully automated bases. It also keeps the crafting recipe worth having, since
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

    /**
     * Fibre from one retted stem, against the crafting recipe's 4. The gap is the whole point: doing
     * it properly pays about 50% better, and paying for it with a trip to a cauldron rather than a
     * scarce resource is fair because the real cost here is hand work, not water. Vanilla has the
     * same shape in the stonecutter, which beats the crafting grid's ratios for the price of needing
     * a specific block. Tunable — if crafting ever looks pointless, lower this rather than raising
     * the water cost, since water is effectively free either way.
     */
    public static final int FIBER_PER_RETTED_STEM = 6;

    private ModCauldronBehaviors() {
    }

    public static void registerCauldronBehaviors() {
        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map()
                .put(ModItems.HEMP_STEM, (state, world, pos, player, hand, stack) ->
                        soak(state, world, pos, player, stack,
                                ModItems.HEMP_FIBER, FIBER_PER_RETTED_STEM, RET_PER_LEVEL));
        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.map()
                .put(ModItems.DECARBOXYLATED_HEMP, (state, world, pos, player, hand, stack) ->
                        soak(state, world, pos, player, stack,
                                ModItems.WASHED_DECARBOXYLATED_HEMP, 1, WASH_PER_LEVEL));
    }

    /**
     * Consumes as much of {@code stack} as the cauldron's water can cover, hands back
     * {@code outputPerItem} of {@code output} for each one, and spends the water levels used.
     */
    private static ItemActionResult soak(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                         ItemStack stack, Item output, int outputPerItem, int itemsPerLevel) {
        int levelsAvailable = state.get(LeveledCauldronBlock.LEVEL);
        if (levelsAvailable <= 0) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int toProcess = Math.min(stack.getCount(), levelsAvailable * itemsPerLevel);
        if (toProcess <= 0) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int levelsUsed = Math.min(levelsAvailable, ceilDiv(toProcess, itemsPerLevel));

        if (!world.isClient) {
            Item input = stack.getItem();
            if (!player.getAbilities().creativeMode) {
                stack.decrement(toProcess);
            }
            give(player, output, toProcess * outputPerItem);
            player.incrementStat(Stats.USE_CAULDRON);
            player.incrementStat(Stats.USED.getOrCreateStat(input));

            for (int i = 0; i < levelsUsed; i++) {
                LeveledCauldronBlock.decrementFluidLevel(world.getBlockState(pos), world, pos);
            }

            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }
        return ItemActionResult.success(world.isClient);
    }

    /**
     * Hands over {@code count} items, split into legal stacks. Retting multiplies its input, so a
     * full-cauldron batch can easily exceed one stack — and an {@code ItemStack} carrying more than
     * its item's max count is malformed, not merely untidy.
     */
    private static void give(PlayerEntity player, Item item, int count) {
        int max = item.getMaxCount();
        while (count > 0) {
            int batch = Math.min(count, max);
            player.getInventory().offerOrDrop(new ItemStack(item, batch));
            count -= batch;
        }
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
