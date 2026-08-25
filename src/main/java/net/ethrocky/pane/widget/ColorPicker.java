package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

/** HSV picker bound to a 0xRRGGBB State. The SV square is exact in the value
 *  axis (a column at saturation s is the linear ramp v·hsv(h,s,1) → black) and
 *  stepped across 24 strips in the saturation axis. */
public class ColorPicker extends Component {
    private static final int STRIPS = 24;
    private static final int[] HUE_STOPS = {
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};

    private enum Drag { NONE, SV, HUE }

    private final State<Integer> rgb;
    private float hue, sat, val;
    private Drag drag = Drag.NONE;
    private boolean selfWrite;

    private ColorPicker(State<Integer> rgb) {
        this.rgb = rgb;
        float[] hsv = rgbToHsv(rgb.get());
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        rgb.onChange(v -> {
            if (selfWrite) return;
            float[] h = rgbToHsv(v);
            // Grey/black values carry no hue or saturation information — keep ours
            // so the marker doesn't jump while an external writer passes through them.
            if (h[1] > 0) {
                hue = h[0];
                sat = h[1];
            }
            val = h[2];
        });
    }

    public static ColorPicker of(State<Integer> rgb) {
        return new ColorPicker(rgb);
    }

    @Override
    public Size measure(TextMeasurer tm) {
        return new Size(style.width != null ? style.width : 150,
                style.height != null ? style.height : 96);
    }

    private Rect svRect() {
        return new Rect(bounds.x(), bounds.y(), bounds.w() - 16, bounds.h() - 18);
    }

    private Rect hueRect() {
        return new Rect(bounds.right() - 12, bounds.y(), 12, bounds.h() - 18);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        switch (e.kind()) {
            case DOWN -> {
                if (e.inside(svRect())) {
                    drag = Drag.SV;
                    applySv(e.x(), e.y());
                    return true;
                }
                if (e.inside(hueRect())) {
                    drag = Drag.HUE;
                    applyHue(e.y());
                    return true;
                }
                return e.inside(bounds);
            }
            case MOVE -> {
                if (drag == Drag.SV) {
                    applySv(e.x(), e.y());
                    return true;
                }
                if (drag == Drag.HUE) {
                    applyHue(e.y());
                    return true;
                }
            }
            case UP -> {
                if (drag != Drag.NONE) {
                    drag = Drag.NONE;
                    return true;
                }
            }
            default -> { }
        }
        return false;
    }

    private void applySv(double x, double y) {
        Rect sv = svRect();
        sat = clamp01((x - sv.x()) / sv.w());
        val = 1f - clamp01((y - sv.y()) / sv.h());
        push();
    }

    private void applyHue(double y) {
        Rect h = hueRect();
        hue = clamp01((y - h.y()) / h.h());
        push();
    }

    private void push() {
        selfWrite = true;
        rgb.set(hsvToRgb(hue, sat, val));
        selfWrite = false;
    }

    private static float clamp01(double t) {
        return (float) Math.max(0, Math.min(1, t));
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        Rect sv = svRect();
        for (int i = 0; i < STRIPS; i++) {
            int x0 = sv.x() + sv.w() * i / STRIPS;
            int x1 = sv.x() + sv.w() * (i + 1) / STRIPS;
            float s = (i + 0.5f) / STRIPS;
            int top = 0xFF000000 | hsvToRgb(hue, s, 1f);
            r.gradientV(new Rect(x0, sv.y(), x1 - x0, sv.h()), top, 0xFF000000);
        }
        r.border(sv, theme.border(), 1);

        Rect hb = hueRect();
        for (int i = 0; i < 6; i++) {
            int y0 = hb.y() + hb.h() * i / 6;
            int y1 = hb.y() + hb.h() * (i + 1) / 6;
            r.gradientV(new Rect(hb.x(), y0, hb.w(), y1 - y0), HUE_STOPS[i], HUE_STOPS[i + 1]);
        }
        r.border(hb, theme.border(), 1);

        // Markers.
        int mx = sv.x() + Math.round(sat * (sv.w() - 1));
        int my = sv.y() + Math.round((1f - val) * (sv.h() - 1));
        r.border(new Rect(mx - 2, my - 2, 5, 5), 0xFFFFFFFF, 1);
        int hy = hb.y() + Math.round(hue * (hb.h() - 2));
        r.fill(new Rect(hb.x() - 1, hy, hb.w() + 2, 2), 0xFFFFFFFF);

        // Swatch + hex readout.
        int by = bounds.bottom() - 12;
        Rect swatch = new Rect(bounds.x(), by, 12, 12);
        r.fill(swatch, 0xFF000000 | rgb.get());
        r.border(swatch, theme.border(), 1);
        r.text(String.format("#%06X", rgb.get() & 0xFFFFFF),
                bounds.x() + 16, by + (12 - r.height()) / 2, theme.textDim());
    }

    // ---- HSV math (package-visible for tests) ----

    static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = (((g - b) / d % 6) + 6) % 6 / 6f;
        else if (max == g) h = ((b - r) / d + 2) / 6f;
        else h = ((r - g) / d + 4) / 6f;
        float s = max == 0 ? 0 : d / max;
        return new float[]{h, s, max};
    }

    static int hsvToRgb(float h, float s, float v) {
        int i = (int) Math.floor(h * 6) % 6;
        if (i < 0) i += 6;
        float f = h * 6 - (float) Math.floor(h * 6);
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }
}
