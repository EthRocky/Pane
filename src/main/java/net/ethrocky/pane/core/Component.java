package net.ethrocky.pane.core;

import net.ethrocky.pane.core.layout.Flex;
import net.ethrocky.pane.core.layout.FlexLayout;
import net.ethrocky.pane.core.style.Style;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Base of the retained tree. Default behavior is a flex container: measure
 *  derives size from children, layout runs FlexLayout over them, paint draws
 *  the style background/border then recurses. Widgets override the pieces
 *  they change. */
public abstract class Component {

    /** Text metrics without dragging Minecraft into core; McRenderer implements it. */
    public interface TextMeasurer {
        int width(String s);

        int height();
    }

    protected Component parent;
    protected final List<Component> children = new ArrayList<>();
    protected Style style = Style.empty();
    protected Flex.Direction direction = Flex.Direction.COLUMN;
    protected Flex.Justify justify = Flex.Justify.START;
    protected Flex.Align align = Flex.Align.START;
    protected Rect bounds = new Rect(0, 0, 0, 0);
    protected boolean hovered;
    private boolean dirty = true;

    // ---- tree ----

    public Component add(Component child) {
        child.parent = this;
        children.add(child);
        invalidate();
        return this;
    }

    public Rect bounds() {
        return bounds;
    }

    /** Removes every child (for rows rebuilt from changing data). */
    public Component clearChildren() {
        children.clear();
        invalidate();
        return this;
    }

    public int childCount() {
        return children.size();
    }

    public Component childAt(int index) {
        return children.get(index);
    }

    // ---- fluent config ----

    public Component style(Style s)                  { this.style = s; invalidate(); return this; }
    public Component grow(int weight)                { style.grow(weight); invalidate(); return this; }
    public Component direction(Flex.Direction d)     { this.direction = d; invalidate(); return this; }
    public Component justify(Flex.Justify j)         { this.justify = j; invalidate(); return this; }
    public Component align(Flex.Align a)             { this.align = a; invalidate(); return this; }

    /** Both built-in themes use padding 6 / gap 4, so those are the unstyled defaults. */
    protected int pad()   { return style.padding != null ? style.padding : 6; }
    protected int gapPx() { return style.gap != null ? style.gap : 4; }

    // ---- lifecycle ----

    public Size measure(TextMeasurer tm) {
        boolean row = direction == Flex.Direction.ROW;
        int main = 0, cross = 0;
        for (Component c : children) {
            Size s = c.measure(tm);
            main += row ? s.w() : s.h();
            cross = Math.max(cross, row ? s.h() : s.w());
        }
        if (children.size() > 1) main += gapPx() * (children.size() - 1);
        int w = (row ? main : cross) + pad() * 2;
        int h = (row ? cross : main) + pad() * 2;
        if (style.width != null) w = style.width;
        if (style.height != null) h = style.height;
        return new Size(w, h);
    }

