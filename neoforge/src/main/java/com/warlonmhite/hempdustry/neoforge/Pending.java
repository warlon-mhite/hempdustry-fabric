package com.warlonmhite.hempdustry.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Work for a NeoForge event that fires once, at a set point while the game starts: kept until the
 * event comes, then refused, so a registration that arrives too late fails loudly instead of being
 * silently dropped.
 */
public final class Pending<E> {
    private final String what;
    private List<Consumer<E>> waiting = new ArrayList<>();

    public Pending(String what) {
        this.what = what;
    }

    public synchronized void add(Consumer<E> work) {
        if (waiting == null) {
            throw new IllegalStateException("Too late to register " + what + ": NeoForge has already taken them");
        }
        waiting.add(work);
    }

    public synchronized void handOver(E event) {
        List<Consumer<E>> work = waiting;
        waiting = null;
        if (work != null) {
            work.forEach(each -> each.accept(event));
        }
    }
}
