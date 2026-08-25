package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Popups;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.layout.Flex;
import net.ethrocky.pane.core.style.Style;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

/** Right-click menu. Items close the menu before running. Opens on the shared
 *  popup layer, so it floats above every window and click-away closes it.
 *
 *  Attach to any widget — the base Component routes right-clicks for you:
 *  <pre>
 *  keyframe.contextMenu(() -> ContextMenu.of()
 *          .item("Delete", () -> deleteKeyframe(kf))
 *          .item("Duplicate", () -> duplicate(kf))
 *          .separator()
 *          .item("Reset value", () -> reset(kf)));
 *  </pre>
 *  or open one manually at any position with {@link #openAt(double, double)}. */
public class ContextMenu extends Component {

    private ContextMenu() {
        style(Style.empty().padding(2).gap(0));
        align(Flex.Align.STRETCH);
    }

    public static ContextMenu of() {
        return new ContextMenu();
    }

    public ContextMenu item(String label, Runnable action) {
        add(new Item(label, action, true));
        return this;
    }

    /** A greyed, non-clickable entry (e.g. a heading or unavailable action). */
    public ContextMenu disabledItem(String label) {
        add(new Item(label, null, false));
        return this;
    }

    public ContextMenu separator() {
        add(new Separator());
        return this;
    }

    /** Opens at (x, y) — e.g. the mouse position of a hand-routed right-click. */
    public void openAt(double x, double y) {
        Popups.open(this, (int) x, (int) y);
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        r.fill(bounds, theme.surfaceRaised());
        r.border(bounds, theme.border(), 1);
    }

    private static final class Item extends Component {
        private final String label;
        private final Runnable action;
        private final boolean enabled;

        Item(String label, Runnable action, boolean enabled) {
            this.label = label;
            this.action = action;
            this.enabled = enabled;
        }

        @Override
        public Size measure(TextMeasurer tm) {
            return new Size(tm.width(label) + 16, tm.height() + 6);
        }

        @Override
        protected boolean handleMouse(UiEvent.Mouse e) {
            if (e.kind() == UiEvent.Kind.DOWN && e.inside(bounds)) {
                if (enabled) {
                    Popups.close();
                    if (action != null) action.run();
                }
                return true;   // even a disabled item swallows the click
            }
            return false;
        }

        @Override
        protected void paintSelf(PaneRenderer r, Theme theme) {
            if (enabled && hovered) r.fill(bounds, theme.accent());
            int ty = bounds.y() + (bounds.h() - r.height()) / 2;
            int color = !enabled ? theme.textDim()
                    : hovered ? theme.surface()
                    : theme.text();
            r.text(label, bounds.x() + 8, ty, color);
        }
    }

    private static final class Separator extends Component {
        @Override
        public Size measure(TextMeasurer tm) {
            return new Size(0, 5);
        }

        @Override
        protected void paintSelf(PaneRenderer r, Theme theme) {
            r.fill(new net.ethrocky.pane.core.Rect(bounds.x() + 4, bounds.y() + 2, bounds.w() - 8, 1),
                    theme.border());
        }
    }
}
