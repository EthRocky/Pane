package net.ethrocky.pane.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Transient notifications. Anything may call show(); the host paints whatever
 *  is still alive and this holder expires the rest. */
public final class Toasts {
    public static final long TTL_MS = 3000;

    public record Toast(String message, long shownAt) {
    }

    private static final List<Toast> active = new CopyOnWriteArrayList<>();

    private Toasts() {
    }

    public static void show(String message) {
        active.add(new Toast(message, System.currentTimeMillis()));
    }

    /** Live toasts at {@code now}, oldest first; expired ones are dropped. */
    public static List<Toast> active(long now) {
        active.removeIf(t -> now - t.shownAt() > TTL_MS);
        return List.copyOf(active);
    }
}
