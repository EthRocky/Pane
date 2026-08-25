package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

public class Slider extends Component {
    private final State<Float> value;
    private final float min, max;
    private boolean dragging;

    private Slider(State<Float> value, float min, float max) {
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public static Slider of(State<Float> value, float min, float max) {
        return new Slider(value, min, max);
    }

    @Override
    public Size measure(TextMeasurer tm) {
        return new Size(style.width != null ? style.width : 120, tm.height() + 6);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        switch (e.kind()) {
            case DOWN -> {
                if (e.inside(bounds)) {
                    dragging = true;
                    setFromX(e.x());
                    return true;
                }
            }
            case MOVE -> {
                // A live drag follows the cursor even once it leaves the track.
                if (dragging) {
                    setFromX(e.x());
                    return true;
                }
            }
            case UP -> {
                if (dragging) {
                    dragging = false;
                    return true;
                }
            }
            default -> { }
        }
        return false;
    }

    private void setFromX(double x) {
        double t = bounds.w() <= 0 ? 0 : (x - bounds.x()) / bounds.w();
        t = Math.max(0, Math.min(1, t));
        value.set(min + (float) t * (max - min));
    }

    private float fraction() {
        float range = max - min;
        float t = range == 0 ? 0 : (value.get() - min) / range;
        return Math.max(0, Math.min(1, t));
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        int trackY = bounds.y() + bounds.h() / 2 - 1;
        r.fill(new Rect(bounds.x(), trackY, bounds.w(), 2), theme.border());
        int fillW = Math.round(bounds.w() * fraction());
        if (fillW > 0) r.fill(new Rect(bounds.x(), trackY, fillW, 2), theme.accent());
        int handleX = bounds.x() + Math.max(0, Math.min(bounds.w() - 4, fillW - 2));
        r.fill(new Rect(handleX, bounds.y(), 4, bounds.h()),
                hovered || dragging ? theme.accent() : theme.text());
    }
}
