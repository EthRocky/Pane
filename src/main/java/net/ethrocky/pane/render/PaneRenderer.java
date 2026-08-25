package net.ethrocky.pane.render;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;

/** Everything a widget may draw with. The only implementation that touches
 *  Minecraft is McRenderer — keeping this interface pure keeps core headless. */
public interface PaneRenderer extends Component.TextMeasurer {
    void fill(Rect r, int argb);

    void border(Rect r, int argb, int width);

    void text(String s, int x, int y, int argb);

    /** Vertical gradient from topArgb at the rect's top edge to bottomArgb at its bottom. */
    void gradientV(Rect r, int topArgb, int bottomArgb);

    void pushClip(Rect r);

    void popClip();
}
