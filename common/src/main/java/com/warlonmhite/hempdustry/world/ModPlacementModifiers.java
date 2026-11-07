package com.warlonmhite.hempdustry.world;

import com.warlonmhite.hempdustry.Hempdustry;

/**
 * The newer lines' one placement modifier of the mod, {@code hempdustry:in_beldia_biome}, filters each
 * wild Beldía by its own biome. Minecraft 1.16.5 has no placement modifiers: the same test is made by
 * {@link com.warlonmhite.hempdustry.world.gen.BeldiaPatchFeature} for each plant it places.
 */
public class ModPlacementModifiers {

    public static void registerPlacementModifiers() {
        Hempdustry.LOGGER.info("Registering Placement Modifiers for " + Hempdustry.MOD_ID);
    }
}
