package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.layout.Flex;
import net.ethrocky.pane.core.style.Style;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

import java.util.List;

/** Column of selectable rows bound to a State. Nest inside a ScrollContainer
 *  when the item count outgrows the panel. */
public class ListView extends Component {
    private final State<String> selected;
    private State<java.util.Set<String>> selection;   // null = single-select only
    private List<String> items = List.of();
    private String anchor;                            // shift-range origin

    private ListView(State<String> selected, List<String> items) {
        this.selected = selected;
        style(Style.empty().padding(1).gap(1));
        align(Flex.Align.STRETCH);
        setItems(items);
    }

    public static ListView of(State<String> selected, List<String> items) {
        return new ListView(selected, items);
    }

    /** Opt in to multi-select: ctrl-click toggles a row, shift-click takes the range from the
     *  last anchor, a plain click collapses back to one. The single-selection State keeps
     *  tracking the most recent click either way, so property panels bound to it still work. */
    public ListView multiSelect(State<java.util.Set<String>> selection) {
        this.selection = selection;
        return this;
    }

    // ---- drag reorder (opt-in) ----

    private java.util.function.BiConsumer<Integer, Integer> onReorder;
    private int dragFrom = -1;      // row index a left-press started on
    private double dragStartY;
    private boolean dragLive;       // true once the pointer moved far enough to mean "drag"
    private int dropIndex = -1;     // insertion slot 0..items.size()

    /** Opt in to drag-reorder: rows can be dragged vertically; on drop the handler
     *  gets (fromIndex, toIndex) — indices into the CURRENT item list — and is
     *  expected to move the item in its backing data and call setItems again.
     *  Plain clicks still select; a drag only starts after a few pixels of travel. */
    public ListView reorderable(java.util.function.BiConsumer<Integer, Integer> onMove) {
        this.onReorder = onMove;
        return this;
    }

    // ---- per-row context menu (opt-in) ----

    private java.util.function.Function<String, Component> rowMenu;

    /** Right-clicking a row selects it, then opens the supplied menu (usually a
     *  ContextMenu) at the cursor on the shared popup layer. The function runs per
     *  open with the clicked item, so the menu can reflect that row's state. */
    public ListView rowContextMenu(java.util.function.Function<String, Component> menuFor) {
        this.rowMenu = menuFor;
        return this;
    }

    @Override
    public boolean onMouse(UiEvent.Mouse e) {
        if (onReorder != null && dragFrom >= 0) {
            if (e.kind() == UiEvent.Kind.MOVE) {
                if (!dragLive && Math.abs(e.y() - dragStartY) > 4) dragLive = true;
                if (dragLive) dropIndex = insertionIndexAt(e.y());
            } else if (e.kind() == UiEvent.Kind.UP) {
                boolean acted = false;
                if (dragLive && dropIndex >= 0) {
                    int to = dropIndex > dragFrom ? dropIndex - 1 : dropIndex;
                    if (to != dragFrom) {
                        onReorder.accept(dragFrom, to);
                        acted = true;
                    }
                }
                dragFrom = -1;
                dragLive = false;
                dropIndex = -1;
                if (acted) return true;
            }
        }
        return super.onMouse(e);
    }

    /** Insertion slot for a pointer at {@code y}: before the first row whose middle
     *  the pointer is above, else after the last. */
    private int insertionIndexAt(double y) {
        for (int i = 0; i < children.size(); i++) {
            var b = children.get(i).bounds();
            if (y < b.y() + b.h() / 2.0) return i;
        }
        return children.size();
    }

    @Override
    public void paint(PaneRenderer r, Theme theme) {
        super.paint(r, theme);
        if (dragLive && dropIndex >= 0 && !children.isEmpty()) {
            int y = dropIndex < children.size()
                    ? children.get(dropIndex).bounds().y() - 1
                    : children.get(children.size() - 1).bounds().bottom();
            r.fill(new net.ethrocky.pane.core.Rect(bounds.x() + 2, y, bounds.w() - 4, 2), theme.accent());
        }
    }

    /** Replaces all rows (e.g. when the backing data changes). */
    public void setItems(List<String> items) {
        this.items = List.copyOf(items);
        children.clear();
        for (String item : this.items) add(new Row(this, item));
        if (selection != null) {
            // Drop selections for rows that no longer exist.
            java.util.Set<String> kept = new java.util.LinkedHashSet<>(selection.get());
            if (kept.retainAll(this.items)) selection.set(kept);
        }
        invalidate();
    }

    private void click(String item, UiEvent.Mouse e) {
        if (selection == null) {
            selected.set(item);
            return;
        }
        java.util.Set<String> next = new java.util.LinkedHashSet<>(selection.get());
        if (e.shift() && anchor != null) {
            int from = items.indexOf(anchor), to = items.indexOf(item);
            if (from >= 0 && to >= 0) {
                // Shift alone replaces the selection with the range; ctrl+shift adds to it.
                // The anchor stays put so you can re-drag the range from the same origin.
                if (!e.ctrl()) next.clear();
                for (int i = Math.min(from, to); i <= Math.max(from, to); i++) next.add(items.get(i));
            }
        } else if (e.ctrl()) {
            if (!next.remove(item)) next.add(item);
            anchor = item;
        } else {
            next.clear();
            next.add(item);
            anchor = item;
        }
        selection.set(next);
        selected.set(item);
    }

    private boolean isSelected(String item) {
        if (selection != null && selection.get().contains(item)) return true;
        return selection == null && item.equals(selected.get());
    }

    private static final class Row extends Component {
        private final ListView owner;
        private final String item;

        Row(ListView owner, String item) {
            this.owner = owner;
            this.item = item;
        }

        @Override
        public Size measure(TextMeasurer tm) {
            return new Size(tm.width(item) + 8, tm.height() + 4);
        }

        @Override
        protected boolean handleMouse(UiEvent.Mouse e) {
            if (e.kind() == UiEvent.Kind.DOWN && e.inside(bounds)) {
                if (e.button() == 1 && owner.rowMenu != null) {
                    // Right-click targets the row: keep a multi-selection that already
                    // contains it; otherwise select just this row (no ctrl/shift semantics).
                    if (owner.selection == null || !owner.selection.get().contains(item)) {
                        owner.click(item, UiEvent.Mouse.down(e.x(), e.y(), 1, 0));
                    }
                    Component menu = owner.rowMenu.apply(item);
                    if (menu != null) {
                        net.ethrocky.pane.core.Popups.open(menu, (int) e.x(), (int) e.y());
                    }
                    return true;
                }
                owner.click(item, e);
                if (owner.onReorder != null && e.button() == 0) {
                    owner.dragFrom = owner.items.indexOf(item);
                    owner.dragStartY = e.y();
                    owner.dragLive = false;
                }
                return true;
            }
            return false;
        }

        @Override
        protected void paintSelf(PaneRenderer r, Theme theme) {
            boolean isSelected = owner.isSelected(item);
            boolean isFocused = item.equals(owner.selected.get());
            if (isSelected) r.fill(bounds, theme.accent());
            else if (hovered) r.fill(bounds, theme.border());
            // In multi-select the most recent click also gets an outline, so it stays clear
            // which row the property panel is showing.
            if (isFocused && owner.selection != null && !isSelected) {
                r.border(bounds, theme.accent(), 1);
            }
            int ty = bounds.y() + (bounds.h() - r.height()) / 2;
            r.text(item, bounds.x() + 4, ty, isSelected ? theme.surface() : theme.text());
        }
    }
}
