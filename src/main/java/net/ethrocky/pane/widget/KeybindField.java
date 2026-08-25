package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Focus;
import net.ethrocky.pane.core.PaneKeys;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

import java.util.function.IntFunction;

/** Keybind capture: click to arm, press a key to bind it. The bound value is a
 *  GLFW keycode in a {@code State<Integer>} (−1 = unbound). While armed, ESCAPE
 *  cancels (keeps the old bind) and BACKSPACE/DELETE unbinds.
 *
 *  Key display names come from a swappable resolver so core stays headless:
 *  the runtime installs a localized one at client init; the built-in fallback
 *  covers letters, digits, F-keys and the navigation cluster. */
public class KeybindField extends Component {

    private static IntFunction<String> keyNames = KeybindField::defaultName;

    /** Runtime hook: replace the key-name resolver (e.g. with localized names). */
    public static void setKeyNames(IntFunction<String> resolver) {
        keyNames = resolver != null ? resolver : KeybindField::defaultName;
    }

    private final State<Integer> key;
    private boolean arming;

    private KeybindField(State<Integer> key) {
        this.key = key;
    }

    public static KeybindField of(State<Integer> key) {
        return new KeybindField(key);
    }

    public boolean arming() {
        return arming;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public Size measure(TextMeasurer tm) {
        int w = style.width != null ? style.width : Math.max(90, tm.width(text()) + 16);
        return new Size(w, tm.height() + 8);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        if (e.kind() == UiEvent.Kind.DOWN && e.inside(bounds)) {
            Focus.request(this);
            arming = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(UiEvent.Key e) {
        if (!arming) return false;
        if (e.key() == PaneKeys.ESCAPE) {
            // cancel: keep the old bind
        } else if (e.key() == PaneKeys.BACKSPACE || e.key() == PaneKeys.DELETE) {
            key.set(-1);
        } else {
            key.set(e.key());
        }
        arming = false;
        Focus.clear();
        return true;
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        // Clicking away moves focus without telling us — disarm when that happens.
        if (arming && !focused()) arming = false;
        r.fill(bounds, theme.surfaceRaised());
        r.border(bounds, arming ? theme.accent() : hovered ? theme.text() : theme.border(), 1);
        String s = text();
        int ty = bounds.y() + (bounds.h() - r.height()) / 2;
        r.text(s, bounds.x() + (bounds.w() - r.width(s)) / 2, ty, arming ? theme.accent() : theme.text());
    }

    private String text() {
        return arming ? "Press a key…" : keyNames.apply(key.get());
    }

    /** Headless fallback names, GLFW codes. */
    static String defaultName(int code) {
        if (code < 0) return "None";
        if (code >= 'A' && code <= 'Z') return String.valueOf((char) code);
        if (code >= '0' && code <= '9') return String.valueOf((char) code);
        if (code >= 290 && code <= 314) return "F" + (code - 289);
        return switch (code) {
            case 32 -> "Space";
            case PaneKeys.ENTER -> "Enter";
            case PaneKeys.TAB -> "Tab";
            case PaneKeys.BACKSPACE -> "Backspace";
            case PaneKeys.DELETE -> "Delete";
            case PaneKeys.LEFT -> "Left";
            case PaneKeys.RIGHT -> "Right";
            case PaneKeys.UP -> "Up";
            case PaneKeys.DOWN -> "Down";
            case PaneKeys.HOME -> "Home";
            case PaneKeys.END -> "End";
            case 340 -> "Left Shift";
            case 341 -> "Left Ctrl";
            case 342 -> "Left Alt";
            case 344 -> "Right Shift";
            case 345 -> "Right Ctrl";
            case 346 -> "Right Alt";
            default -> "Key " + code;
        };
    }
}
