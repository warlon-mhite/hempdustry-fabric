package com.warlonmhite.hempdustry.test;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The four {@code GameTestHelper} checks the tests use that vanilla 1.18.2's helper does not have yet,
 * written the way the newer lines' vanilla writes them, so a test reads the same on every line.
 */
public final class Asserts {

    private Asserts() {
    }

    public static void assertTrue(GameTestHelper context, boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    public static void assertFalse(GameTestHelper context, boolean condition, String message) {
        if (condition) {
            throw new GameTestAssertException(message);
        }
    }

    /** No live dropped {@code item} within {@code radius} of {@code pos}, a room position. */
    public static void assertItemEntityNotPresent(GameTestHelper context, Item item, BlockPos pos, double radius) {
        BlockPos absolute = context.absolutePos(pos);
        Vec3 centre = Vec3.atCenterOf(absolute);
        for (ItemEntity dropped : context.getLevel().getEntities(EntityType.ITEM, new AABB(absolute).inflate(radius), Entity::isAlive)) {
            if (dropped.getItem().is(item) && dropped.position().closerThan(centre, radius)) {
                throw new GameTestAssertException("Did not expect " + item + " as an item entity near " + pos);
            }
        }
    }

    /** Every {@code type} within {@code radius} of {@code pos}, a room position. */
    public static <T extends Entity> List<T> getEntities(GameTestHelper context, EntityType<T> type, BlockPos pos, double radius) {
        BlockPos absolute = context.absolutePos(pos);
        Vec3 centre = Vec3.atCenterOf(absolute);
        return context.getLevel().getEntities(type, new AABB(absolute).inflate(radius),
                entity -> entity.position().closerThan(centre, radius));
    }
}
