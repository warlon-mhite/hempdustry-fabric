package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.entity.custom.HempBoatEntity;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Mirrors vanilla {@code BoatItem} (1.16.5's, checked in its bytecode), but spawns {@link HempBoatEntity}
 * instead of relying on the closed {@code Boat.Type} enum, which mods can't extend. There is no chest
 * variant: chest boats came with 1.19.
 */
public class HempBoatItem extends Item {
    private static final Predicate<Entity> RIDERS = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
    public HempBoatItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        HitResult hitResult = getPlayerPOVHitResult(world, user, ClipContext.Fluid.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemStack);
        }

        Vec3 rotationVec = user.getViewVector(1.0F);
        List<Entity> list = world.getEntities(user, user.getBoundingBox().expandTowards(rotationVec.scale(5.0)).inflate(1.0), RIDERS);
        if (!list.isEmpty()) {
            Vec3 eyePos = user.getEyePosition(1.0F);
            for (Entity entity : list) {
                AABB box = entity.getBoundingBox().inflate((double) entity.getPickRadius());
                if (box.contains(eyePos)) {
                    return InteractionResultHolder.pass(itemStack);
                }
            }
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemStack);
        }

        Vec3 pos = hitResult.getLocation();
        Boat boatEntity = new HempBoatEntity(world, pos.x, pos.y, pos.z);
        boatEntity.yRot = user.yRot;
        if (!world.noCollision(boatEntity, boatEntity.getBoundingBox().inflate(-0.1D))) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (!world.isClientSide) {
            world.addFreshEntity(boatEntity);
            if (!user.abilities.instabuild) {
                itemStack.shrink(1);
            }
        }

        user.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
    }
}
