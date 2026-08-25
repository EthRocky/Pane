package net.ethrocky.pane.core;

import java.util.ArrayList;
import java.util.List;

/** Who owns the keyboard. One client, one focus — a static holder keeps widgets
 *  able to request focus without reaching into any host. Hosts (overlay, screen)
 *  read it to decide whether keys go to the UI or the game. */
public final class Focus {
    private static Component current;

    private Focus() {
    }

    public static void request(Component c) {
        current = c;
    }

    public static void clear() {
        current = null;
    }

    public static Component current() {
        return current;
    }

    /** Moves focus to the next (or previous) focusable widget under {@code root},
     *  in tree order, wrapping at the ends. Hosts call this on TAB / shift-TAB when
     *  the focused widget didn't consume the key. No-op if nothing is focusable. */
    public static void traverse(Component root, boolean backward) {
        List<Component> order = new ArrayList<>();
        collect(root, order);
        if (order.isEmpty()) return;
        int i = order.indexOf(current);
        int n = order.size();
        int next = i < 0
                ? (backward ? n - 1 : 0)
                : (i + (backward ? -1 : 1) + n) % n;
        current = order.get(next);
    }

    private static void collect(Component c, List<Component> out) {
        if (c.focusable()) out.add(c);
        for (int i = 0; i < c.childCount(); i++) collect(c.childAt(i), out);
    }
}
