package com.warlonmhite.hempdustry.test.framework;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs every test on a running server, the way the newer lines' game-test server does: in batches of
 * at most a hundred, each test in a room of its own on a flat world, a batch at a time, all of a
 * batch's tests started on the same tick. Ticked once per server tick, after the worlds.
 *
 * <p>The rooms are laid out as vanilla lays them out: eight to a row, 13 blocks apart east to west and
 * 14 north to south, from {@code (0, 4, 0)} -- the first air above 1.16.5's classic flat, as vanilla's
 * {@code (0, -60, 0)} is on 1.18+. Before a test, its room and a margin around it are cleared as
 * vanilla clears them: air from the room's floor up, the flat world's layers under it, every entity
 * but a player gone, no block tick or block event left pending. A structure block marks each room's
 * corner, where vanilla's stands; the command block and button vanilla adds outside the room to rerun
 * a test by hand are left out, since there is no {@code /test} command on 1.16.5.
 */
public final class GameTestRunner {
    private static final Logger LOGGER = LogManager.getLogger("GameTest");
    private static final BlockPos FIRST_ROOM = new BlockPos(0, 4, 0);
    private static final int TESTS_PER_ROW = 8;
    private static final int ROOM = 8;
    private static final int BATCH_SIZE = 100;
    /** 1.16.5's classic flat, bottom up, as {@code FlatLevelGeneratorSettings#getDefault} builds it. */
    private static final List<BlockState> LAYERS = ImmutableList.of(Blocks.BEDROCK.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState());

    private final ServerLevel level;
    private final List<List<TestFunction>> batches = new ArrayList<>();
    private final List<String> batchNames = new ArrayList<>();
    private final List<GameTestInfo> all = new ArrayList<>();
    private List<GameTestInfo> running = Collections.emptyList();
    private final BlockPos.MutableBlockPos nextRoom = FIRST_ROOM.mutable();
    private int roomsInRow;
    private int batch = -1;
    private boolean finished;

    public GameTestRunner(ServerLevel level, List<TestFunction> functions) {
        this.level = level;
        Map<String, List<TestFunction>> byBatch = new LinkedHashMap<>();
        for (TestFunction function : functions) {
            byBatch.computeIfAbsent(function.getBatchName(), name -> new ArrayList<>()).add(function);
        }
        byBatch.forEach((name, list) -> {
            int part = 0;
            for (List<TestFunction> partition : Iterables.partition(list, BATCH_SIZE)) {
                batchNames.add(name + ":" + ++part);
                batches.add(partition);
            }
        });
    }

    /**
     * The test world as vanilla's test server keeps it: no mob spawning, no weather, clear skies. And
     * first, that it is the world the tests were written for: on another one (a normal world, whose
     * deepslate and caves sit where the rooms go) the tests would run, and fail for the wrong reasons.
     */
    public void start() {
        for (int y = 0; y <= LAYERS.size(); y++) {
            BlockState expected = y < LAYERS.size() ? LAYERS.get(y) : Blocks.AIR.defaultBlockState();
            BlockState found = level.getBlockState(new BlockPos(FIRST_ROOM.getX() - 8, y, FIRST_ROOM.getZ() - 8));
            if (found != expected) {
                throw new IllegalStateException("The test world is not 1.16.5's classic flat: " + found + " at y " + y
                        + " where it has " + expected + "; check the server.properties the gameTestServer run writes");
            }
        }
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.setWeatherParameters(20000000, 20000000, false, false);
        LOGGER.info("{} game tests in {} batch(es)", batches.stream().mapToInt(List::size).sum(), batches.size());
        nextBatch();
    }

