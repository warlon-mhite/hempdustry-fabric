package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.block.ModCauldronBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The mod's water-cauldron jobs (retting, washing): Minecraft 1.16.5 hard-codes every cauldron
 * interaction in {@code CauldronBlock#use}, so ours are asked first, for an item that has one.
 */
@Mixin(CauldronBlock.class)
public class CauldronMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void hempdustry$interact(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                     BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        ModCauldronBehaviors.Interaction interaction = ModCauldronBehaviors.interaction(stack.getItem());
        if (interaction == null) {
            return;
        }
        InteractionResult result = interaction.interact(state, world, pos, player, hand, stack);
        if (result != InteractionResult.PASS) {
            cir.setReturnValue(result);
        }
    }
}
