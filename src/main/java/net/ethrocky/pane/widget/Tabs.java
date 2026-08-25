package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Style;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

import java.util.ArrayList;
import java.util.List;

/** Tab strip over exactly one active content component. Children are always
 *  [bar, active content]; select() swaps the second. */
public class Tabs extends Component {
    private final List<String> titles = new ArrayList<>();
    private final List<Component> contents = new ArrayList<>();
    private final Panel bar;
    private int active = -1;

    private Tabs() {
        style(Style.empty().padding(0).gap(4));
        // Content tracks the width Tabs is given — a child's natural width must
        // never push past the container (it clamps or wraps inside instead).
        align(net.ethrocky.pane.core.layout.Flex.Align.STRETCH);
        bar = Panel.row();
        bar.style(Style.empty().padding(0).gap(2));
        add(bar);
    }

    public static Tabs of() {
        return new Tabs();
    }

    public Tabs tab(String title, Component content) {
        titles.add(title);
        contents.add(content);
        bar.add(new TabButton(this, titles.size() - 1));
        if (active < 0) select(0);
        return this;
    }

    public void select(int index) {
        if (index == active || index < 0 || index >= contents.size()) return;
        if (active >= 0) children.remove(contents.get(active));
        active = index;
        add(contents.get(index));
        invalidate();
    }

    public int active() {
        return active;
    }

    private static final class TabButton extends Component {
        private final Tabs owner;
        private final int index;

        TabButton(Tabs owner, int index) {
            this.owner = owner;
            this.index = index;
        }

        @Override
        public Size measure(TextMeasurer tm) {
            return new Size(tm.width(owner.titles.get(index)) + 12, tm.height() + 6);
        }

        @Override
        protected boolean handleMouse(UiEvent.Mouse e) {
            if (e.kind() == UiEvent.Kind.DOWN && e.inside(bounds)) {
                owner.select(index);
                return true;
            }
            return false;
        }

        @Override
        protected void paintSelf(PaneRenderer r, Theme theme) {
            boolean act = owner.active == index;
            r.fill(bounds, act ? theme.surfaceRaised() : theme.surface());
            r.border(bounds, act ? theme.accent() : (hovered ? theme.textDim() : theme.border()), 1);
            String title = owner.titles.get(index);
            int tx = bounds.x() + (bounds.w() - r.width(title)) / 2;
            int ty = bounds.y() + (bounds.h() - r.height()) / 2;
            r.text(title, tx, ty, act ? theme.text() : theme.textDim());
        }
    }
}
