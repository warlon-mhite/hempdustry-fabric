package com.warlonmhite.hempdustry.forge.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Work for a Forge event that fires once, at a set point while the game starts: kept until the
 * event comes, then refused, so a registration that arrives too late fails loudly instead of being
 * silently dropped. The client's own, for the client setup and its registration events.
 */
final class ClientPending<E> {
    private final String what;
    private List<Consumer<E>> waiting = new ArrayList<>();

    ClientPending(String what) {
        this.what = what;
    }

    synchronized void add(Consumer<E> work) {
        if (waiting == null) {
            throw new IllegalStateException("Too late to register " + what + ": Forge has already taken them");
        }
        waiting.add(work);
    }

    synchronized void handOver(E event) {
        List<Consumer<E>> work = waiting;
        waiting = null;
        if (work != null) {
            work.forEach(each -> each.accept(event));
        }
    }
}
