package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.layout.Flex;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

/** Top-level draggable, resizable container. It owns its own screen position; the
 *  overlay host measures it and lays it out at that origin each time it goes dirty.
 *  Children stretch to the window's width. Resizing below the content's natural
 *  height turns the body into a scrolling viewport with a scrollbar. */
public class Window extends Component {
    private static final int BAR_W = 4;
    private static final int EDGE = 4;      // grab strip along each side
    private static final int CORNER = 10;   // widened zone near corners

    /** Resize edge bits, also returned by resizeEdgesAt() for hover feedback. */
    public static final int LEFT = 1, RIGHT = 2, TOP = 4, BOTTOM = 8;

    private final String title;
    private Runnable onClose;
    private int posX = 40, posY = 40;
    private int titleH = 17;
    private int contentH;
    private int scrollY;
    private boolean dragging, pressedClose, resizing, draggingThumb;
    private int resizeEdges;
    private double dragOffX, dragOffY;
    private double resizeGrabX, resizeGrabY, thumbGrabY;
    private int resizeStartW, resizeStartH, resizeStartX, resizeStartY;
    private Integer userW, userH;   // set by the resize grip or size(); null = auto

    // Behavior switches — everything on by default, each opt-out per window.
    private boolean resizable = true;
    private boolean scrollable = true;
    private boolean draggable = true;
    private int minWidth = 120;
    private int minBody = 40;

    private Window(String title) {
        this.title = title;
        // Children track the window's width, so resizing reflows the content.
        align(Flex.Align.STRETCH);
    }

    public static Window of(String title) {
        return new Window(title);
    }

    public Window at(int x, int y) {
        this.posX = x;
        this.posY = y;
        invalidate();
        return this;
    }

