package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;

public class HempBoatEntity extends Boat {
    // Since 1.21.2 the drop item is a constructor argument on AbstractBoat and getDropItem() is
    // final, so there is nothing left to override here.
    public HempBoatEntity(EntityType<? extends Boat> entityType, Level world) {
        super(entityType, world, () -> ModItems.HEMP_BOAT);
    }

    public HempBoatEntity(Level world, double x, double y, double z) {
        this(ModEntities.HEMP_BOAT, world);
        this.setPos(x, y, z);
        this.setOldPosAndRot();
    }
}
