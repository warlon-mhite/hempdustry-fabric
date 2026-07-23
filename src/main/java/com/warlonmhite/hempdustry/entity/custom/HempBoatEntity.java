package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class HempBoatEntity extends BoatEntity {
    public HempBoatEntity(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world);
    }

    public HempBoatEntity(World world, double x, double y, double z) {
        this(ModEntities.HEMP_BOAT, world);
        this.setPosition(x, y, z);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
    }

    @Override
    public Item asItem() {
        return ModItems.HEMP_BOAT;
    }
}