    public void layout(Rect bounds, TextMeasurer tm) {
        this.bounds = bounds;
        dirty = false;
        if (children.isEmpty()) return;
        boolean row = direction == Flex.Direction.ROW;
        List<FlexLayout.Item> items = new ArrayList<>(children.size());
        for (Component c : children) {
            Size s = c.measure(tm);
            int grow = c.style.grow != null ? c.style.grow : 0;
            items.add(new FlexLayout.Item(row ? s.w() : s.h(), row ? s.h() : s.w(), grow));
        }
        List<Rect> rects = FlexLayout.layout(innerRect(), direction, justify, align, gapPx(), items);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).layout(rects.get(i), tm);
        }
    }

    /** Region children lay out in. Widgets with chrome (e.g. a title bar) override. */
    protected Rect innerRect() {
        return bounds.inset(pad());
    }

    public void paint(PaneRenderer r, Theme theme) {
        paintSelf(r, theme);
        for (Component c : children) c.paint(r, theme);
    }

    protected void paintSelf(PaneRenderer r, Theme theme) {
        if (style.bg != null) r.fill(bounds, style.bg);
        if (style.borderColor != null) {
            r.border(bounds, style.borderColor, style.borderWidth != null ? style.borderWidth : 1);
        }
    }

    // ---- events ----

    /** Routes to children topmost (last-added) first. MOVE visits every child so
     *  hover state stays honest; other kinds stop at the first consumer. */
    public boolean onMouse(UiEvent.Mouse e) {
        if (e.kind() == UiEvent.Kind.MOVE) hovered = e.inside(bounds);
        boolean consumed = false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).onMouse(e)) {
                consumed = true;
                if (e.kind() != UiEvent.Kind.MOVE) return true;
            }
        }
        return consumed || openContextMenu(e) || handleMouse(e);
    }

    private java.util.function.Supplier<Component> contextMenuSupplier;

    /** A right-click on this widget (that no child consumed) opens the supplied menu
     *  at the cursor, on the shared popup layer — click-away closes it for free.
     *  The supplier runs per open, so the menu can reflect current state. */
    public Component contextMenu(java.util.function.Supplier<Component> menu) {
        this.contextMenuSupplier = menu;
        return this;
    }

    private boolean openContextMenu(UiEvent.Mouse e) {
        if (contextMenuSupplier == null) return false;
        if (e.kind() != UiEvent.Kind.DOWN || e.button() != 1 || !e.inside(bounds)) return false;
        Component menu = contextMenuSupplier.get();
        if (menu != null) Popups.open(menu, (int) e.x(), (int) e.y());
        return true;
    }

    /** Widget-level handling after children had their chance. */
    protected boolean handleMouse(UiEvent.Mouse e) {
        return false;
    }

    /** Whether this widget can own the keyboard (see Focus). */
    public boolean focusable() {
        return false;
    }

    /** A key press/repeat routed to the focus owner. */
    public boolean onKey(UiEvent.Key e) {
        return false;
    }

    /** A typed character routed to the focus owner. */
    public boolean onChar(UiEvent.Char e) {
        return false;
    }

    public boolean focused() {
        return Focus.current() == this;
    }

    public boolean isDescendantOf(Component ancestor) {
        for (Component c = this; c != null; c = c.parent) {
            if (c == ancestor) return true;
        }
        return false;
    }

    /** Topmost ancestor (the window or dialog this widget lives in). */
    public Component root() {
        Component c = this;
        while (c.parent != null) c = c.parent;
        return c;
    }

    // ---- markup metadata (selector matching + tree lookup) ----

    private String tag;
    private String id;
    private final Set<String> cssClasses = new LinkedHashSet<>();

    public Component tag(String tag) {
        this.tag = tag;
        return this;
    }

    /** Selector type name: the PML element name when built from markup, otherwise
     *  the lowercased class name (Label → "label"). */
    public String tag() {
        if (tag != null) return tag;
        String simple = getClass().getSimpleName().toLowerCase();
        return simple.isEmpty() ? "component" : simple;
    }

    public Component id(String id) {
        this.id = id;
        return this;
    }

    public String id() {
        return id;
    }

    public Component cssClass(String name) {
        cssClasses.add(name);
        return this;
    }

    public Set<String> cssClasses() {
        return cssClasses;
    }

    public Style style() {
        return style;
    }

    // ---- tooltip ----

    private String tooltip;

    /** Hover text drawn by the host near the cursor. */
    public Component tooltip(String text) {
        this.tooltip = text;
        return this;
    }

    public String tooltipText() {
        return tooltip;
    }

    /** Deepest component under (x, y) that carries a tooltip, topmost child first. */
    public Component findTooltip(double x, double y) {
        if (!bounds.contains(x, y)) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            Component hit = children.get(i).findTooltip(x, y);
            if (hit != null) return hit;
        }
        return tooltip != null ? this : null;
    }

    /** Marks the tree dirty; the host re-measures and re-lays out the root next frame. */
    public void invalidate() {
        if (parent != null) parent.invalidate();
        else dirty = true;
    }

    /** Host-side: returns and clears the root dirty flag. */
    public boolean consumeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }
}
