package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.world.World;

public class HempChestBoatEntity extends ChestBoatEntity {
    public HempChestBoatEntity(EntityType<? extends ChestBoatEntity> entityType, World world) {
        super(entityType, world, () -> ModItems.HEMP_CHEST_BOAT);
    }

    public HempChestBoatEntity(World world, double x, double y, double z) {
        this(ModEntities.HEMP_CHEST_BOAT, world);
        this.setPosition(x, y, z);
        this.resetPosition();
    }
}
