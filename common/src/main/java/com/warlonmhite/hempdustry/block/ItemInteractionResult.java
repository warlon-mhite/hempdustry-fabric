package com.warlonmhite.hempdustry.block;

import java.util.function.Supplier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/**
 * Minecraft 1.20.5's answer to a click on a block with an item in hand, kept on 1.20.1 so a block's two
 * halves read as they do on the newer lines.
 *
 * <p>1.20.1 has one {@code Block#use}; 1.20.5 split it into {@code useItemOn}, asked first with the
 * held stack, and {@code useWithoutItem}, asked only when that answers
 * {@link #PASS_TO_DEFAULT_BLOCK_INTERACTION} for the main hand. A block of the mod keeps both methods
 * and overrides {@code use} with {@link #dispatch}, which plays them out in the order the newer game
 * does ({@code ServerPlayerGameMode#useItemOn} on 1.21).
 */
public enum ItemInteractionResult {
    SUCCESS,
    CONSUME,
    CONSUME_PARTIAL,
    PASS_TO_DEFAULT_BLOCK_INTERACTION,
    SKIP_DEFAULT_BLOCK_INTERACTION,
    FAIL;

    public boolean consumesAction() {
        return this.result().consumesAction();
    }

    public static ItemInteractionResult sidedSuccess(boolean clientSide) {
        return clientSide ? SUCCESS : CONSUME;
    }

    public InteractionResult result() {
        return switch (this) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            // 1.16.5 has no CONSUME_PARTIAL; CONSUME is what it meant before 1.17 split it off.
            case CONSUME_PARTIAL -> InteractionResult.CONSUME;
            case PASS_TO_DEFAULT_BLOCK_INTERACTION, SKIP_DEFAULT_BLOCK_INTERACTION -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    /**
     * The one answer 1.20.1's {@code use} gives: the item half's, unless it passes to the block, in which
     * case (main hand only) the block half's.
     */
    public static InteractionResult dispatch(ItemInteractionResult withItem, InteractionHand hand,
                                             Supplier<InteractionResult> withoutItem) {
        if (withItem == PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == InteractionHand.MAIN_HAND) {
            return withoutItem.get();
        }
        return withItem.result();
    }
}
