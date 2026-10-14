package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.util.SemVer;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The update checker's version order is Fabric Loader's, on every loader.
 *
 * <p>It used to ask Fabric Loader's {@code SemanticVersion} directly; it now asks {@link SemVer},
 * which runs on NeoForge too. Fabric's parser has leniencies plain SemVer does not, and a release
 * the checker read differently would be announced wrongly or not at all, so this holds the two to
 * the same answers: which strings are versions, and how every pair of them sorts.
 */
public final class SemVerGameTest {

    /** Every version the mod has shipped or announced, those it is about to, and SemVer's own examples. */
    private static final List<String> VERSIONS = List.of(
            "2.0.0-beta", "2.0.0-beta+1.21.11", "2.0.1-beta", "2.0.1-beta+1.21.1", "2.0.2", "2.0.2+1.21.1",
            "2.0.3", "2.0.3+1.21.1", "2.0.3+1.21.11", "2.1.0-beta", "2.1.0-beta+1.21.11", "2.1.0-beta+1.21.1",
            "2.1.0-beta.2", "2.1.0-rc.1", "2.1.0", "2.1.0+1.21.11", "2.10.0", "3.0.0",
            "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11",
            "1.0.0-rc.1", "1.0.0", "1.0", "1", "1.0.0.0", "1.0.0-", "1.0.0-0", "1.0.0-01", "1.0.0-x-y",
            "0.0.0");

    /** Strings either parser might be tempted to accept. */
    private static final List<String> NOT_VERSIONS = List.of(
            "", "v2.0.0", "2.0.x", "x", "2..0", ".2.0", "2.0.", "2.0.0-beta..1", "2.0.0-beta_1",
            "2.0.0-é", "-2.0.0", "2.-1.0", " 2.0.0", "2.0.0 ", "abc", "2.0.0-+", "99999999999.0.0");

    public static void semVerOrdersLikeFabric(GameTestHelper context) {
        List<String> findings = new ArrayList<>();
        List<String> all = new ArrayList<>(VERSIONS);
        all.addAll(NOT_VERSIONS);
        for (String version : all) {
            if ((SemVer.parse(version) != null) != (fabric(version) != null)) {
                findings.add("'" + version + "': ours " + (SemVer.parse(version) != null ? "reads" : "refuses")
                        + " it and Fabric's does not");
            }
        }
        for (String a : VERSIONS) {
            for (String b : VERSIONS) {
                SemVer ours = SemVer.parse(a);
                SemanticVersion theirs = fabric(a);
                if (ours == null || theirs == null) {
                    continue;
                }
                int expected = Integer.signum(theirs.compareTo(fabric(b)));
                int actual = Integer.signum(ours.compareTo(SemVer.parse(b)));
                if (expected != actual) {
                    findings.add(a + " vs " + b + ": Fabric " + expected + ", ours " + actual);
                }
            }
        }
        context.assertTrue(findings.isEmpty(), "SemVer and Fabric's SemanticVersion disagree: " + findings);
        context.assertTrue(VERSIONS.stream().allMatch(v -> SemVer.parse(v) != null),
                "a version the mod has shipped does not read as one");
        context.succeed();
    }

    private static SemanticVersion fabric(String version) {
        try {
            return SemanticVersion.parse(version);
        } catch (VersionParsingException e) {
            return null;
        }
    }
}
