package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class HempChestBoatEntity extends ChestBoatEntity {
    public HempChestBoatEntity(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world);
    }

    public HempChestBoatEntity(World world, double x, double y, double z) {
        this(ModEntities.HEMP_CHEST_BOAT, world);
        this.setPosition(x, y, z);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
    }

    @Override
    public Item asItem() {
        return ModItems.HEMP_CHEST_BOAT;
    }
}
