package net.ethrocky.pane.core;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Clipboard access for text widgets. Headless-safe: without an installed provider
 *  copies live in-process, which already covers field-to-field copying. Game runtimes
 *  install the OS clipboard (GLFW) at startup so text also moves in and out of the game. */
public final class PaneClipboard {
    private static String memory = "";
    private static Supplier<String> getter;
    private static Consumer<String> setter;

    private PaneClipboard() {
    }

    /** Point at the OS clipboard. Either side may fail (no window yet, non-text
     *  content); the in-process copy is the standing fallback. */
    public static void install(Supplier<String> get, Consumer<String> set) {
        getter = get;
        setter = set;
    }

    public static String get() {
        if (getter != null) {
            try {
                String s = getter.get();
                if (s != null) return s;
            } catch (Throwable ignored) {
            }
        }
        return memory;
    }

    public static void set(String s) {
        memory = s == null ? "" : s;
        if (setter != null) {
            try {
                setter.accept(memory);
            } catch (Throwable ignored) {
            }
        }
    }
}
