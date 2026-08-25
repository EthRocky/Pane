package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

/** Fixed-height viewport over a COLUMN of children. The children are laid out in
 *  real screen space, just shifted up by the scroll offset — so hit-testing and
 *  every widget behavior work unchanged; paint clips them to the viewport. */
public class ScrollContainer extends Component {
    private static final int BAR_W = 4;
    private static final int WHEEL_STEP = 12;

    private int scrollY;
    private int contentH;
    private boolean draggingThumb;
    private double dragGrabY;

    private ScrollContainer(int viewportHeight) {
        style.height(viewportHeight);
    }

    public static ScrollContainer of(int viewportHeight) {
        return new ScrollContainer(viewportHeight);
    }

    public int scrollY() {
        return scrollY;
    }

    @Override
    public Size measure(TextMeasurer tm) {
        int maxW = 0;
        contentH = 0;
        for (Component c : children) {
            Size s = c.measure(tm);
            maxW = Math.max(maxW, s.w());
            contentH += s.h();
        }
        if (children.size() > 1) contentH += gapPx() * (children.size() - 1);
        int w = style.width != null ? style.width : maxW + pad() * 2 + BAR_W + 2;
        int h = style.height != null ? style.height : 150;
        return new Size(w, h);
    }

    private int viewportH() {
        return Math.max(0, bounds.h() - pad() * 2);
    }

    private int maxScroll() {
        return Math.max(0, contentH - viewportH());
    }

    private int clampScroll(int value) {
        return Math.max(0, Math.min(value, maxScroll()));
    }

    @Override
    public void layout(Rect b, TextMeasurer tm) {
        // Re-clamp against the new viewport before children take positions from it.
        scrollY = Math.max(0, Math.min(scrollY, Math.max(0, contentH - (b.h() - pad() * 2))));
        super.layout(b, tm);
    }

    @Override
    protected Rect innerRect() {
        return new Rect(bounds.x() + pad(), bounds.y() + pad() - scrollY,
                Math.max(0, bounds.w() - pad() * 2 - BAR_W - 2),
                Math.max(contentH, viewportH()));
    }

    private Rect trackRect() {
        return new Rect(bounds.right() - pad() - BAR_W, bounds.y() + pad(), BAR_W, viewportH());
    }

    private Rect thumbRect() {
        Rect track = trackRect();
        if (maxScroll() == 0) return track;
        int th = Math.max(16, track.h() * viewportH() / Math.max(1, contentH));
        int ty = track.y() + (int) ((track.h() - th) * (scrollY / (double) maxScroll()));
        return new Rect(track.x(), ty, BAR_W, th);
    }

    /** DOWN and SCROLL only count inside the viewport; MOVE and UP always flow so a
     *  drag that leaves the container still updates and releases. */
    @Override
    public boolean onMouse(UiEvent.Mouse e) {
        boolean gated = e.kind() == UiEvent.Kind.DOWN || e.kind() == UiEvent.Kind.SCROLL;
        if (gated && !e.inside(bounds)) return false;
        return super.onMouse(e);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        switch (e.kind()) {
            case SCROLL -> {
                scrollY = clampScroll(scrollY - (int) Math.round(e.scrollY() * WHEEL_STEP));
                invalidate();
                return true;
            }
            case DOWN -> {
                Rect thumb = thumbRect();
                if (maxScroll() > 0 && e.inside(thumb)) {
                    draggingThumb = true;
                    dragGrabY = e.y() - thumb.y();
                    return true;
                }
                if (maxScroll() > 0 && e.inside(trackRect())) {
                    jumpTo(e.y() - thumbRect().h() / 2.0);
                    return true;
                }
                return e.inside(bounds);
            }
            case MOVE -> {
                if (draggingThumb) {
                    jumpTo(e.y() - dragGrabY);
                    return true;
                }
            }
            case UP -> {
                if (draggingThumb) {
                    draggingThumb = false;
                    return true;
                }
            }
        }
        return false;
    }

    /** Positions the thumb's top edge at the given y and derives the scroll. */
    private void jumpTo(double thumbTop) {
        Rect track = trackRect();
        int th = thumbRect().h();
        int span = track.h() - th;
        if (span <= 0) return;
        double t = (thumbTop - track.y()) / span;
        scrollY = clampScroll((int) Math.round(t * maxScroll()));
        invalidate();
    }

    @Override
    public void paint(PaneRenderer r, Theme theme) {
        paintSelf(r, theme);
        r.pushClip(new Rect(bounds.x(), bounds.y() + pad(), bounds.w(), viewportH()));
        for (Component c : children) c.paint(r, theme);
        r.popClip();
        if (maxScroll() > 0) {
            r.fill(trackRect(), theme.border());
            r.fill(thumbRect(), draggingThumb ? theme.accent() : theme.textDim());
        }
    }
}
