package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Popups;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.layout.Flex;
import net.ethrocky.pane.core.style.Style;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

import java.util.List;

/** Selection box whose option list opens on the shared popup layer, so it floats
 *  above every window. The host closes it on click-away; clicking the box while
 *  open therefore closes it (the click-away fires first), and the next click
 *  reopens — standard toggle feel without extra state. */
public class Dropdown extends Component {
    private final State<String> selected;
    private final List<String> options;

    private Dropdown(State<String> selected, List<String> options) {
        this.selected = selected;
        this.options = options;
    }

    public static Dropdown of(State<String> selected, List<String> options) {
        return new Dropdown(selected, options);
    }

    @Override
    public Size measure(TextMeasurer tm) {
        int w;
        if (style.width != null) {
            w = style.width;
        } else {
            int max = 0;
            for (String o : options) max = Math.max(max, tm.width(o));
            w = max + 24;
        }
        return new Size(w, tm.height() + 8);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        if (e.kind() == UiEvent.Kind.DOWN && e.inside(bounds)) {
            Popups.open(new OptionList(this, bounds.w()), bounds.x(), bounds.bottom() + 1);
            return true;
        }
        return false;
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        r.fill(bounds, theme.surfaceRaised());
        r.border(bounds, hovered ? theme.accent() : theme.border(), 1);
        int ty = bounds.y() + (bounds.h() - r.height()) / 2;
        r.text(selected.get(), bounds.x() + 6, ty, theme.text());
        r.text("v", bounds.right() - 6 - r.width("v"), ty, theme.textDim());
    }

    private static final class OptionList extends Component {
        OptionList(Dropdown owner, int width) {
            style(Style.empty().width(width).padding(2).gap(1));
            align(Flex.Align.STRETCH);
            for (String opt : owner.options) add(new OptionRow(owner, opt));
        }

        @Override
        protected void paintSelf(PaneRenderer r, Theme theme) {
            r.fill(bounds, theme.surfaceRaised());
            r.border(bounds, theme.border(), 1);
        }
    }

    private static final class OptionRow extends Component {
        private final Dropdown owner;
        private final String option;

        OptionRow(Dropdown owner, String option) {
            this.owner = owner;
            this.option = option;
        }

        @Override
        public Size measure(TextMeasurer tm) {
            return new Size(tm.width(option) + 8, tm.height() + 4);
        }

        @Override
        protected boolean handleMouse(UiEvent.Mouse e) {
            if (e.kind() == UiEvent.Kind.DOWN && e.inside(bounds)) {
                owner.selected.set(option);
                Popups.close();
                return true;
            }
            return false;
        }

        @Override
        protected void paintSelf(PaneRenderer r, Theme theme) {
            if (hovered) r.fill(bounds, theme.accent());
            int ty = bounds.y() + (bounds.h() - r.height()) / 2;
            r.text(option, bounds.x() + 4, ty, hovered ? theme.surface() : theme.text());
        }
    }
}
