package com.warlonmhite.hempdustry.test.forge;

import com.warlonmhite.hempdustry.test.HempdustryGameTests;
import com.warlonmhite.hempdustry.test.framework.GameTestRunner;
import com.warlonmhite.hempdustry.test.framework.TestFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The test mod's entry point on Forge, and its test server.
 *
 * <p>Minecraft 1.16.5 ships no game-test server, so the dev server the {@code gameTestServer} run starts
 * becomes one here: once it has started, every test runs in the overworld ({@link GameTestRunner}), one
 * step per server tick; when the last has ended the server stops, and the process exits with the
 * number of required tests that failed, as the newer lines' test server does. The run passes
 * {@code -Dhempdustry.gametest=true}; without it the mod does nothing, so a server that happens to
 * carry it is left alone.
 */
@Mod(ForgeGameTests.MOD_ID)
public final class ForgeGameTests {
    /** The test mod's id; a Forge mod id cannot hold the hyphen of the tests' own namespace. */
    public static final String MOD_ID = "hempdustry_gametest";

    private static final Logger LOGGER = LogManager.getLogger("GameTest");

    private GameTestRunner runner;
    private boolean started;
    private int exitCode = 1;

    public ForgeGameTests() {
        if (!Boolean.getBoolean("hempdustry.gametest")) {
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopped);
    }

    private void serverStarted(FMLServerStartedEvent event) {
        List<TestFunction> tests = HempdustryGameTests.all();
        String only = System.getProperty("hempdustry.gametest.only");
        if (only != null && !only.isEmpty()) {
            Pattern pattern = Pattern.compile(only);
            tests = tests.stream().filter(test -> pattern.matcher(test.getTestName()).find()).collect(Collectors.toList());
            LOGGER.info("Only the {} tests matching {}", tests.size(), only);
        }
        runner = new GameTestRunner(event.getServer().overworld(), tests);
    }

    /** After the worlds have ticked, as vanilla ticks its tests; the first tick lays out the first batch. */
    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || runner == null || runner.isFinished()) {
            return;
        }
        if (!started) {
            started = true;
            runner.start();
        } else {
            runner.tick();
        }
        if (runner.isFinished()) {
            runner.finish(Paths.get("gametest-results.txt"));
            exitCode = runner.failedRequiredCount();
            MinecraftServer server = net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
            server.halt(false);
        }
    }

    /**
     * The exit code, once the server thread has ended. Exiting from the server thread itself would
     * hang: the dedicated server's shutdown hook waits for that thread to end.
     */
    private void serverStopped(FMLServerStoppedEvent event) {
        Thread serverThread = Thread.currentThread();
        int code = runner != null && runner.isFinished() ? exitCode : 1;
        if (code != exitCode || runner == null) {
            LOGGER.error("The test server stopped before every test had run");
        }
        Thread exit = new Thread(() -> {
            try {
                serverThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.exit(code);
        }, "Game test exit");
        exit.start();
    }
}
