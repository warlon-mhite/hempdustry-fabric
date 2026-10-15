package com.warlonmhite.hempdustry.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.entity.custom.HempBoatEntity;
import com.warlonmhite.hempdustry.entity.custom.HempChestBoatEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    // Boats can't reuse vanilla's Boat.Type (a closed enum with a fixed 9 wood types), so hemp gets its
    // own EntityType entirely, with its own entity classes, item, and client renderer.
    //
    // Built with vanilla's own EntityType.Builder rather than FabricEntityTypeBuilder, which is deprecated.
    // build(null): no datafixer id, which a mod has no business inventing. It is exactly what Fabric's
    // own no-argument build() passes, and both loaders skip the data fixer's lookup for it.
    //
    // Every value here matches vanilla's boat exactly (EntityType.BOAT, verified in the 1.21.1 jar), which
    // is the point: a hemp boat should behave like a boat.
    public static final EntityType<HempBoatEntity> HEMP_BOAT = register("hemp_boat",
            EntityType.Builder.<HempBoatEntity>of(HempBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .clientTrackingRange(10)
                    .build(null));

    public static final EntityType<HempChestBoatEntity> HEMP_CHEST_BOAT = register("hemp_chest_boat",
            EntityType.Builder.<HempChestBoatEntity>of(HempChestBoatEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .clientTrackingRange(10)
                    .build(null));

    private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, name), type);
    }

    public static void registerModEntities() {
        Hempdustry.LOGGER.info("Registering Mod Entities for " + Hempdustry.MOD_ID);
    }
}
