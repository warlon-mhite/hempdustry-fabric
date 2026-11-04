package com.warlonmhite.hempdustry.advancement;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.advancements.CriteriaTriggers;

/**
 * Custom advancement criteria (triggers). Registered eagerly at class-load; {@link #registerCriteria()}
 * only forces that init, mirroring the mod's other holder classes.
 */
public class ModCriteria {

    public static final SmokeCriterion SMOKE = CriteriaTriggers.register(new SmokeCriterion());

    public static final HarvestHempCriterion HARVEST_HEMP = CriteriaTriggers.register(new HarvestHempCriterion());

    public static void registerCriteria() {
        Hempdustry.LOGGER.info("Registering Advancement Criteria for " + Hempdustry.MOD_ID);
    }
}
