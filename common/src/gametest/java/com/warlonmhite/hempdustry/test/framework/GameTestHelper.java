package com.warlonmhite.hempdustry.test.framework;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * What a test is handed: its room, in coordinates relative to the room's corner, and the test's clock.
 *
 * <p>The newer lines' {@code GameTestHelper}, method for method, for the part of it the tests use, and
 * with the same meaning: Minecraft 1.16.5 ships the class with none of its methods. A test is never
 * rotated, so a relative position is the room's corner plus the offset. The messages are built from
 * what a dedicated server has: names through their keys, since a 1.16.5 server strips the client-only
 * {@code Item#getDescription}, {@code Block#getName} and {@code Vec3i#toShortString}.
 */
public final class GameTestHelper {
    private final GameTestInfo testInfo;
    private boolean finalCheckAdded;

    GameTestHelper(GameTestInfo testInfo) {
        this.testInfo = testInfo;
    }

    public ServerLevel getLevel() {
        return testInfo.getLevel();
    }

    public BlockPos absolutePos(BlockPos pos) {
        return testInfo.getOrigin().offset(pos);
    }

    public BlockPos relativePos(BlockPos pos) {
        return pos.subtract(testInfo.getOrigin());
    }

    public Vec3 absoluteVec(Vec3 relative) {
        return Vec3.atLowerCornerOf(testInfo.getOrigin()).add(relative);
    }

    public BlockState getBlockState(BlockPos pos) {
        return getLevel().getBlockState(absolutePos(pos));
    }

    public BlockEntity getBlockEntity(BlockPos pos) {
        return getLevel().getBlockEntity(absolutePos(pos));
    }

    public void setBlock(int x, int y, int z, Block block) {
        setBlock(new BlockPos(x, y, z), block);
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        setBlock(new BlockPos(x, y, z), state);
    }

    public void setBlock(BlockPos pos, Block block) {
        setBlock(pos, block.defaultBlockState());
    }

    public void setBlock(BlockPos pos, BlockState state) {
        getLevel().setBlock(absolutePos(pos), state, 3);
    }

    /** A redstone block at {@code pos} for {@code delay} ticks, then air. */
    public void pulseRedstone(BlockPos pos, long delay) {
        setBlock(pos, Blocks.REDSTONE_BLOCK);
        runAfterDelay(delay, () -> setBlock(pos, Blocks.AIR));
    }

    public <E extends Entity> E spawn(EntityType<E> type, BlockPos pos) {
        return spawn(type, Vec3.atBottomCenterOf(pos));
    }

    public <E extends Entity> E spawn(EntityType<E> type, int x, int y, int z) {
        return spawn(type, new BlockPos(x, y, z));
    }

    public <E extends Entity> E spawn(EntityType<E> type, float x, float y, float z) {
        return spawn(type, new Vec3(x, y, z));
    }

    /** As vanilla's: a mob spawned here never despawns, and keeps every goal it was made with. */
    public <E extends Entity> E spawn(EntityType<E> type, Vec3 pos) {
        ServerLevel level = getLevel();
        E entity = type.create(level);
        if (entity == null) {
            throw new NullPointerException("Failed to create entity " + EntityType.getKey(type));
        }
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        Vec3 at = absoluteVec(pos);
        entity.moveTo(at.x, at.y, at.z, entity.yRot, entity.xRot);
        level.addFreshEntity(entity);
        return entity;
    }

    public <T extends Entity> List<T> getEntities(EntityType<T> type, BlockPos pos, double radius) {
        return getLevel().getEntities(type, new AABB(absolutePos(pos)).inflate(radius), Entity::isAlive);
    }

    public void assertItemEntityCountIs(Item item, BlockPos pos, double expansionAmount, int count) {
        BlockPos at = absolutePos(pos);
        int found = 0;
        for (ItemEntity entity : getLevel().getEntities(EntityType.ITEM, new AABB(at).inflate(expansionAmount), Entity::isAlive)) {
            ItemStack stack = entity.getItem();
            if (stack.getItem() == item) {
                found += stack.getCount();
            }
        }
        if (found != count) {
            throw atPos("Expected " + count + " " + new ItemStack(item).getHoverName().getString() + " items to exist (found " + found + ")", at, pos);
        }
    }

    public void assertItemEntityNotPresent(Item item, BlockPos pos, double radius) {
        BlockPos at = absolutePos(pos);
        for (ItemEntity entity : getLevel().getEntities(EntityType.ITEM, new AABB(at).inflate(radius), Entity::isAlive)) {
            if (entity.getItem().getItem() == item) {
                throw atPos("Did not expect " + new ItemStack(item).getHoverName().getString() + " item", at, pos);
            }
        }
    }

    public void assertBlockPresent(Block block, int x, int y, int z) {
        assertBlockPresent(block, new BlockPos(x, y, z));
    }

    public void assertBlockPresent(Block block, BlockPos pos) {
        BlockState state = getBlockState(pos);
        if (!state.is(block)) {
            throw atPos("Expected " + new TranslatableComponent(block.getDescriptionId()).getString() + ", got " + new TranslatableComponent(state.getBlock().getDescriptionId()).getString(),
                    absolutePos(pos), pos);
        }
    }

    public void assertTrue(boolean condition, String failureMessage) {
        if (!condition) {
            throw new GameTestAssertException(failureMessage);
        }
    }

    public void assertFalse(boolean condition, String failureMessage) {
        if (condition) {
            throw new GameTestAssertException(failureMessage);
        }
    }

    public void fail(String message) {
        throw new GameTestAssertException(message);
    }

    public void succeed() {
        testInfo.succeed();
    }

    /** Passes on the first tick {@code criterion} runs without an assertion failing; at the timeout, its last failure is the test's. */
    public void succeedWhen(Runnable criterion) {
        if (finalCheckAdded) {
            throw new IllegalStateException("This test already has final clause");
        }
        finalCheckAdded = true;
        testInfo.addFinalCheck(criterion);
    }

    public void runAtTickTime(long tickTime, Runnable task) {
        testInfo.setRunAtTickTime(tickTime, task);
    }

    public void runAfterDelay(long delay, Runnable task) {
        runAtTickTime(testInfo.getTick() + delay, task);
    }

    public long getTick() {
        return testInfo.getTick();
    }

    /** Vanilla's message for a failure at a block: what, where in the world, where in the room, and when. */
    private GameTestAssertException atPos(String message, BlockPos absolute, BlockPos relative) {
        return new GameTestAssertException(message + " at " + shortString(absolute) + " (relative: "
                + shortString(relative) + ") (t=" + testInfo.getTick() + ")");
    }

    /** {@code Vec3i#toShortString}, which 1.16.5 keeps on the client only. */
    private static String shortString(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }
}
