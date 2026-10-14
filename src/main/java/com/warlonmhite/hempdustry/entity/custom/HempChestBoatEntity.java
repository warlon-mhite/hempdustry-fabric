package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class HempChestBoatEntity extends ChestBoat {
    public HempChestBoatEntity(EntityType<? extends Boat> entityType, Level world) {
        super(entityType, world);
    }

    public HempChestBoatEntity(Level world, double x, double y, double z) {
        this(ModEntities.HEMP_CHEST_BOAT, world);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public Item getDropItem() {
        return ModItems.HEMP_CHEST_BOAT;
    }
}
