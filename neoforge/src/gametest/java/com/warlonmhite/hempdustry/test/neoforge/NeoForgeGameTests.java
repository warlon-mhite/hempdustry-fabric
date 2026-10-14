package com.warlonmhite.hempdustry.test.neoforge;

import com.warlonmhite.hempdustry.test.HempdustryGameTests;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The test mod's entry point on NeoForge: every shared test, registered with the game's test functions. */
@Mod(NeoForgeGameTests.MOD_ID)
public final class NeoForgeGameTests {
    /** The test mod's id; a NeoForge mod id cannot hold the hyphen of the tests' own namespace. */
    public static final String MOD_ID = "hempdustry_gametest";

    public NeoForgeGameTests(IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(Registries.TEST_FUNCTION)) {
                HempdustryGameTests.registerAll();
            }
        });
    }
}
