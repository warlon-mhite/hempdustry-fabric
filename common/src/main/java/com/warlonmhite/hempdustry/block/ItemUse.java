package com.warlonmhite.hempdustry.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A block of the mod that answers a click with an item in hand on its own, before the click falls to
 * the block itself: Minecraft 1.20.5's {@code Block#useItemOn}, kept on 1.20.1, where the block's
 * {@code use} plays it out ({@link ItemInteractionResult#dispatch}).
 */
public interface ItemUse {
    ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                    Player player, InteractionHand hand, BlockHitResult hit);
}
