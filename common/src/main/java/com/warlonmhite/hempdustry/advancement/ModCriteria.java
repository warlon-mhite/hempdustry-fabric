package com.warlonmhite.hempdustry.advancement;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Custom advancement criteria (triggers). Registered eagerly at class-load; {@link #registerCriteria()}
 * only forces that init, mirroring the mod's other holder classes.
 */
public class ModCriteria {

    public static final SmokeCriterion SMOKE = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "smoke"),
            new SmokeCriterion());

    public static final HarvestHempCriterion HARVEST_HEMP = Registry.register(
            BuiltInRegistries.TRIGGER_TYPES,
            ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "harvest_hemp"),
            new HarvestHempCriterion());

    public static void registerCriteria() {
        Hempdustry.LOGGER.info("Registering Advancement Criteria for " + Hempdustry.MOD_ID);
    }
}
