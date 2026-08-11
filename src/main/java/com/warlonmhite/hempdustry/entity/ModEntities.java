package com.warlonmhite.hempdustry.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.entity.custom.HempBoatEntity;
import com.warlonmhite.hempdustry.entity.custom.HempChestBoatEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    // Boats can't reuse vanilla's BoatEntity.Type (a closed enum with a fixed 9 wood types), so hemp gets its
    // own EntityType entirely, with its own entity classes, item, and client renderer.
    //
    // Built with vanilla's own EntityType.Builder rather than FabricEntityTypeBuilder, which is deprecated.
    // The no-argument build() is Fabric's — it is interface-injected onto the vanilla builder, and is the
    // vanilla build(String) without the datafixer id a mod has no business inventing.
    //
    // Every value here matches vanilla's boat exactly (EntityType.BOAT, verified in the 1.21.1 jar), which
    // is the point: a hemp boat should behave like a boat.
    public static final EntityType<HempBoatEntity> HEMP_BOAT = register("hemp_boat",
            EntityType.Builder.<HempBoatEntity>create(HempBoatEntity::new, SpawnGroup.MISC)
                    .dimensions(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .maxTrackingRange(10)
                    .build());

    public static final EntityType<HempChestBoatEntity> HEMP_CHEST_BOAT = register("hemp_chest_boat",
            EntityType.Builder.<HempChestBoatEntity>create(HempChestBoatEntity::new, SpawnGroup.MISC)
                    .dimensions(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .maxTrackingRange(10)
                    .build());

    private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, Identifier.of(Hempdustry.MOD_ID, name), type);
    }

    public static void registerModEntities() {
        Hempdustry.LOGGER.info("Registering Mod Entities for " + Hempdustry.MOD_ID);
    }
}
