package com.warlonmhite.hempdustry.test;

import com.mojang.authlib.GameProfile;
import com.warlonmhite.hempdustry.block.ItemInteractionResult;
import com.warlonmhite.hempdustry.block.ItemUse;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * What the newer lines' game tests take from the game and 1.18.2's does not have, written out with
 * the same meaning, so a test reads the same on every line.
 */
final class Line {

    private Line() {
    }

    /**
     * One roll of {@code table} with {@code seed}: the newer lines' {@code getRandomItems(params, seed)}.
     * A 1.18.2 loot context carries its random itself, so the builder is given a seeded one for each roll.
     */
    static java.util.List<net.minecraft.world.item.ItemStack> roll(net.minecraft.world.level.storage.loot.LootTable table,
            net.minecraft.world.level.storage.loot.LootContext.Builder params,
            net.minecraft.world.level.storage.loot.parameters.LootContextParamSet set, long seed) {
        return table.getRandomItems(params.withRandom(new java.util.Random(seed)).create(set));
    }

    /** {@code GameTestHelper#assertValueEqual} from 1.20.5 on, in 1.21.1's order: the actual value first. */
    static void equal(GameTestHelper context, Object actual, Object expected, String name) {
        if (!Objects.equals(actual, expected)) {
            throw new GameTestAssertException("Expected " + name + " to be " + expected + ", but was " + actual);
        }
    }

    /**
     * {@code BlockState#useItemOn} from 1.20.5 on: the block's answer to a click with {@code stack}, before
     * the click falls to the block. A block of the mod answers it itself ({@link ItemUse}). A vanilla
     * block answers it in its one {@code use} on 1.18.2, with the stack in the hand -- a cauldron's
     * washes, which 1.20.5 moved to the item half, among them.
     */
    static ItemInteractionResult useItemOn(BlockState state, ItemStack stack, Level level, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (state.getBlock() instanceof ItemUse block) {
            return block.useItemOn(stack, state, level, hit.getBlockPos(), player, hand, hit);
        }
        ItemStack held = player.getItemInHand(hand);
        player.setItemInHand(hand, stack);
        try {
            return switch (state.use(level, player, hand, hit)) {
                case SUCCESS -> ItemInteractionResult.SUCCESS;
                case CONSUME -> ItemInteractionResult.CONSUME;
                case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
                case FAIL -> ItemInteractionResult.FAIL;
                case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            };
        } finally {
            if (player.getItemInHand(hand) == stack) {
                player.setItemInHand(hand, held);
            }
        }
    }

    /** {@code GameTestHelper#makeMockPlayer(GameType)} from 1.20.5 on: a plain player in that game mode. */
    static Player mockPlayer(GameTestHelper context, GameType mode) {
        return new Player(context.getLevel(), context.absolutePos(net.minecraft.core.BlockPos.ZERO), 0.0F,
                new GameProfile(UUID.randomUUID(), "test-mock-player")) {
            @Override
            public boolean isSpectator() {
                return mode == GameType.SPECTATOR;
            }

            @Override
            public boolean isCreative() {
                return mode == GameType.CREATIVE;
            }
        };
    }

    /** {@code GameTestHelper#getEntities(EntityType)} from 1.20.5 on: every one in the test's 8×8×8 room. */
    static <T extends net.minecraft.world.entity.Entity> java.util.List<T> entities(GameTestHelper context,
                                                                                 net.minecraft.world.entity.EntityType<T> type) {
        return Asserts.getEntities(context, type, new net.minecraft.core.BlockPos(4, 4, 4), 8.0);
    }

    /** {@code AABB#encapsulatingFullBlocks} from 1.20.5 on: the box holding both blocks whole. */
    static net.minecraft.world.phys.AABB box(net.minecraft.core.BlockPos from, net.minecraft.core.BlockPos to) {
        return new net.minecraft.world.phys.AABB(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()), Math.max(from.getX(), to.getX()) + 1,
                Math.max(from.getY(), to.getY()) + 1, Math.max(from.getZ(), to.getZ()) + 1);
    }
}
