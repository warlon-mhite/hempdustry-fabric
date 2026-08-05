package com.warlonmhite.hempdustry.advancement;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Custom advancement criteria (triggers). Registered eagerly at class-load; {@link #registerCriteria()}
 * only forces that init, mirroring the mod's other holder classes.
 */
public class ModCriteria {

    public static final SmokeCriterion SMOKE = Registry.register(
            Registries.CRITERION,
            Identifier.of(Hempdustry.MOD_ID, "smoke"),
            new SmokeCriterion());

    public static final HarvestHempCriterion HARVEST_HEMP = Registry.register(
            Registries.CRITERION,
            Identifier.of(Hempdustry.MOD_ID, "harvest_hemp"),
            new HarvestHempCriterion());

    public static void registerCriteria() {
        Hempdustry.LOGGER.info("Registering Advancement Criteria for " + Hempdustry.MOD_ID);
    }
}
