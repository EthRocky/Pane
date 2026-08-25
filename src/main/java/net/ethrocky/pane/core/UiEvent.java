package net.ethrocky.pane.core;

/** Input events as the component tree sees them: already in GUI-scaled coordinates,
 *  already stripped of anything the host decided the game should keep. */
public final class UiEvent {
    private UiEvent() {
    }

    public enum Kind { MOVE, DOWN, UP, SCROLL }

    /** Modifier bits, numerically equal to GLFW's. */
    public static final int SHIFT = 1, CTRL = 2, ALT = 4;

    /** scrollY is 0 unless kind == SCROLL; button is -1 unless DOWN/UP. */
    public record Mouse(double x, double y, int button, Kind kind, double scrollY, int modifiers) {
        public static Mouse move(double x, double y)              { return new Mouse(x, y, -1, Kind.MOVE, 0, 0); }
        public static Mouse down(double x, double y, int button)   { return new Mouse(x, y, button, Kind.DOWN, 0, 0); }
        public static Mouse up(double x, double y, int button)     { return new Mouse(x, y, button, Kind.UP, 0, 0); }
        public static Mouse scroll(double x, double y, double sy)  { return new Mouse(x, y, -1, Kind.SCROLL, sy, 0); }

        public static Mouse down(double x, double y, int button, int modifiers) {
            return new Mouse(x, y, button, Kind.DOWN, 0, modifiers);
        }

        public static Mouse up(double x, double y, int button, int modifiers) {
            return new Mouse(x, y, button, Kind.UP, 0, modifiers);
        }

        public boolean inside(Rect r) { return r.contains(x, y); }

        public boolean ctrl()  { return (modifiers & CTRL) != 0; }
        public boolean shift() { return (modifiers & SHIFT) != 0; }
        public boolean alt()   { return (modifiers & ALT) != 0; }
    }

    /** A key press or repeat — releases are never routed. Codes match PaneKeys. */
    public record Key(int key, int modifiers) {
    }

    /** A typed character (already layout- and modifier-resolved by the OS). */
    public record Char(int codepoint) {
    }
}
