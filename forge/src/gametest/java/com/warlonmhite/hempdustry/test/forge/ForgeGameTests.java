package com.warlonmhite.hempdustry.test.forge;

import com.warlonmhite.hempdustry.test.HempdustryGameTests;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.warlonmhite.hempdustry.test.HempdustryGameTests;

/** The test mod's entry point on Forge: every shared test, added to the game's test functions. */
@Mod(ForgeGameTests.MOD_ID)
public final class ForgeGameTests {
    /** The test mod's id; a Forge mod id cannot hold the hyphen of the tests' own namespace. */
    public static final String MOD_ID = "hempdustry_gametest";

    public ForgeGameTests() {
        // Forge's own registration reads the annotations its way; the shared tests are named and
        // placed as on Fabric instead, so they are handed to the game directly when Forge asks.
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterGameTestsEvent event) -> HempdustryGameTests.register());
    }
}
