package com.warlonmhite.hempdustry.entity.custom;

import com.warlonmhite.hempdustry.entity.ModEntities;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.network.protocol.Packet;
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

    /**
     * Spawned on the client through the loader: a 1.16.5 client builds only vanilla's own entities
     * from vanilla's spawn packet, and a modded boat would never appear.
     */
    @Override
    public Packet<?> getAddEntityPacket() {
        return Services.PLATFORM.entitySpawnPacket(this);
    }
}
