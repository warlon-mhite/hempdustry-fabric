package com.warlonmhite.hempdustry.test.framework;

import java.util.function.Consumer;

/** One test as the runner knows it: its name, its batch, its limits and what to run. */
public final class TestFunction {
    private final String batchName;
    private final String testName;
    private final int maxTicks;
    private final long setupTicks;
    private final boolean required;
    private final Consumer<GameTestHelper> function;

    public TestFunction(String batchName, String testName, int maxTicks, long setupTicks, boolean required,
                        Consumer<GameTestHelper> function) {
        this.batchName = batchName;
        this.testName = testName;
        this.maxTicks = maxTicks;
        this.setupTicks = setupTicks;
        this.required = required;
        this.function = function;
    }

    public void run(GameTestHelper helper) {
        function.accept(helper);
    }

    public String getBatchName() {
        return batchName;
    }

    public String getTestName() {
        return testName;
    }

    public int getMaxTicks() {
        return maxTicks;
    }

    public long getSetupTicks() {
        return setupTicks;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return testName;
    }
}