    public Window closable(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    /** Corner grip on/off. Off also hides the grip. Default true. */
    public Window resizable(boolean resizable) {
        this.resizable = resizable;
        invalidate();
        return this;
    }

    /** Whether shrinking below the content height scrolls the body. Off restores
     *  the hard floor: the window never gets smaller than its content. Default true. */
    public Window scrollable(boolean scrollable) {
        this.scrollable = scrollable;
        invalidate();
        return this;
    }

    /** Title-bar dragging on/off. Default true. */
    public Window draggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    /** Programmatic size, same as dragging the grip there. */
    public Window size(int w, int h) {
        this.userW = w;
        this.userH = h;
        invalidate();
        return this;
    }

    /** Floors used by the grip and size(): minimum width and minimum body height. */
    public Window minSize(int minWidth, int minBodyHeight) {
        this.minWidth = minWidth;
        this.minBody = minBodyHeight;
        invalidate();
        return this;
    }

    public int posX() { return posX; }
    public int posY() { return posY; }

    @Override
    public Size measure(TextMeasurer tm) {
        titleH = tm.height() + 8;
        Size content = super.measure(tm);
        contentH = content.h();
        // Auto width = fit the content (220 floor keeps slim columns from collapsing);
        // an explicit size() or a user resize still wins.
        int w = userW != null ? userW : (style.width != null ? style.width : Math.max(220, content.w()));
        // Below the natural height the body scrolls instead of clamping — unless
        // scrolling is off, in which case the content height is a hard floor.
        int h = userH != null ? Math.max(userH, titleH + minBody) : titleH + contentH;
        if (!scrollable) h = Math.max(h, titleH + contentH);
        return new Size(Math.max(w, minWidth), h);
    }

    private int viewportH() {
        return Math.max(0, bounds.h() - titleH);
    }

    private boolean scrollActive() {
        return scrollable && contentH > viewportH();
    }

    private int maxScroll() {
        return Math.max(0, contentH - viewportH());
    }

    private int clampScroll(int value) {
        return Math.max(0, Math.min(value, maxScroll()));
    }

    @Override
    public void layout(Rect b, TextMeasurer tm) {
        scrollY = Math.max(0, Math.min(scrollY, Math.max(0, contentH - (b.h() - titleH))));
        super.layout(b, tm);
    }

    @Override
    protected Rect innerRect() {
        int barW = scrollActive() ? BAR_W + 4 : 0;
        int innerH = scrollActive() ? contentH - pad() * 2
                : Math.max(0, bounds.h() - titleH - pad() * 2);
        return new Rect(bounds.x() + pad(), bounds.y() + titleH + pad() - scrollY,
                Math.max(0, bounds.w() - pad() * 2 - barW), Math.max(0, innerH));
    }

    private Rect bodyRect() {
        return new Rect(bounds.x(), bounds.y() + titleH, bounds.w(), viewportH());
    }

    private Rect titleBarRect() {
        return new Rect(bounds.x(), bounds.y(), bounds.w(), titleH);
    }

    private Rect closeRect() {
        int s = titleH - 6;
        return new Rect(bounds.right() - s - 3, bounds.y() + 3, s, s);
    }

    private Rect gripRect() {
        return new Rect(bounds.right() - 10, bounds.bottom() - 10, 10, 10);
    }

    /** Which edges a point grabs: 4px strips per side, widened near corners so a
     *  diagonal grab is easy. The old corner grip square counts as bottom-right. */
    private int edgesAt(double x, double y) {
        if (!resizable || !bounds.contains(x, y)) return 0;
        int ex = x < bounds.x() + EDGE ? LEFT : (x > bounds.right() - EDGE ? RIGHT : 0);
        int ey = y < bounds.y() + EDGE ? TOP : (y > bounds.bottom() - EDGE ? BOTTOM : 0);
        if (ex != 0 && ey == 0) {
            if (y < bounds.y() + CORNER) ey = TOP;
            else if (y > bounds.bottom() - CORNER) ey = BOTTOM;
        }
        if (ey != 0 && ex == 0) {
            if (x < bounds.x() + CORNER) ex = LEFT;
            else if (x > bounds.right() - CORNER) ex = RIGHT;
        }
        if (ex == 0 && ey == 0 && gripRect().contains(x, y)) return RIGHT | BOTTOM;
        return ex | ey;
    }

    /** What a resize grab at (x, y) would take — hosts use it for cursor shapes. */
    public int resizeEdgesAt(double x, double y) {
        return edgesAt(x, y);
    }

    /** The edges of a resize drag in progress, 0 when not resizing. */
    public int activeResizeEdges() {
        return resizing ? resizeEdges : 0;
    }

    private Rect trackRect() {
        // Ends short of the corner so the thumb never sits on the resize grip.
        return new Rect(bounds.right() - BAR_W - 2, bounds.y() + titleH + 2,
                BAR_W, Math.max(0, viewportH() - 16));
    }

    private Rect thumbRect() {
        Rect track = trackRect();
        if (maxScroll() == 0 || track.h() <= 0) return track;
        int th = Math.max(14, Math.min(track.h(), track.h() * viewportH() / Math.max(1, contentH)));
        int ty = track.y() + (int) ((track.h() - th) * (scrollY / (double) maxScroll()));
        return new Rect(track.x(), ty, BAR_W, th);
    }

    /** While scrolled, children can extend under the title bar or past the bottom —
     *  presses and scrolls outside the visible body must not reach them. */
    @Override
    public boolean onMouse(UiEvent.Mouse e) {
        boolean gated = e.kind() == UiEvent.Kind.DOWN || e.kind() == UiEvent.Kind.SCROLL;
        if (scrollActive() && gated && !e.inside(bodyRect())) {
            return handleMouse(e);
        }
        return super.onMouse(e);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        switch (e.kind()) {
            case DOWN -> {
                if (onClose != null && e.inside(closeRect())) {
                    pressedClose = true;
                    return true;
                }
                if (scrollActive() && e.inside(thumbRect())) {
                    draggingThumb = true;
                    thumbGrabY = e.y() - thumbRect().y();
                    return true;
                }
                if (scrollActive() && e.inside(trackRect())) {
                    thumbTo(e.y() - thumbRect().h() / 2.0);
                    return true;
                }
                int edges = edgesAt(e.x(), e.y());
                if (edges != 0) {
                    resizing = true;
                    resizeEdges = edges;
                    resizeGrabX = e.x();
                    resizeGrabY = e.y();
                    resizeStartW = bounds.w();
                    resizeStartH = bounds.h();
                    resizeStartX = posX;
                    resizeStartY = posY;
                    return true;
                }
                if (draggable && e.inside(titleBarRect())) {
                    dragging = true;
                    dragOffX = e.x() - posX;
                    dragOffY = e.y() - posY;
                    return true;
                }
                // Clicks on window body (not on a widget) still belong to the window.
                return e.inside(bounds);
            }
            case MOVE -> {
                if (resizing) {
                    int dx = (int) Math.round(e.x() - resizeGrabX);
                    int dy = (int) Math.round(e.y() - resizeGrabY);
                    int minH = titleH + minBody;
                    if ((resizeEdges & RIGHT) != 0) {
                        userW = Math.max(minWidth, resizeStartW + dx);
                    }
                    if ((resizeEdges & LEFT) != 0) {
                        // The right edge stays put: the origin absorbs whatever the width gives up.
                        int w = Math.max(minWidth, resizeStartW - dx);
                        userW = w;
                        posX = resizeStartX + (resizeStartW - w);
                    }
                    if ((resizeEdges & BOTTOM) != 0) {
                        userH = Math.max(minH, resizeStartH + dy);
                    }
                    if ((resizeEdges & TOP) != 0) {
                        int h = Math.max(minH, resizeStartH - dy);
                        userH = h;
                        posY = resizeStartY + (resizeStartH - h);
                    }
                    invalidate();
                    return true;
                }
                if (draggingThumb) {
                    thumbTo(e.y() - thumbGrabY);
                    return true;
                }
                if (dragging) {
                    posX = (int) Math.round(e.x() - dragOffX);
                    posY = (int) Math.round(e.y() - dragOffY);
                    invalidate();
                    return true;
                }
            }
            case UP -> {
                if (pressedClose) {
                    pressedClose = false;
                    if (e.inside(closeRect())) onClose.run();
                    return true;
                }
                if (resizing) {
                    resizing = false;
                    return true;
                }
                if (draggingThumb) {
                    draggingThumb = false;
                    return true;
                }
                if (dragging) {
                    dragging = false;
                    return true;
                }
                return e.inside(bounds);
            }
            case SCROLL -> {
                if (scrollActive() && e.inside(bounds)) {
                    scrollY = clampScroll(scrollY - (int) Math.round(e.scrollY() * 12));
                    invalidate();
                }
                return e.inside(bounds);
            }
        }
        return false;
    }

    /** Positions the thumb's top edge at the given y and derives the scroll. */
    private void thumbTo(double thumbTop) {
        Rect track = trackRect();
        int span = track.h() - thumbRect().h();
        if (span <= 0) return;
        double t = (thumbTop - track.y()) / span;
        scrollY = clampScroll((int) Math.round(t * maxScroll()));
        invalidate();
    }

    @Override
    public void paint(PaneRenderer r, Theme theme) {
        paintSelf(r, theme);
        if (scrollActive()) {
            r.pushClip(new Rect(bounds.x() + 1, bounds.y() + titleH, bounds.w() - 2, viewportH() - 1));
            for (Component c : children) c.paint(r, theme);
            r.popClip();
            r.fill(trackRect(), theme.border());
            r.fill(thumbRect(), draggingThumb ? theme.accent() : theme.textDim());
        } else {
            for (Component c : children) c.paint(r, theme);
        }
        if (resizable) {
            // Resize grip last so nothing overdraws it: three diagonal steps.
            int gx = bounds.right() - 3, gy = bounds.bottom() - 3;
            int grip = resizing ? theme.accent() : theme.textDim();
            r.fill(new Rect(gx - 6, gy, 6, 1), grip);
            r.fill(new Rect(gx - 4, gy - 2, 4, 1), grip);
            r.fill(new Rect(gx - 2, gy - 4, 2, 1), grip);
        }
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        r.fill(bounds, theme.surface());
        r.fill(titleBarRect(), theme.surfaceRaised());
        r.border(bounds, theme.border(), 1);
        r.text(title, bounds.x() + 6, bounds.y() + (titleH - r.height()) / 2, theme.text());
        if (onClose != null) {
            Rect c = closeRect();
            r.border(c, hovered ? theme.accent() : theme.border(), 1);
            r.text("x", c.x() + (c.w() - r.width("x")) / 2, c.y() + (c.h() - r.height()) / 2, theme.textDim());
        }
    }
}
