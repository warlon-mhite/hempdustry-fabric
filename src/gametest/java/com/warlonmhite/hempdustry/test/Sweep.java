package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * What the registry-wide game tests share.
 *
 * <p>A sweep walks a registry instead of naming things, so new content is checked the day it is
 * registered, before anyone has written a test of its own for it. Two rules keep a sweep honest:
 * <ul>
 *   <li><b>An exemption names an id and says why</b>, and an exemption that no longer excuses a
 *   finding fails too — so the list cannot quietly grow into a blanket.</li>
 *   <li><b>A sweep asserts how much it saw.</b> A check over nothing passes, which is how a scanner
 *   that stops understanding its input reports success.</li>
 * </ul>
 */
final class Sweep {

    private static Map<String, Path> files;

    private Sweep() {
    }

    static boolean ours(Identifier id) {
        return id.getNamespace().equals(Hempdustry.MOD_ID);
    }

    /**
     * Fails on every finding the exemptions do not name, and on every exemption that names no finding.
     *
     * @param findings what broke the rule, by id, with what is wrong with each
     * @param exempt   ids that may break it, each with the reason
     */
    static void report(TestContext context, String rule, Map<String, String> findings, Map<String, String> exempt) {
        Map<String, String> unexcused = new TreeMap<>(findings);
        exempt.keySet().forEach(unexcused::remove);
        Set<String> stale = new HashSet<>(exempt.keySet());
        stale.removeAll(findings.keySet());
        StringBuilder message = new StringBuilder(rule);
        if (!unexcused.isEmpty()) message.append(" — ").append(unexcused.size()).append(" do not:");
        unexcused.forEach((id, what) -> message.append("\n  ").append(id).append(": ").append(what));
        if (!stale.isEmpty()) message.append("\n  these exemptions no longer excuse anything, remove them: ").append(stale);
        context.assertTrue(unexcused.isEmpty() && stale.isEmpty(), message.toString());
    }

    static void atLeast(TestContext context, String what, int seen, int floor) {
        context.assertTrue(seen >= floor, "saw only " + seen + " " + what + " where there are at least " + floor
                + ": the sweep is no longer seeing what it checks");
    }

    /**
     * Every file the mod ships, keyed by its path inside the jar ({@code assets/hempdustry/sounds.json},
     * {@code com/warlonmhite/hempdustry/Hempdustry.class}). In a dev run the classes are a classpath
     * entry of their own, beside the mod's resources, so both are walked.
     */
    static Map<String, Path> modFiles() {
        if (files == null) {
            Set<Path> roots = new HashSet<>(FabricLoader.getInstance().getModContainer(Hempdustry.MOD_ID)
                    .orElseThrow().getRootPaths());
            try {
                roots.add(Path.of(Hempdustry.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
            Map<String, Path> found = new TreeMap<>();
            for (Path root : roots) {
                try (Stream<Path> walk = Files.walk(root)) {
                    walk.filter(Files::isRegularFile)
                            .forEach(file -> found.put(root.relativize(file).toString().replace('\\', '/'), file));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            files = found;
        }
        return files;
    }

    static byte[] bytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static JsonElement json(Path file) {
        return JsonParser.parseString(new String(bytes(file), StandardCharsets.UTF_8));
    }
}
