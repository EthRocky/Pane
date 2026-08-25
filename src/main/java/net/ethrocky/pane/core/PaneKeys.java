package net.ethrocky.pane.core;

/** Key codes widgets react to, numerically equal to GLFW's — defined here so core
 *  needs no LWJGL on the test classpath. */
public final class PaneKeys {
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int TAB = 258;
    public static final int BACKSPACE = 259;
    public static final int DELETE = 261;
    public static final int RIGHT = 262;
    public static final int LEFT = 263;
    public static final int DOWN = 264;
    public static final int UP = 265;
    public static final int HOME = 268;
    public static final int END = 269;
    // Letter keys carry their ASCII code in GLFW; named for the clipboard shortcuts.
    public static final int C = 67;
    public static final int V = 86;
    public static final int X = 88;

    private PaneKeys() {
    }
}
