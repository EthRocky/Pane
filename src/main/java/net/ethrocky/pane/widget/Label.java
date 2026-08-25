package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

public class Label extends Component {
    private String text;
    private boolean dim;

    private Label(String text) {
        this.text = text;
    }

    public static Label of(String text) {
        return new Label(text);
    }

    /** Live label: re-renders (and re-lays out, since width changes) on state change. */
    public static Label of(State<String> state) {
        Label l = new Label(state.get());
        state.onChange(v -> {
            l.text = v;
            l.invalidate();
        });
        return l;
    }

    public Label dim(boolean dim) {
        this.dim = dim;
        return this;
    }

    @Override
    public Size measure(TextMeasurer tm) {
        return new Size(tm.width(text), tm.height());
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        super.paintSelf(r, theme);
        int color = style.fg != null ? style.fg : (dim ? theme.textDim() : theme.text());
        int y = bounds.y() + (bounds.h() - r.height()) / 2;
        r.text(text, bounds.x(), y, color);
    }
}
