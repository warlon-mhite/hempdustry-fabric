package com.warlonmhite.hempdustry.test.neoforge;

import com.warlonmhite.hempdustry.test.HempdustryGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** The test mod's entry point on NeoForge: every shared test, added to the game's test functions. */
@Mod(NeoForgeGameTests.MOD_ID)
public final class NeoForgeGameTests {
    /** The test mod's id; a NeoForge mod id cannot hold the hyphen of the tests' own namespace. */
    public static final String MOD_ID = "hempdustry_gametest";

    public NeoForgeGameTests(IEventBus modBus) {
        // NeoForge's own registration reads the annotations its way; the shared tests are named and
        // placed as on Fabric instead, so they are handed to the game directly when NeoForge asks.
        modBus.addListener(RegisterGameTestsEvent.class, event -> HempdustryGameTests.register());
    }
}
