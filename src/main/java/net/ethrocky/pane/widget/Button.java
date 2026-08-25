package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

public class Button extends Component {
    private final String label;
    private Runnable onClick = () -> {};
    private boolean pressed;

    private Button(String label) {
        this.label = label;
    }

    public static Button of(String label) {
        return new Button(label);
    }

    public Button onClick(Runnable action) {
        this.onClick = action;
        return this;
    }

    @Override
    public Size measure(TextMeasurer tm) {
        return new Size(tm.width(label) + 12, tm.height() + 8);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        switch (e.kind()) {
            case DOWN -> {
                if (e.inside(bounds)) {
                    pressed = true;
                    return true;
                }
            }
            case UP -> {
                if (pressed) {
                    pressed = false;
                    if (e.inside(bounds)) onClick.run();
                    return true;
                }
            }
            default -> { }
        }
        return false;
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        r.fill(bounds, pressed ? theme.surface() : theme.surfaceRaised());
        r.border(bounds, hovered ? theme.accent() : theme.border(), 1);
        int x = bounds.x() + (bounds.w() - r.width(label)) / 2;
        int y = bounds.y() + (bounds.h() - r.height()) / 2;
        r.text(label, x, y, theme.text());
    }
}
