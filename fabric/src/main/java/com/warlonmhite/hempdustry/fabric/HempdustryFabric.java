package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.api.ModInitializer;

/** Fabric's entry point: the mod, as the rest of the code registers it. */
public class HempdustryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Hempdustry.init();
    }
}
