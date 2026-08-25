package net.ethrocky.pane.core;

/** The one floating component painted above every window (dropdown lists, and later
 *  tooltips and context menus). Widgets open it; the host lays it out at (x, y),
 *  routes input to it first, and closes it on click-away. Opening replaces any
 *  previous popup. */
public final class Popups {
    private static Component popup;
    private static int x, y;

    private Popups() {
    }

    public static void open(Component p, int px, int py) {
        popup = p;
        x = px;
        y = py;
    }

    public static void close() {
        popup = null;
    }

    public static Component current() {
        return popup;
    }

    public static int x() {
        return x;
    }

    public static int y() {
        return y;
    }
}
