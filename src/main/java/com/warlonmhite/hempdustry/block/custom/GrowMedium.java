package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.util.ModTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;

/**
 * Something a hemp plant can be planted in that is not farmland: the Grow Pot and the Hydro Tray.
 *
 * <p>A medium answers the questions the growth roll asks of the ground — how wet it is, how fast it
 * drives the plant, and how likely it is to stress one — and takes a charge off itself each time a
 * plant ripens in it. The crops
 * ask through this interface rather than naming the blocks, so a third bed is one class, and a
 * plant in <em>any</em> medium is free of vanilla's crowding penalty: each has soil or solution of
 * its own.
 *
 * <p>It extends {@link Fertilizable}, because a medium is exactly a block bone meal fills back up —
 * which also means a dispenser of bone meal feeds one with no code of ours, as it does any
 * fertilizable block. {@link #tryFeed} adds the rest of the ecosystem's fertilisers on top of that
 * vanilla path, through {@code #c:fertilizers}.
 */
public interface GrowMedium extends Fertilizable {

    /**
     * The moisture the plant above reads, in place of vanilla's farmland count
     * ({@code CropBlock#getAvailableMoisture}). 10 is a lone plant on fully watered farmland.
     */
    float moisture(BlockState state);

    /** The factor this medium multiplies the growth odds by — 1.0 when it has nothing to give. */
    float speed(BlockState state);

    /** A plant has just ripened on this block: spend one charge, if there is one. */
    void spend(World world, BlockPos pos, BlockState state);

    /** Whether there is food in this bed already — bone meal on a flowering plant here overfeeds it. */
    boolean fed(BlockState state);

    /** The odds one growth step in this bed stresses a flowering plant; see {@link PlantStress}. */
    float stress(BlockState state);

    /**
     * Feeds a bed from the player's hand with <b>anything in {@code #c:fertilizers}</b> — bone meal
     * today, and whatever a modpack tags tomorrow, with no compile dependency either way.
     *
     * <p>Returns {@code null} when this click is not a feed (not a fertiliser, or the bed is full or
     * refusing), leaving the caller to fall through to {@code super.onUseWithItem} — never a bare
     * {@code PASS}, which would swallow the click (CLAUDE.md §5).
     *
     * <p>The effect is one charge whatever was used: a pack's fancier fertiliser is an alternative,
     * never an upgrade, so pack content cannot power-creep a hemp farm.
     */
    @Nullable
    static ActionResult tryFeed(ItemStack stack, BlockState state, World world, BlockPos pos,
                                PlayerEntity player) {
        if (!stack.isIn(ConventionalItemTags.FERTILIZERS)
                || !(state.getBlock() instanceof GrowMedium medium)
                || !medium.isFertilizable(world, pos, state)) {
            return null;
        }
        if (world instanceof ServerWorld server) {
            medium.grow(server, server.getRandom(), pos, state);
            stack.decrementUnlessCreative(1, player);
            // Vanilla's own "that was fertilised" feedback, the event BoneMealItem fires.
            server.syncWorldEvent(WorldEvents.BONE_MEAL_USED, pos, 0);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * Breaking a bed under a plant harvests the plant first, while the bed is still under it, and as
     * the player's own break: the loot table sees the bed and the tool in hand, and the plant's
     * {@code onBreak} feeds the harvest advancements. Otherwise the plant pops off after the bed has
     * gone, its loot no longer sees a bed, and breaking the bed becomes the way round the root-bound
     * stem — and round Sea of Green and Midnight Sun, which never fire.
     */
    static void harvestAbove(World world, BlockPos pos, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockState(pos.up()).isIn(ModTags.Blocks.HEMP_CROPS)) {
            serverPlayer.interactionManager.tryBreakBlock(pos.up());
        }
    }
}
