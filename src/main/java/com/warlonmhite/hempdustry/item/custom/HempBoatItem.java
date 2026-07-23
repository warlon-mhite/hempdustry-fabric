package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.entity.custom.HempBoatEntity;
import com.warlonmhite.hempdustry.entity.custom.HempChestBoatEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;
import java.util.function.Predicate;

/**
 * Mirrors vanilla {@code BoatItem}, but spawns {@link HempBoatEntity}/{@link HempChestBoatEntity} instead of
 * relying on the closed {@code BoatEntity.Type} enum, which mods can't extend.
 */
public class HempBoatItem extends Item {
    private static final Predicate<Entity> RIDERS = EntityPredicates.EXCEPT_SPECTATOR.and(Entity::canHit);
    private final boolean chest;

    public HempBoatItem(boolean chest, Settings settings) {
        super(settings);
        this.chest = chest;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        HitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(itemStack);
        }

        Vec3d rotationVec = user.getRotationVec(1.0F);
        List<Entity> list = world.getOtherEntities(user, user.getBoundingBox().stretch(rotationVec.multiply(5.0)).expand(1.0), RIDERS);
        if (!list.isEmpty()) {
            Vec3d eyePos = user.getEyePos();
            for (Entity entity : list) {
                Box box = entity.getBoundingBox().expand((double) entity.getTargetingMargin());
                if (box.contains(eyePos)) {
                    return TypedActionResult.pass(itemStack);
                }
            }
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return TypedActionResult.pass(itemStack);
        }

        BoatEntity boatEntity = this.createEntity(world, hitResult, itemStack, user);
        boatEntity.setYaw(user.getYaw());
        if (!world.isSpaceEmpty(boatEntity, boatEntity.getBoundingBox())) {
            return TypedActionResult.fail(itemStack);
        }

        if (!world.isClient) {
            world.spawnEntity(boatEntity);
            world.emitGameEvent(user, GameEvent.ENTITY_PLACE, hitResult.getPos());
            itemStack.decrementUnlessCreative(1, user);
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(itemStack, world.isClient());
    }

    private BoatEntity createEntity(World world, HitResult hitResult, ItemStack stack, PlayerEntity player) {
        Vec3d pos = hitResult.getPos();
        BoatEntity boatEntity = this.chest
                ? new HempChestBoatEntity(world, pos.x, pos.y, pos.z)
                : new HempBoatEntity(world, pos.x, pos.y, pos.z);
        if (world instanceof ServerWorld serverWorld) {
            EntityType.copier(serverWorld, stack, player).accept(boatEntity);
        }
        return boatEntity;
    }
}
