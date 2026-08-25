package net.ethrocky.pane.runtime;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Focus;
import net.ethrocky.pane.core.Modals;
import net.ethrocky.pane.core.PaneKeys;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.render.McRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

/** Modal host: a vanilla Screen whose content is a Pane component tree. Input
 *  arrives through the normal Screen path, so no mixins are involved here. */
public class PaneScreen extends Screen {
    private final Component root;

    public PaneScreen(String title, Component root) {
        super(net.minecraft.network.chat.Component.literal(title));
        this.root = root;
    }

    private net.ethrocky.pane.core.Component.TextMeasurer measurer() {
        return new net.ethrocky.pane.core.Component.TextMeasurer() {
            @Override public int width(String s) { return font.width(s); }
            @Override public int height()        { return font.lineHeight; }
        };
    }

    private void relayout() {
        var tm = measurer();
        Size s = root.measure(tm);
        root.layout(new Rect((width - s.w()) / 2, (height - s.h()) / 2, s.w(), s.h()), tm);
    }

    @Override
    protected void init() {
        relayout();
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
    //?} else {
    /*@Override
    public void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
    *///?}
        if (root.consumeDirty()) relayout();
        McRenderer r = new McRenderer(ctx, font);
        root.paint(r, PaneOverlay.theme());
        Component modal = Modals.current();
        if (modal != null) {
            r.fill(new Rect(0, 0, width, height), 0x88000000);
            modal.consumeDirty();
            Size s = modal.measure(r);
            modal.layout(new Rect((width - s.w()) / 2, (height - s.h()) / 2, s.w(), s.h()), r);
            modal.paint(r, PaneOverlay.theme());
        }
    }

    @Override
    public void mouseMoved(double x, double y) {
        Component modal = Modals.current();
        if (modal != null) modal.onMouse(UiEvent.Mouse.move(x, y));
        root.onMouse(UiEvent.Mouse.move(x, y));
    }

    //? if <1.21.9 {
    /*private static int modifiers() {
        int m = 0;
        if (hasShiftDown()) m |= UiEvent.SHIFT;
        if (hasControlDown()) m |= UiEvent.CTRL;
        if (hasAltDown()) m |= UiEvent.ALT;
        return m;
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        Component focus = Focus.current();
        if (focus != null && !focus.bounds().contains(e.x(), e.y())) Focus.clear();
        Component modal = Modals.current();
        if (modal != null) {
            if (modal.bounds().contains(e.x(), e.y())) {
                modal.onMouse(UiEvent.Mouse.down(e.x(), e.y(), e.button(), e.modifiers()));
            }
            return true;   // outside clicks are swallowed while a modal is up
        }
        if (root.onMouse(UiEvent.Mouse.down(e.x(), e.y(), e.button(), e.modifiers()))) return true;
        return super.mouseClicked(e, doubled);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double x, double y, int button) {
        Component focus = Focus.current();
        if (focus != null && !focus.bounds().contains(x, y)) Focus.clear();
        Component modal = Modals.current();
        if (modal != null) {
            if (modal.bounds().contains(x, y)) {
                modal.onMouse(UiEvent.Mouse.down(x, y, button, modifiers()));
            }
            return true;   // outside clicks are swallowed while a modal is up
        }
        if (root.onMouse(UiEvent.Mouse.down(x, y, button, modifiers()))) return true;
        return super.mouseClicked(x, y, button);
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent e) {
        Component modal = Modals.current();
        Component focus = Focus.current();
        if (focus != null) {
            boolean consumed = focus.onKey(new UiEvent.Key(e.key(), e.modifiers()));
            if (!consumed && e.key() == PaneKeys.TAB) {
                Focus.traverse(focus.root(), (e.modifiers() & UiEvent.SHIFT) != 0);
            } else if (!consumed && e.key() == PaneKeys.ESCAPE && modal != null) {
                Modals.close();
            }
            return true;
        }
        if (modal != null) {
            if (e.key() == PaneKeys.ESCAPE) Modals.close();
            else if (e.key() == PaneKeys.TAB) Focus.traverse(modal, (e.modifiers() & UiEvent.SHIFT) != 0);
            else modal.onKey(new UiEvent.Key(e.key(), e.modifiers()));
            return true;   // a modal never lets ESC fall through and close the screen
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        Component focus = Focus.current();
        if (focus != null) {
            focus.onChar(new UiEvent.Char(e.codepoint()));
            return true;
        }
        return super.charTyped(e);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        Component modal = Modals.current();
        if (modal != null) {
            modal.onMouse(UiEvent.Mouse.up(e.x(), e.y(), e.button(), e.modifiers()));
            return true;
        }
        if (root.onMouse(UiEvent.Mouse.up(e.x(), e.y(), e.button(), e.modifiers()))) return true;
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (root.onMouse(UiEvent.Mouse.move(e.x(), e.y()))) return true;
        return super.mouseDragged(e, dx, dy);
    }
    //?} else {
    /*@Override
    public boolean keyPressed(int keyCode, int scancode, int mods) {
        Component modal = Modals.current();
        Component focus = Focus.current();
        if (focus != null) {
            boolean consumed = focus.onKey(new UiEvent.Key(keyCode, mods));
            if (!consumed && keyCode == PaneKeys.TAB) {
                Focus.traverse(focus.root(), (mods & UiEvent.SHIFT) != 0);
            } else if (!consumed && keyCode == PaneKeys.ESCAPE && modal != null) {
                Modals.close();
            }
            return true;
        }
        if (modal != null) {
            if (keyCode == PaneKeys.ESCAPE) Modals.close();
            else if (keyCode == PaneKeys.TAB) Focus.traverse(modal, (mods & UiEvent.SHIFT) != 0);
            else modal.onKey(new UiEvent.Key(keyCode, mods));
            return true;   // a modal never lets ESC fall through and close the screen
        }
        return super.keyPressed(keyCode, scancode, mods);
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        Component focus = Focus.current();
        if (focus != null) {
            focus.onChar(new UiEvent.Char(chr));
            return true;
        }
        return super.charTyped(chr, mods);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        Component modal = Modals.current();
        if (modal != null) {
            modal.onMouse(UiEvent.Mouse.up(x, y, button, modifiers()));
            return true;
        }
        if (root.onMouse(UiEvent.Mouse.up(x, y, button, modifiers()))) return true;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (root.onMouse(UiEvent.Mouse.move(x, y))) return true;
        return super.mouseDragged(x, y, button, dx, dy);
    }
    *///?}

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        Component modal = Modals.current();
        if (modal != null) {
            modal.onMouse(UiEvent.Mouse.scroll(x, y, vertical));
            return true;
        }
        if (root.onMouse(UiEvent.Mouse.scroll(x, y, vertical))) return true;
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
