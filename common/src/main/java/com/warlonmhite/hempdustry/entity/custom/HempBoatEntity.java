package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class HempBoatEntity extends Boat {
    public HempBoatEntity(EntityType<? extends Boat> entityType, Level world) {
        super(entityType, world);
    }

    public HempBoatEntity(Level world, double x, double y, double z) {
        this(ModEntities.HEMP_BOAT, world);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public Item getDropItem() {
        return ModItems.HEMP_BOAT;
    }
}
