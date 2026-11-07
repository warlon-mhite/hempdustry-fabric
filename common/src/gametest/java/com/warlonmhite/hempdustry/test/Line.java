package com.warlonmhite.hempdustry.test;

import com.mojang.authlib.GameProfile;
import com.warlonmhite.hempdustry.block.ItemInteractionResult;
import com.warlonmhite.hempdustry.block.ItemUse;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestAssertException;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * What the newer lines' game tests take from the game and 1.20.1's and 1.16.5's do not have, written
 * out with the same meaning, so a test reads the same on every line.
 */
final class Line {

    private Line() {
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
     * block answers it in its one {@code use} before 1.20.5, with the stack in the hand -- a cauldron's
     * washes, which 1.20.5 moved to the item half, among them. (1.16.5 has no {@code CONSUME_PARTIAL}.)
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
        return context.getEntities(type, new net.minecraft.core.BlockPos(4, 4, 4), 8.0);
    }

    /** {@code AABB#encapsulatingFullBlocks} from 1.20.5 on: the box holding both blocks whole. */
    static net.minecraft.world.phys.AABB box(net.minecraft.core.BlockPos from, net.minecraft.core.BlockPos to) {
        return new net.minecraft.world.phys.AABB(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()), Math.max(from.getX(), to.getX()) + 1,
                Math.max(from.getY(), to.getY()) + 1, Math.max(from.getZ(), to.getZ()) + 1);
    }

    /**
     * {@code ItemCooldowns#removeCooldown}, which 1.16.5 keeps on the client only, so a dedicated server
     * has no such method. A cooldown of no ticks is how its server ends one: {@code isOnCooldown}
     * answers false at once, the next tick drops the entry, and the client's packet handler reads a
     * zero duration as "removed".
     */
    static void clearCooldown(Player player, net.minecraft.world.item.Item item) {
        player.getCooldowns().addCooldown(item, 0);
    }

    /**
     * {@code FoodData#setSaturation}, which 1.16.5 keeps on the client only, so a dedicated server has no
     * such method: the saturation goes in through the food data's own save, the rest of it unchanged.
     */
    static void setSaturation(Player player, float saturation) {
        net.minecraft.nbt.CompoundTag food = new net.minecraft.nbt.CompoundTag();
        player.getFoodData().addAdditionalSaveData(food);
        food.putFloat("foodSaturationLevel", saturation);
        player.getFoodData().readAdditionalSaveData(food);
    }

    /**
     * {@code BlockEntity#load} with a hand-written save, as the newer lines read one. A 1.16.5 block
     * entity takes its position from the save it loads ({@code x}, {@code y}, {@code z}), so a save
     * without them would move it to the world's origin; they are filled in from where it stands.
     */
    static void load(net.minecraft.world.level.block.entity.BlockEntity entity, net.minecraft.nbt.CompoundTag save) {
        net.minecraft.nbt.CompoundTag placed = save.copy();
        placed.putInt("x", entity.getBlockPos().getX());
        placed.putInt("y", entity.getBlockPos().getY());
        placed.putInt("z", entity.getBlockPos().getZ());
        entity.load(entity.getBlockState(), placed);
    }
}
