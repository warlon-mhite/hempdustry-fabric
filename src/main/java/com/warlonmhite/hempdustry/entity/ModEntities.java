package com.warlonmhite.hempdustry.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.entity.custom.HempBoatEntity;
import com.warlonmhite.hempdustry.entity.custom.HempChestBoatEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    // Boats can't reuse vanilla's BoatEntity.Type (a closed enum with a fixed 9 wood types), so hemp gets its
    // own EntityType entirely, with its own entity classes, item, and client renderer.
    public static final EntityType<HempBoatEntity> HEMP_BOAT = register("hemp_boat",
            FabricEntityTypeBuilder.<HempBoatEntity>create(SpawnGroup.MISC, HempBoatEntity::new)
                    .dimensions(EntityDimensions.changing(1.375F, 0.5625F).withEyeHeight(0.5625F))
                    .trackRangeBlocks(10)
                    .build());

    public static final EntityType<HempChestBoatEntity> HEMP_CHEST_BOAT = register("hemp_chest_boat",
            FabricEntityTypeBuilder.<HempChestBoatEntity>create(SpawnGroup.MISC, HempChestBoatEntity::new)
                    .dimensions(EntityDimensions.changing(1.375F, 0.5625F).withEyeHeight(0.5625F))
                    .trackRangeBlocks(10)
                    .build());

    private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, Identifier.of(Hempdustry.MOD_ID, name), type);
    }

    public static void registerModEntities() {
        Hempdustry.LOGGER.info("Registering Mod Entities for " + Hempdustry.MOD_ID);
    }
}
