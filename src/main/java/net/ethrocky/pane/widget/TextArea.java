package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Focus;
import net.ethrocky.pane.core.PaneKeys;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

/** Multi-line text field bound to a State. Enter inserts a newline (there is no submit —
 *  the bound State updates as you type); up/down move between lines; escape releases focus.
 *  Click positions the caret at the nearest character. */
public class TextArea extends Component {
    private final State<String> text;
    private int caret;
    private int visibleRows = 4;
    private int scrollRow;

    private TextArea(State<String> text) {
        this.text = text;
        this.caret = text.get().length();
        text.onChange(v -> caret = Math.min(caret, v.length()));
    }

    public static TextArea of(State<String> text) {
        return new TextArea(text);
    }

    /** Height in text rows. Default 4. */
    public TextArea rows(int rows) {
        this.visibleRows = Math.max(1, rows);
        invalidate();
        return this;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public Size measure(TextMeasurer tm) {
        return new Size(style.width != null ? style.width : 160,
                visibleRows * (tm.height() + 1) + 6);
    }

    private String[] lines() {
        return text.get().split("\n", -1);
    }

    /** Caret as {row, column}. */
    private int[] caretRowCol() {
        String s = text.get();
        int row = 0, lineStart = 0;
        for (int i = 0; i < caret && i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                row++;
                lineStart = i + 1;
            }
        }
        return new int[]{row, caret - lineStart};
    }

    private int indexOf(int row, int col) {
        String[] lines = lines();
        row = Math.max(0, Math.min(lines.length - 1, row));
        col = Math.max(0, Math.min(lines[row].length(), col));
        int index = 0;
        for (int i = 0; i < row; i++) index += lines[i].length() + 1;
        return index + col;
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        if (e.kind() != UiEvent.Kind.DOWN || !e.inside(bounds)) return false;
        Focus.request(this);
        pendingClick = new double[]{e.x(), e.y()};
        return true;
    }

    /** Click position, resolved at paint time when a text measurer is available. */
    private double[] pendingClick;

    @Override
    public boolean onKey(UiEvent.Key e) {
        if (!focused()) return false;
        String s = text.get();
        int[] rc = caretRowCol();
        switch (e.key()) {
            case PaneKeys.BACKSPACE -> {
                if (caret > 0) {
                    caret--;
                    text.set(s.substring(0, caret) + s.substring(caret + 1));
                }
            }
            case PaneKeys.DELETE -> {
                if (caret < s.length()) text.set(s.substring(0, caret) + s.substring(caret + 1));
            }
            case PaneKeys.LEFT -> caret = Math.max(0, caret - 1);
            case PaneKeys.RIGHT -> caret = Math.min(s.length(), caret + 1);
            case PaneKeys.UP -> caret = indexOf(rc[0] - 1, rc[1]);
            case PaneKeys.DOWN -> caret = indexOf(rc[0] + 1, rc[1]);
            case PaneKeys.HOME -> caret = indexOf(rc[0], 0);
            case PaneKeys.END -> caret = indexOf(rc[0], Integer.MAX_VALUE);
            case PaneKeys.ENTER -> {
                text.set(s.substring(0, caret) + "\n" + s.substring(caret));
                caret++;
            }
            case PaneKeys.ESCAPE -> Focus.clear();
            default -> { }
        }
        return true;
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
        r.border(bounds, focused() ? theme.accent() : (hovered ? theme.textDim() : theme.border()), 1);

        int lineH = r.height() + 1;
        String[] lines = lines();
        int[] rc = caretRowCol();

        if (pendingClick != null) {
            int row = Math.max(0, Math.min(lines.length - 1,
                    scrollRow + (int) ((pendingClick[1] - bounds.y() - 3) / lineH)));
            String line = lines[row];
            int col = line.length();
            for (int i = 0; i <= line.length(); i++) {
                if (bounds.x() + 4 + r.width(line.substring(0, i)) >= pendingClick[0]) {
                    col = i;
                    break;
                }
            }
            caret = indexOf(row, col);
            rc = caretRowCol();
            pendingClick = null;
        }

        // Follow the caret vertically.
        if (rc[0] < scrollRow) scrollRow = rc[0];
        if (rc[0] >= scrollRow + visibleRows) scrollRow = rc[0] - visibleRows + 1;
        scrollRow = Math.max(0, Math.min(scrollRow, Math.max(0, lines.length - visibleRows)));

        r.pushClip(bounds.inset(1));
        for (int i = 0; i < visibleRows; i++) {
            int row = scrollRow + i;
            if (row >= lines.length) break;
            int ty = bounds.y() + 3 + i * lineH;
            r.text(lines[row], bounds.x() + 4, ty, theme.text());
            if (focused() && row == rc[0] && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cx = bounds.x() + 4 + r.width(lines[row].substring(0, Math.min(rc[1], lines[row].length())));
                r.fill(new Rect(cx, ty - 1, 1, r.height() + 1), theme.accent());
            }
        }
        r.popClip();
    }
}
