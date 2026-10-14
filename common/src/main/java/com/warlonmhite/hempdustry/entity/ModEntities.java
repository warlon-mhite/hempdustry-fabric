package com.warlonmhite.hempdustry.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.entity.custom.HempBoatEntity;
import com.warlonmhite.hempdustry.entity.custom.HempChestBoatEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    // Boats can't reuse vanilla's Boat.Type (a closed enum with a fixed 9 wood types), so hemp gets its
    // own EntityType entirely, with its own entity classes, item, and client renderer.
    //
    // Every value here matches vanilla's boat exactly (EntityType.BOAT, verified in the 1.21.1 jar), which
    // is the point: a hemp boat should behave like a boat.
    public static final EntityType<HempBoatEntity> HEMP_BOAT = register("hemp_boat",
            EntityType.Builder.<HempBoatEntity>of(HempBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .clientTrackingRange(10));

    public static final EntityType<HempChestBoatEntity> HEMP_CHEST_BOAT = register("hemp_chest_boat",
            EntityType.Builder.<HempChestBoatEntity>of(HempChestBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .clientTrackingRange(10));

    // build() takes the type's own ResourceKey since 1.21.2 — the same "know your id before you are
    // built" move items and blocks made.
    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key =
                ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void registerModEntities() {
        Hempdustry.LOGGER.info("Registering Mod Entities for " + Hempdustry.MOD_ID);
    }
}
