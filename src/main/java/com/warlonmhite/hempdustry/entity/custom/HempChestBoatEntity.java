package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.level.Level;

public class HempChestBoatEntity extends ChestBoat {
    public HempChestBoatEntity(EntityType<? extends ChestBoat> entityType, Level world) {
        super(entityType, world, () -> ModItems.HEMP_CHEST_BOAT);
    }

    public HempChestBoatEntity(Level world, double x, double y, double z) {
        this(ModEntities.HEMP_CHEST_BOAT, world);
        this.setPos(x, y, z);
        this.setOldPosAndRot();
    }
}
