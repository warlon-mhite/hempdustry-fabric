package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A config file the mod cannot read is left alone, and the settings already running stay.
 *
 * <p>It used to be written over with every default, so one typo in a server's
 * {@code config/hempdustry.json} -- by hand, then {@code /hempdustry reload} -- quietly replaced
 * everything else the owner had set in it. Each broken file below is one way that happened.
 *
 * <p>Everything happens inside one tick, and the real file is put back in a {@code finally}: the
 * config is global, and a test that left it changed would change every other test's numbers.
 */
public final class ConfigGameTest implements FabricGameTest {

    private static final String[] BROKEN = {
            "{ \"effects\": { \"maxBuffLevel\": 3, }",           // not JSON: cut off mid-edit
            "{ \"effects\": { \"maxLevel\": \"four\" } }",       // JSON, but a word where a number goes
            "{ \"effects\": 3 }",                                // a whole section replaced by a number
            "[ \"effects\" ]"                                    // JSON, but not an object
    };

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void aBrokenConfigIsLeftAlone(TestContext context) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(Hempdustry.MOD_ID + ".json");
        byte[] original = read(path);
        try {
            HempdustryConfig running = HempdustryConfig.get();
            for (String broken : BROKEN) {
                write(path, broken);
                context.assertFalse(HempdustryConfig.load(), "load() reported success on: " + broken);
                context.assertTrue(broken.equals(new String(read(path), StandardCharsets.UTF_8)),
                        "the file was written over after failing to read: " + broken);
                context.assertTrue(HempdustryConfig.get() == running,
                        "the running config changed after failing to read: " + broken);
            }

            // And a good file still goes in: a field it leaves out keeps its default, and the file
            // comes back out complete.
            write(path, "{ \"effects\": { \"maxBuffLevel\": 3 } }");
            context.assertTrue(HempdustryConfig.load(), "a valid file was refused");
            context.assertTrue(HempdustryConfig.get().effects().maxBuffLevel() == 3,
                    "maxBuffLevel 3 was not applied: " + HempdustryConfig.get().effects().maxBuffLevel());
            String rewritten = new String(read(path), StandardCharsets.UTF_8);
            context.assertTrue(rewritten.contains("\"maxLevel\"") && rewritten.contains("\"cropGrowthMultiplier\""),
                    "a valid file was not written back complete");
        } finally {
            write(path, original);
            HempdustryConfig.load();
        }
        context.complete();
    }

    private static byte[] read(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path path, String text) {
        write(path, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void write(Path path, byte[] bytes) {
        try {
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
