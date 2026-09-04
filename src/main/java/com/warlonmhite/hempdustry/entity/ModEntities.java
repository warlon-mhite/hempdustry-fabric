package com.warlonmhite.hempdustry.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.entity.custom.HempBoatEntity;
import com.warlonmhite.hempdustry.entity.custom.HempChestBoatEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    // Boats can't reuse vanilla's BoatEntity.Type (a closed enum with a fixed 9 wood types), so hemp gets its
    // own EntityType entirely, with its own entity classes, item, and client renderer.
    //
    // Every value here matches vanilla's boat exactly (EntityType.BOAT, verified in the 1.21.1 jar), which
    // is the point: a hemp boat should behave like a boat.
    public static final EntityType<HempBoatEntity> HEMP_BOAT = register("hemp_boat",
            EntityType.Builder.<HempBoatEntity>create(HempBoatEntity::new, SpawnGroup.MISC)
                    .dimensions(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .maxTrackingRange(10));

    public static final EntityType<HempChestBoatEntity> HEMP_CHEST_BOAT = register("hemp_chest_boat",
            EntityType.Builder.<HempChestBoatEntity>create(HempChestBoatEntity::new, SpawnGroup.MISC)
                    .dimensions(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .maxTrackingRange(10));

    // build() takes the type's own RegistryKey since 1.21.2 — the same "know your id before you are
    // built" move items and blocks made.
    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key =
                RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(Hempdustry.MOD_ID, name));
        return Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void registerModEntities() {
        Hempdustry.LOGGER.info("Registering Mod Entities for " + Hempdustry.MOD_ID);
    }
}
