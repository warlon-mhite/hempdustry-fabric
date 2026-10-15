package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.util.SemVer;
import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * What the update checker asks the loader, held to what each test mod knows on its own: the loader's
 * name, which picks the builds Modrinth is asked for, and the mod's version, which must come back as
 * the jar was built -- a SemVer with the game's version as its build metadata.
 *
 * <p>The checker says nothing at all about a running version it cannot read, so a loader that handed
 * the version back in another shape would turn the check off on that loader alone, with no error; and
 * the Modrinth query named Fabric on both loaders until this was asked of the platform.
 */
public final class PlatformGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY)
    public void theLoaderNamesItselfAndTheModsVersion(GameTestHelper context) {
        String loader = Services.PLATFORM.loaderName();
        context.assertTrue(loader.equals(TestPlatform.INSTANCE.loaderName()),
                "the mod says it runs on '" + loader + "', the test mod on '" + TestPlatform.INSTANCE.loaderName() + "'");
        String version = Services.PLATFORM.modVersion();
        context.assertTrue(SemVer.parse(version) != null, "the loader's version of the mod, '" + version
                + "', is no version the update checker can read");
        String game = SharedConstants.getCurrentVersion().getName();
        context.assertTrue(version.endsWith("+" + game), "the loader's version of the mod, '" + version
                + "', has lost the build metadata the jar was built with (+" + game + ")");
        context.succeed();
    }
}
