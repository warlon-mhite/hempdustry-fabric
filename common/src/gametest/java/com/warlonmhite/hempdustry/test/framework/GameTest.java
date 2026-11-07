package com.warlonmhite.hempdustry.test.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A game test: a method taking a {@link GameTestHelper}, run on the test server in a room of its own.
 *
 * <p>Minecraft 1.16.5 ships the game-test classes without the annotation, the helper's methods or a
 * server that runs them, so this harness is the newer lines' annotation, written out with the same
 * names and defaults. Only what the tests use is here.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
    /** How many ticks the test may take before it fails, counted from its first tick. */
    int timeoutTicks() default 100;

    /** The tests of one batch start together; the next batch starts when the last of them ends. */
    String batch() default "defaultBatch";

    /** The structure the test stands in. Every test here stands in the same 8×8×8 of air. */
    String template() default "";

    /** Ticks between laying the room out and running the test. */
    long setupTicks() default 0L;

    /** Whether a failure fails the run. */
    boolean required() default true;
}
