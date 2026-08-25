package net.ethrocky.pane.core;

/** The one component painted above everything else with exclusive input — dialogs.
 *  While a modal is open the host centers it, dims everything behind it, routes all
 *  mouse and key input to it, and swallows clicks that land outside it. ESCAPE
 *  closes it (unless the modal consumes the key first). Opening replaces any
 *  previous modal and closes any popup. */
public final class Modals {
    private static Component modal;

    private Modals() {
    }

    public static void open(Component m) {
        Popups.close();
        Focus.clear();
        modal = m;
    }

    public static void close() {
        modal = null;
        Focus.clear();
    }

    public static Component current() {
        return modal;
    }
}
