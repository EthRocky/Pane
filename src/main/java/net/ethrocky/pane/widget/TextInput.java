package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Focus;
import net.ethrocky.pane.core.PaneClipboard;
import net.ethrocky.pane.core.PaneKeys;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

/** Single-line text field bound to a State. Click to focus; while focused it
 *  consumes every key so gameplay binds can't fire mid-typing. */
public class TextInput extends Component {
    private final State<String> text;
    private int caret;
    private Runnable onSubmit = () -> {};
    private int scrollX;

    protected TextInput(State<String> text) {
        this.text = text;
        this.caret = text.get().length();
        // External writers may shrink the value under us.
        text.onChange(v -> caret = Math.min(caret, v.length()));
    }

    /** Grab keyboard focus with the caret at the end (subclasses decide when). */
    protected void takeFocus() {
        Focus.request(this);
        caret = text.get().length();
    }

    public static TextInput of(State<String> text) {
        return new TextInput(text);
    }

    public TextInput onSubmit(Runnable action) {
        this.onSubmit = action;
        return this;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public Size measure(TextMeasurer tm) {
        return new Size(style.width != null ? style.width : 140, tm.height() + 8);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        if (e.kind() == UiEvent.Kind.DOWN && e.inside(bounds)) {
            takeFocus();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKey(UiEvent.Key e) {
        if (!focused()) return false;
        String s = text.get();
        switch (e.key()) {
            case PaneKeys.BACKSPACE -> {
                if (caret > 0) {
                    caret--;
                    text.set(s.substring(0, caret) + s.substring(caret + 1));
                }
            }
            case PaneKeys.DELETE -> {
                if (caret < s.length()) {
                    text.set(s.substring(0, caret) + s.substring(caret + 1));
                }
            }
            case PaneKeys.LEFT -> caret = Math.max(0, caret - 1);
            case PaneKeys.RIGHT -> caret = Math.min(s.length(), caret + 1);
            case PaneKeys.HOME -> caret = 0;
            case PaneKeys.END -> caret = s.length();
            case PaneKeys.ENTER -> {
                onSubmit.run();
                Focus.clear();
            }
            case PaneKeys.ESCAPE -> Focus.clear();
            // No selection model, so copy/cut act on the whole field — which is what a
            // name or number field wants anyway. Without CTRL these fall through as before.
            case PaneKeys.C -> {
                if (ctrl(e) && !s.isEmpty()) PaneClipboard.set(s);
            }
            case PaneKeys.X -> {
                if (ctrl(e) && !s.isEmpty()) {
                    PaneClipboard.set(s);
                    text.set("");
                    caret = 0;
                }
            }
            case PaneKeys.V -> {
                if (ctrl(e)) {
                    // Single-line field: flatten whatever whitespace rode in with the paste.
                    String clip = PaneClipboard.get().replaceAll("\\s+", " ").trim();
                    if (!clip.isEmpty()) {
                        text.set(s.substring(0, caret) + clip + s.substring(caret));
                        caret += clip.length();
                    }
                }
            }
            default -> { }
        }
        return true;
    }

    private static boolean ctrl(UiEvent.Key e) {
        return (e.modifiers() & UiEvent.CTRL) != 0;
    }

    @Override
    public boolean onChar(UiEvent.Char e) {
        if (!focused() || e.codepoint() < 32) return false;
        String s = text.get();
        String inserted = new String(Character.toChars(e.codepoint()));
        text.set(s.substring(0, caret) + inserted + s.substring(caret));
        caret += inserted.length();
        return true;
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        r.fill(bounds, theme.surface());
        int borderColor = focused() ? theme.accent() : (hovered ? theme.textDim() : theme.border());
        r.border(bounds, borderColor, 1);

        String s = text.get();
        int caretX = r.width(s.substring(0, Math.min(caret, s.length())));
        int innerW = bounds.w() - 8;
        // Keep the caret inside the box: follow it when it runs off either edge.
        if (caretX - scrollX > innerW) scrollX = caretX - innerW;
        if (caretX - scrollX < 0) scrollX = caretX;

        int tx = bounds.x() + 4 - scrollX;
        int ty = bounds.y() + (bounds.h() - r.height()) / 2;
        r.pushClip(bounds.inset(1));
        r.text(s, tx, ty, theme.text());
        if (focused() && (System.currentTimeMillis() / 500) % 2 == 0) {
            r.fill(new Rect(tx + caretX, ty - 1, 1, r.height() + 2), theme.accent());
        }
        r.popClip();
    }
}
