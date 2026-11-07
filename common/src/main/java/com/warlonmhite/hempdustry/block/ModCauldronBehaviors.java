package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the mod does with a plain water cauldron. Two jobs, no new block. Minecraft 1.16.5 has no
 * table of cauldron interactions (1.17 added it), so {@code CauldronMixin} asks {@link #interaction}
 * before vanilla's own hard-coded ones.
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
 * <p>Neither can be automated: cauldrons aren't {@code Container}-based, so no hopper can feed one.
 * That matches vanilla keeping its own finishing actions — banner washing, armour de-dyeing — manual
 * even in otherwise fully automated bases, and it is what keeps the dry crafting route worth having:
 * retting is strictly hand work.
 *
 * <p>Registered by mutating {@link CauldronInteraction#WATER}'s map, which is the
 * supported extension point.
 */
public final class ModCauldronBehaviors {
    /** Decarboxylated hemp one level of water will rinse. Three levels (a bucket) covers a 64 stack. */
    public static final int WASH_PER_LEVEL = 22;

    /** Stems one level of water will ret. Fewer than a rinse — soaking a stalk takes more than a wash. */
    public static final int RET_PER_LEVEL = 16;

    private ModCauldronBehaviors() {
    }

    /** What one item does to a water cauldron, as the newer lines' {@code CauldronInteraction}. */
    @FunctionalInterface
    public interface Interaction {
        InteractionResult interact(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   ItemStack stack);
    }

    private static final Map<Item, Interaction> WATER = new HashMap<>();

    /** The interaction for {@code item} in a cauldron holding water, or {@code null} for vanilla's. */
    public static @Nullable Interaction interaction(Item item) {
        return WATER.get(item);
    }

    public static void registerCauldronBehaviors() {
        WATER.put(ModItems.HEMP_STEM, (state, world, pos, player, hand, stack) ->
                soak(state, world, pos, player, stack, ModItems.RETTED_HEMP_STEM, RET_PER_LEVEL));
        WATER.put(ModItems.DECARBOXYLATED_HEMP, (state, world, pos, player, hand, stack) ->
                soak(state, world, pos, player, stack, ModItems.WASHED_DECARBOXYLATED_HEMP, WASH_PER_LEVEL));

        // A dyed shirt or pair of harem pants washes out as leather does: on 1.16.5 vanilla's cauldron
        // washes anything that is a DyeableLeatherItem, which both are, so they need nothing here.
    }

    private static InteractionResult soak(BlockState state, Level world, BlockPos pos, Player player,
                                         ItemStack stack, Item output, int itemsPerLevel) {
        int levelsAvailable = state.getValue(CauldronBlock.LEVEL);
        if (levelsAvailable <= 0) {
            return InteractionResult.PASS;
        }

        int toProcess = Math.min(stack.getCount(), levelsAvailable * itemsPerLevel);
        if (toProcess <= 0) {
            return InteractionResult.PASS;
        }
        int levelsUsed = Math.min(levelsAvailable, ceilDiv(toProcess, itemsPerLevel));

        if (!world.isClientSide) {
            Item input = stack.getItem();
            if (player.abilities.instabuild) {
                // Vanilla's creative rule for an exchange at a cauldron (ItemUtils#createFilledResult,
                // creativeOverride): the input is never spent, so the output is only handed over
                // when the player has none — otherwise every click mints another stack out of
                // nothing. One item, not the batch: nothing was consumed to pay for a batch.
                ItemStack one = new ItemStack(output);
                if (!player.inventory.contains(one)) {
                    player.inventory.add(one);
                }
            } else {
                stack.shrink(toProcess);
                // toProcess is capped by the input stack, so this never exceeds one legal stack.
                player.inventory.placeItemBackInInventory(world, new ItemStack(output, toProcess));
            }
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(input));

            ((CauldronBlock) Blocks.CAULDRON).setWaterLevel(world, pos, state, levelsAvailable - levelsUsed);

            world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
