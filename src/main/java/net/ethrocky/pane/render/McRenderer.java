package net.ethrocky.pane.render;

import net.ethrocky.pane.core.Rect;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The one class that knows how Minecraft draws. Porting Pane to another game
 *  version means re-checking these five calls and nothing else. */
public final class McRenderer implements PaneRenderer {
    private final GuiGraphicsExtractor ctx;
    private final Font font;

    public McRenderer(GuiGraphicsExtractor ctx, Font font) {
        this.ctx = ctx;
        this.font = font;
    }

    @Override
    public void fill(Rect r, int argb) {
        ctx.fill(r.x(), r.y(), r.right(), r.bottom(), argb);
    }

    @Override
    public void border(Rect r, int argb, int width) {
        ctx.fill(r.x(), r.y(), r.right(), r.y() + width, argb);
        ctx.fill(r.x(), r.bottom() - width, r.right(), r.bottom(), argb);
        ctx.fill(r.x(), r.y() + width, r.x() + width, r.bottom() - width, argb);
        ctx.fill(r.right() - width, r.y() + width, r.right(), r.bottom() - width, argb);
    }

    @Override
    public void text(String s, int x, int y, int argb) {
        //? if >=26.1 {
        ctx.text(font, s, x, y, argb);
        //?} else {
        /*ctx.drawString(font, s, x, y, argb, false);
        *///?}
    }

    @Override
    public void gradientV(Rect r, int topArgb, int bottomArgb) {
        ctx.fillGradient(r.x(), r.y(), r.right(), r.bottom(), topArgb, bottomArgb);
    }

    @Override
    public void pushClip(Rect r) {
        ctx.enableScissor(r.x(), r.y(), r.right(), r.bottom());
    }

    @Override
    public void popClip() {
        ctx.disableScissor();
    }

    @Override
    public int width(String s) {
        return font.width(s);
    }

    @Override
    public int height() {
        return font.lineHeight;
    }
}
