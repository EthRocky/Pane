package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

/** Clickable colour square — palettes, colour history, any "pick this colour" row. */
public class Swatch extends Component {
    private int rgb;
    private Runnable onClick = () -> {};
    private int size = 12;

    private Swatch(int rgb) {
        this.rgb = rgb;
    }

    public static Swatch of(int rgb) {
        return new Swatch(rgb);
    }

    public Swatch onClick(Runnable action) {
        this.onClick = action;
        return this;
    }

    public Swatch size(int px) {
        this.size = Math.max(4, px);
        invalidate();
        return this;
    }

    public Swatch color(int rgb) {
        this.rgb = rgb;
        return this;
    }

    public int color() {
        return rgb;
    }

    @Override
    public Size measure(TextMeasurer tm) {
        return new Size(size, size);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        if (e.kind() == UiEvent.Kind.DOWN && e.inside(bounds)) {
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        r.fill(bounds, 0xFF000000 | rgb);
        r.border(bounds, hovered ? theme.accent() : theme.border(), 1);
    }
}
