package com.warlonmhite.hempdustry.test.framework;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One run of one test, ticked by {@link GameTestRunner}: the newer lines' {@code GameTestInfo}, with the
 * same clock. The test function runs on the test's tick 0, one server tick after its room is laid out
 * (plus its setup ticks); tasks run on the tick they were asked for; a test still undecided after its
 * timeout fails.
 *
 * <p>One departure: anything a test throws fails that test, an {@link Error} included. Vanilla's catches
 * only exceptions, and an error thrown there takes the whole server down with it.
 */
public final class GameTestInfo {
    private final TestFunction function;
    private final ServerLevel level;
    private final BlockPos origin;
    private final Map<Runnable, Long> runAtTickTime = new LinkedHashMap<>();
    private final List<Runnable> finalChecks = new ArrayList<>();
    private long startTick;
    private long tickCount;
    private boolean started;
    private boolean done;
    private Throwable error;
    private long startedAt;
    private long millis;

    GameTestInfo(TestFunction function, ServerLevel level, BlockPos origin) {
        this.function = function;
        this.level = level;
        this.origin = origin;
    }

    void startExecution() {
        startTick = level.getGameTime() + 1L + function.getSetupTicks();
        startedAt = System.nanoTime();
    }

    void tick() {
        if (done) {
            return;
        }
        tickCount = level.getGameTime() - startTick;
        if (tickCount < 0L) {
            return;
        }
        if (tickCount == 0L) {
            startTest();
        }
        // Due tasks are taken out before they run, so a task may schedule another.
        List<Runnable> due = new ArrayList<>();
        for (Iterator<Map.Entry<Runnable, Long>> it = runAtTickTime.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Runnable, Long> entry = it.next();
            if (entry.getValue() <= tickCount) {
                due.add(entry.getKey());
                it.remove();
            }
        }
        for (Runnable task : due) {
            if (done) {
                return;
            }
            try {
                task.run();
            } catch (Throwable t) {
                fail(t);
            }
        }
        if (done) {
            return;
        }
        if (tickCount > function.getMaxTicks()) {
            if (finalChecks.isEmpty()) {
                fail(new GameTestAssertException("Didn't succeed or fail within " + function.getMaxTicks() + " ticks"));
            } else {
                checkFinal(true);
                if (!done) {
                    fail(new GameTestAssertException("No sequences finished"));
                }
            }
        } else {
            checkFinal(false);
        }
    }

    private void startTest() {
        if (started) {
            throw new IllegalStateException("Test already started");
        }
        started = true;
        try {
            function.run(new GameTestHelper(this));
        } catch (Throwable t) {
            fail(t);
        }
    }

    /** {@code succeedWhen}'s criteria: met, the test passes; at the timeout, the last assertion is the failure. */
    private void checkFinal(boolean last) {
        for (Runnable check : new ArrayList<>(finalChecks)) {
            if (done) {
                return;
            }
            try {
                check.run();
                succeed();
            } catch (GameTestAssertException waiting) {
                if (last) {
                    fail(waiting);
                }
            } catch (Throwable t) {
                fail(t);
            }
        }
    }

    void setRunAtTickTime(long tick, Runnable task) {
        runAtTickTime.put(task, tick);
    }

    void addFinalCheck(Runnable criterion) {
        finalChecks.add(criterion);
    }

    long getTick() {
        return tickCount;
    }

    public String getTestName() {
        return function.getTestName();
    }

    public TestFunction getFunction() {
        return function;
    }

    /** The test's {@code (0, 0, 0)}: the north-west corner of its room, on the layer above the ground. */
    public BlockPos getOrigin() {
        return origin;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public boolean isDone() {
        return done;
    }

    public boolean hasSucceeded() {
        return done && error == null;
    }

    public boolean hasFailed() {
        return error != null;
    }

    public boolean isRequired() {
        return function.isRequired();
    }

    public Throwable getError() {
        return error;
    }

    /** Server ticks from the test's tick 0 to its end. */
    public long getTicksTaken() {
        return tickCount;
    }

    public long getMillis() {
        return millis;
    }

    void succeed() {
        if (error == null) {
            finish();
        }
    }

    /** As vanilla's: a failure after {@code succeed()} in the same tick still fails the test. */
    void fail(Throwable t) {
        if (error != null) {
            return;
        }
        error = t;
        finish();
    }

    private void finish() {
        if (!done) {
            done = true;
            millis = (System.nanoTime() - startedAt) / 1_000_000L;
        }
    }

    @Override
    public String toString() {
        return getTestName();
    }
}
