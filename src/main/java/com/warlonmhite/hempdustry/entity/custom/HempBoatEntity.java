package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.world.World;

public class HempBoatEntity extends BoatEntity {
    // Since 1.21.2 the drop item is a constructor argument on AbstractBoatEntity and asItem() is
    // final, so there is nothing left to override here.
    public HempBoatEntity(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world, () -> ModItems.HEMP_BOAT);
    }

    public HempBoatEntity(World world, double x, double y, double z) {
        this(ModEntities.HEMP_BOAT, world);
        this.setPosition(x, y, z);
        this.resetPosition();
    }
}
