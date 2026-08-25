package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

public class ProgressBar extends Component {
    private final State<Float> progress;

    private ProgressBar(State<Float> progress) {
        this.progress = progress;
    }

    public static ProgressBar of(State<Float> progress) {
        return new ProgressBar(progress);
    }

    @Override
    public Size measure(TextMeasurer tm) {
        return new Size(style.width != null ? style.width : 140,
                style.height != null ? style.height : 8);
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        r.fill(bounds, theme.surface());
        r.border(bounds, theme.border(), 1);
        float t = Math.max(0f, Math.min(1f, progress.get()));
        int fillW = Math.round((bounds.w() - 2) * t);
        if (fillW > 0) r.fill(new Rect(bounds.x() + 1, bounds.y() + 1, fillW, bounds.h() - 2), theme.accent());
    }
}