    public void tick() {
        if (finished || batch < 0) {
            return;
        }
        for (GameTestInfo info : running) {
            boolean wasDone = info.isDone();
            info.tick();
            if (!wasDone && info.isDone()) {
                report(info);
            }
        }
        if (running.stream().allMatch(GameTestInfo::isDone)) {
            nextBatch();
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public int failedRequiredCount() {
        return (int) all.stream().filter(info -> info.isRequired() && !info.hasSucceeded()).count();
    }

    private void nextBatch() {
        batch++;
        if (batch >= batches.size()) {
            finished = true;
            return;
        }
        List<TestFunction> functions = batches.get(batch);
        LOGGER.info("Running test batch '{}' ({} tests)...", batchNames.get(batch), functions.size());
        // every room of the batch first, then every test's clock, as vanilla does
        running = new ArrayList<>();
        for (TestFunction function : functions) {
            running.add(new GameTestInfo(function, level, layOutRoom()));
        }
        all.addAll(running);
        running.forEach(GameTestInfo::startExecution);
    }

    /** The next room in the grid, cleared and marked; its corner is the test's {@code (0, 0, 0)}. */
    private BlockPos layOutRoom() {
        BlockPos origin = nextRoom.immutable();
        nextRoom.move(ROOM + 5, 0, 0);
        if (++roomsInRow == TESTS_PER_ROW) {
            roomsInRow = 0;
            nextRoom.setX(FIRST_ROOM.getX());
            nextRoom.move(0, 0, ROOM + 6);
        }
        ChunkPos chunk = new ChunkPos(origin);
        for (int dx = -1; dx < 4; dx++) {
            for (int dz = -1; dz < 4; dz++) {
                level.setChunkForced(chunk.x + dx, chunk.z + dz, true);
            }
        }
        BoundingBox room = new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + ROOM - 1, origin.getY() + ROOM - 1, origin.getZ() + ROOM - 1);
        BoundingBox cleared = new BoundingBox(room.x0 - 2, room.y0 - 3, room.z0 - 3, room.x1 + 3, room.y1 + 20, room.z1 + 3);
        BlockPos.betweenClosedStream(cleared.x0, cleared.y0, cleared.z0, cleared.x1, cleared.y1, cleared.z1)
                .forEach(pos -> clearBlock(origin.getY(), pos));
        level.getBlockTicks().fetchTicksInArea(cleared, true, false);
        level.getLiquidTicks().fetchTicksInArea(cleared, true, false);
        level.clearBlockEvents(cleared);
        AABB box = new AABB(cleared.x0, cleared.y0, cleared.z0, cleared.x1, cleared.y1, cleared.z1);
        level.getEntitiesOfClass(Entity.class, box, entity -> !(entity instanceof Player)).forEach(Entity::remove);
        level.setBlockAndUpdate(origin, Blocks.STRUCTURE_BLOCK.defaultBlockState());
        return origin;
    }

    /**
     * Vanilla's {@code StructureUtils#clearBlock}: below the room's floor the flat world's layer for that
     * height -- counted from one above the world's bottom, as vanilla counts it, so the block under the
     * room is dirt -- and air everywhere else, a container emptied first so it drops nothing.
     */
    private void clearBlock(int floorY, BlockPos pos) {
        BlockState state = Blocks.AIR.defaultBlockState();
        int layer = pos.getY();
        if (pos.getY() < floorY && layer > 0 && layer <= LAYERS.size()) {
            state = LAYERS.get(layer - 1);
        }
        new BlockInput(state, Collections.emptySet(), null).place(level, pos, 2);
        level.updateNeighborsAt(pos, state.getBlock());
    }

    private void report(GameTestInfo info) {
        if (info.hasSucceeded()) {
            LOGGER.info("{} passed after {} ticks ({} ms)", info.getTestName(), info.getTicksTaken(), info.getMillis());
        } else {
            LOGGER.info("{} {} after {} ticks: {}", info.getTestName(), info.isRequired() ? "failed" : "failed (optional)",
                    info.getTicksTaken(), describe(info.getError()));
        }
    }

    /** The summary, in the log and in {@code file}: one line per test, failures with their stack trace. */
    public void finish(Path file) {
        long failedRequired = failedRequiredCount();
        long failedOptional = all.stream().filter(info -> !info.isRequired() && !info.hasSucceeded()).count();
        long required = all.stream().filter(GameTestInfo::isRequired).count();
        StringBuilder results = new StringBuilder();
        for (GameTestInfo info : all) {
            results.append(info.hasSucceeded() ? "PASS " : info.isRequired() ? "FAIL " : "FAIL (optional) ")
                    .append(info.getTestName()).append(" (").append(info.getTicksTaken()).append(" ticks)");
            if (!info.hasSucceeded()) {
                results.append('\n').append(stackTrace(info.getError()).trim().replaceAll("(?m)^", "    "));
            }
            results.append('\n');
        }
        String summary = all.size() + " tests, " + required + " required: " + (required - failedRequired)
                + " passed, " + failedRequired + " failed" + (failedOptional > 0 ? "; " + failedOptional + " optional failed" : "");
        results.append(summary).append('\n');
        try {
            Files.write(file, results.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        LOGGER.info("========= {} GAME TESTS COMPLETE ======================", all.size());
        if (failedRequired > 0) {
            LOGGER.info("{} required tests failed :(", failedRequired);
            all.stream().filter(info -> info.isRequired() && !info.hasSucceeded())
                    .forEach(info -> LOGGER.info("   - {}: {}", info.getTestName(), describe(info.getError())));
        } else {
            LOGGER.info("All {} required tests passed :)", required);
        }
        LOGGER.info(summary);
        LOGGER.info("Results: {}", file.toAbsolutePath());
        LOGGER.info("====================================================");
    }

    private static String describe(Throwable error) {
        if (error == null) {
            return "(no error)";
        }
        return error.getClass().getName().startsWith("net.minecraft.gametest") ? error.getMessage() : error.toString();
    }

    private static String stackTrace(Throwable error) {
        if (error == null) {
            return "(no error)";
        }
        StringWriter out = new StringWriter();
        error.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
