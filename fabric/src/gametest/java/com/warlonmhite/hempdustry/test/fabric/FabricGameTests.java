package com.warlonmhite.hempdustry.test.fabric;

import com.warlonmhite.hempdustry.test.HempdustryGameTests;
import com.warlonmhite.hempdustry.test.SemVerGameTest;
import net.fabricmc.api.ModInitializer;

/** The test mod's entry point on Fabric: every shared test, and the one that only Fabric can answer. */
public class FabricGameTests implements ModInitializer {
    @Override
    public void onInitialize() {
        HempdustryGameTests.registerAll();
        HempdustryGameTests.register("the_update_checker_orders_versions_like_fabric", SemVerGameTest::semVerOrdersLikeFabric);
    }
}
