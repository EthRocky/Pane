package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

public class Checkbox extends Component {
    private final String label;
    private final State<Boolean> checked;
    private boolean pressed;

    private Checkbox(String label, State<Boolean> checked) {
        this.label = label;
        this.checked = checked;
    }

    public static Checkbox of(String label, State<Boolean> checked) {
        return new Checkbox(label, checked);
    }

    @Override
    public Size measure(TextMeasurer tm) {
        int box = tm.height();
        return new Size(box + 4 + tm.width(label), box);
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
                    if (e.inside(bounds)) checked.set(!checked.get());
                    return true;
                }
            }
            default -> { }
        }
        return false;
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        int box = bounds.h();
        Rect boxRect = new Rect(bounds.x(), bounds.y(), box, box);
        r.fill(boxRect, theme.surfaceRaised());
        r.border(boxRect, hovered ? theme.accent() : theme.border(), 1);
        if (checked.get()) r.fill(boxRect.inset(2), theme.accent());
        int y = bounds.y() + (bounds.h() - r.height()) / 2;
        r.text(label, bounds.x() + box + 4, y, theme.text());
    }
}
