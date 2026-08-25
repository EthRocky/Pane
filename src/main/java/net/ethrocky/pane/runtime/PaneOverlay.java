package net.ethrocky.pane.runtime;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Focus;
import net.ethrocky.pane.core.Modals;
import net.ethrocky.pane.core.PaneKeys;
import net.ethrocky.pane.core.Popups;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.Toasts;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.McRenderer;
import net.ethrocky.pane.widget.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Non-modal host: windows render as a HUD layer while the game keeps running.
 *  The input mixins ask this class who owns the mouse; everything here is
 *  client-thread only. */
public final class PaneOverlay {
    private static final List<Window> windows = new CopyOnWriteArrayList<>();
    private static Theme theme = Theme.STUDIO;
    private static boolean cursorActive;
    private static boolean interactThroughScreens = true;
    private static boolean clampToScreen = true;
    private static boolean cursorShapes = true;
    private static int pressedButtons;
    private static double mouseX, mouseY;

    private PaneOverlay() {
    }

    public static void addWindow(Window w) {
        if (!windows.contains(w)) windows.add(w);
    }

    public static void removeWindow(Window w) {
        windows.remove(w);
        if (Focus.current() != null && Focus.current().isDescendantOf(w)) Focus.clear();
        Popups.close();
    }

    public static boolean hasWindow(Window w) {
        return windows.contains(w);
    }

    public static Theme theme() {
        return theme;
    }

    public static void setTheme(Theme t) {
        theme = t;
    }

    /** Unlocks the game cursor for overlay interaction, or relocks it. The flag flips
     *  before grabMouse so our own mixin doesn't cancel the relock. */
    public static void toggleCursor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null || mc.level == null) return;
        if (cursorActive) {
            cursorActive = false;
            pressedButtons = 0;
            mc.mouseHandler.grabMouse();
        } else {
            cursorActive = true;
            mc.mouseHandler.releaseMouse();
        }
    }

    public static boolean cursorActive() {
        return cursorActive;
    }

    /** True while the pointer can interact with in-world windows: our own unlocked
     *  cursor, OR a cursor some other mod freed (Flashback's replay editor, Axiom).
     *  Pane windows must stay clickable there too; clicks off-window still fall
     *  through to whoever freed the cursor. */
    private static boolean interactWithExternalCursor = true;

    public static boolean pointerActive() {
        if (cursorActive) return true;
        if (!interactWithExternalCursor) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.gui.screen() == null && mc.mouseHandler != null && !mc.mouseHandler.isMouseGrabbed();
    }

    /** Windows stay interactive when another mod frees the cursor. Consumers whose
     *  windows should stay inert during external cursor sessions can turn this off. */
    public static void setInteractWithExternalCursor(boolean enabled) {
        interactWithExternalCursor = enabled;
    }

    /** Windows stay clickable while a vanilla Screen (chat, etc.) is open — that
     *  screen's cursor doubles as the overlay cursor. Consuming mods can turn this
     *  off if their windows should yield to screens entirely. */
    public static void setInteractThroughScreens(boolean enabled) {
        interactThroughScreens = enabled;
    }

    public static boolean interactsThroughScreens() {
        return interactThroughScreens;
    }

    /** Windows lay out no taller than the screen (their body scrolls instead).
     *  Turn off for windows that intentionally overflow. Default true. */
    public static void setClampToScreen(boolean enabled) {
        clampToScreen = enabled;
    }

    /** Hardware resize cursors when hovering a window edge. Default true. */
    public static void setCursorShapes(boolean enabled) {
        cursorShapes = enabled;
    }

    /** Claims clicks that land on the world rather than on a window, while the overlay cursor
     *  is active — for in-world handles like a gizmo. Returning true swallows the click, so it
     *  neither reaches gameplay nor lets the game relock the cursor; returning false leaves
     *  vanilla behaviour intact (a click on empty space still relocks and returns to play). */
    public interface WorldClickHandler {
        /** @param action 1 = press, 0 = release. */
        boolean onWorldClick(int button, int action);
    }

    private static WorldClickHandler worldClickHandler;

    public static void setWorldClickHandler(WorldClickHandler handler) {
        worldClickHandler = handler;
    }

    /** Called by the mouse mixin for clicks the overlay itself doesn't want. */
    public static boolean handleWorldClick(int button, int action) {
        return worldClickHandler != null && cursorActive && worldClickHandler.onWorldClick(button, action);
    }

    /** The game grabbed the cursor back (clicked the world, closed a screen). Called
     *  by the grab mixin so this state can never desync from the real cursor. */
    public static void onCursorLocked() {
        cursorActive = false;
        pressedButtons = 0;
    }

    private static boolean pointerOverWindow() {
        if (Modals.current() != null) return true;   // a modal owns the whole screen
        Component popup = Popups.current();
        if (popup != null && popup.bounds().contains(mouseX, mouseY)) return true;
        for (Window w : windows) {
            if (w.bounds().contains(mouseX, mouseY)) return true;
        }
        return false;
    }

    /** True while the overlay owns the mouse in-world: cursor unlocked AND (pointer
     *  over a window OR a press/drag that started on one is still live — capture must
     *  not drop mid-drag just because the cursor left the window). */
    public static boolean wantsMouse() {
        return pointerActive() && (pressedButtons > 0 || pointerOverWindow());
    }

    /** Same ownership test while a vanilla Screen is open. */
    public static boolean wantsMouseOnScreen() {
        return interactThroughScreens && (pressedButtons > 0 || pointerOverWindow());
    }

    public static boolean wantsKeyboard() {
        return Focus.current() != null || Modals.current() != null;
    }

    /** From KeyboardMixin. Raw ints so the overlay stays version-agnostic; the mixins
     *  extract these from whatever record their MC version wraps them in.
     *  action: 1 = press, 2 = repeat, 0 = release (ignored). */
    public static void keyPress(int key, int modifiers, int action) {
        if (action == 0) return;
        Component modal = Modals.current();
        Component focus = Focus.current();
        if (focus != null) {
            boolean consumed = focus.onKey(new UiEvent.Key(key, modifiers));
            if (!consumed && key == PaneKeys.TAB) {
                Focus.traverse(focus.root(), (modifiers & UiEvent.SHIFT) != 0);
            } else if (!consumed && key == PaneKeys.ESCAPE && modal != null && dismissable(modal)) {
                Modals.close();
            }
            return;
        }
        if (modal != null) {
            if (key == PaneKeys.ESCAPE && dismissable(modal)) {
                Modals.close();
            } else if (key == PaneKeys.TAB) {
                Focus.traverse(modal, (modifiers & UiEvent.SHIFT) != 0);
            } else {
                modal.onKey(new UiEvent.Key(key, modifiers));
            }
        }
    }

    private static boolean dismissable(Component modal) {
        return !(modal instanceof net.ethrocky.pane.widget.Dialog d) || d.isDismissable();
    }

    /** From KeyboardMixin. */
    public static void charTyped(int codepoint) {
        Component focus = Focus.current();
        if (focus == null) return;
        focus.onChar(new UiEvent.Char(codepoint));
    }

    /** From MouseMixin. action: 1 = press, 0 = release, 2 = repeat (ignored). */
    public static void mouseButton(int button, int modifiers, int action) {
        if (action == 2) return;
        Component modal = Modals.current();
        if (modal != null) {
            // Exclusive input: clicks inside route to the modal, outside are swallowed.
            if (action == 1) {
                Component focus = Focus.current();
                if (focus != null && !focus.bounds().contains(mouseX, mouseY)) Focus.clear();
                if (modal.bounds().contains(mouseX, mouseY)) {
                    if (modal.onMouse(UiEvent.Mouse.down(mouseX, mouseY, button, modifiers))) pressedButtons++;
                }
            } else {
                if (pressedButtons > 0) pressedButtons--;
                modal.onMouse(UiEvent.Mouse.up(mouseX, mouseY, button, modifiers));
            }
            return;
        }
        if (action == 1) {
            // Clicking away from the focus owner surrenders the keyboard.
            Component focus = Focus.current();
            if (focus != null && !focus.bounds().contains(mouseX, mouseY)) Focus.clear();
            UiEvent.Mouse e = UiEvent.Mouse.down(mouseX, mouseY, button, modifiers);
            Component popup = Popups.current();
            if (popup != null) {
                if (popup.bounds().contains(mouseX, mouseY)) {
                    if (popup.onMouse(e)) pressedButtons++;
                } else {
                    Popups.close();             // click-away closes, no click-through
                }
                return;
            }
            for (int i = windows.size() - 1; i >= 0; i--) {
                Window w = windows.get(i);
                if (w.onMouse(e)) {
                    pressedButtons++;
                    windows.remove(w);          // bring to front
                    windows.add(w);
                    return;
                }
            }
        } else {
            if (pressedButtons > 0) pressedButtons--;
            UiEvent.Mouse e = UiEvent.Mouse.up(mouseX, mouseY, button, modifiers);
            Component popup = Popups.current();
            if (popup != null && popup.onMouse(e)) return;
            for (int i = windows.size() - 1; i >= 0; i--) {
                if (windows.get(i).onMouse(e)) return;
            }
        }
    }

    /** From MouseMixin; only called while wantsMouse(). */
    public static void mouseScroll(double vertical) {
        UiEvent.Mouse e = UiEvent.Mouse.scroll(mouseX, mouseY, vertical);
        Component modal = Modals.current();
        if (modal != null) {
            modal.onMouse(e);   // exclusive — never scrolls what's behind the scrim
            return;
        }
        Component popup = Popups.current();
        if (popup != null && popup.onMouse(e)) return;
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).onMouse(e)) return;
        }
    }

    /** HUD layer: poll cursor, route hover, lay out dirty windows, paint bottom-up. */
    public static void render(GuiGraphicsExtractor ctx) {
        long now = System.currentTimeMillis();
        var toasts = Toasts.active(now);
        Minecraft mc = Minecraft.getInstance();
        var mcWin = mc.getWindow();
        //? if >=26.1 {
        mouseX = mc.mouseHandler.getScaledXPos(mcWin);
        mouseY = mc.mouseHandler.getScaledYPos(mcWin);
        //?} else {
        /*if (mcWin.getScreenWidth() > 0 && mcWin.getScreenHeight() > 0) {
            mouseX = mc.mouseHandler.xpos() * mcWin.getGuiScaledWidth() / (double) mcWin.getScreenWidth();
            mouseY = mc.mouseHandler.ypos() * mcWin.getGuiScaledHeight() / (double) mcWin.getScreenHeight();
        }
        *///?}
        updateCursorShape(mc);
        Component modal = Modals.current();
        if (windows.isEmpty() && Popups.current() == null && modal == null && toasts.isEmpty()) return;
        boolean pointerLive = mc.gui.screen() == null
                ? pointerActive()
                : interactThroughScreens;
        Component popup = Popups.current();
        if (pointerLive) {
            UiEvent.Mouse move = UiEvent.Mouse.move(mouseX, mouseY);
            if (modal != null) modal.onMouse(move);
            if (popup != null) popup.onMouse(move);
            for (Window w : windows) w.onMouse(move);
        }
        McRenderer r = new McRenderer(ctx, mc.font);
        for (Window w : windows) {
            if (w.consumeDirty()) {
                Size s = w.measure(r);
                int h = clampToScreen ? Math.min(s.h(), mcWin.getGuiScaledHeight() - 8) : s.h();
                w.layout(new Rect(w.posX(), w.posY(), s.w(), h), r);
            }
        }
        for (Window w : windows) w.paint(r, theme);
        if (popup != null) {
            if (popup.consumeDirty()) {
                Size s = popup.measure(r);
                // Clamp on-screen: a menu opened near an edge slides in, not off.
                int px = Math.max(0, Math.min(Popups.x(), mcWin.getGuiScaledWidth() - s.w()));
                int py = Math.max(0, Math.min(Popups.y(), mcWin.getGuiScaledHeight() - s.h()));
                popup.layout(new Rect(px, py, s.w(), s.h()), r);
            }
            popup.paint(r, theme);
        }
        if (modal != null) {
            // Scrim + centered modal, laid out every frame so screen resizes recenter it.
            Rect screen = new Rect(0, 0, mcWin.getGuiScaledWidth(), mcWin.getGuiScaledHeight());
            r.fill(screen, 0x88000000);
            modal.consumeDirty();
            Size s = modal.measure(r);
            modal.layout(new Rect((screen.w() - s.w()) / 2, (screen.h() - s.h()) / 2, s.w(), s.h()), r);
            modal.paint(r, theme);
        }
        if (pointerLive && popup == null && modal == null) {
            for (int i = windows.size() - 1; i >= 0; i--) {
                Component owner = windows.get(i).findTooltip(mouseX, mouseY);
                if (owner != null) {
                    paintTooltip(r, owner.tooltipText(), mcWin.getGuiScaledWidth(), mcWin.getGuiScaledHeight());
                    break;
                }
            }
        }
        paintToasts(r, toasts, now, mcWin.getGuiScaledWidth());
    }

    /** Resize cursor while hovering (or dragging) a window edge; arrow otherwise.
     *  A live resize keeps its shape even when the pointer outruns the edge zone. */
    private static void updateCursorShape(Minecraft mc) {
        int shape = 0;
        boolean pointerLive = mc.gui.screen() == null ? pointerActive() : interactThroughScreens;
        if (cursorShapes && pointerLive) {
            int edges = 0;
            for (int i = windows.size() - 1; i >= 0; i--) {
                Window w = windows.get(i);
                edges = w.activeResizeEdges();
                if (edges != 0) break;
                edges = w.resizeEdgesAt(mouseX, mouseY);
                if (edges != 0) break;
                if (w.bounds().contains(mouseX, mouseY)) break;   // topmost window decides
            }
            shape = shapeFor(edges);
        }
        CursorShapes.set(net.ethrocky.pane.PaneBootstrap.windowHandle(), shape);
    }

    private static int shapeFor(int edges) {
        boolean l = (edges & Window.LEFT) != 0, r = (edges & Window.RIGHT) != 0;
        boolean t = (edges & Window.TOP) != 0, b = (edges & Window.BOTTOM) != 0;
        if ((l && t) || (r && b)) return org.lwjgl.glfw.GLFW.GLFW_RESIZE_NWSE_CURSOR;
        if ((r && t) || (l && b)) return org.lwjgl.glfw.GLFW.GLFW_RESIZE_NESW_CURSOR;
        if (l || r) return org.lwjgl.glfw.GLFW.GLFW_RESIZE_EW_CURSOR;
        if (t || b) return org.lwjgl.glfw.GLFW.GLFW_RESIZE_NS_CURSOR;
        return 0;
    }

    private static void paintTooltip(McRenderer r, String text, int screenW, int screenH) {
        int w = r.width(text) + 8;
        int h = r.height() + 6;
        int x = (int) mouseX + 8;
        int y = (int) mouseY + 8;
        if (x + w > screenW) x = screenW - w;
        if (y + h > screenH) y = screenH - h;
        Rect box = new Rect(x, y, w, h);
        r.fill(box, theme.surfaceRaised());
        r.border(box, theme.border(), 1);
        r.text(text, x + 4, y + 3, theme.text());
    }

    private static void paintToasts(McRenderer r, java.util.List<Toasts.Toast> toasts, long now, int screenW) {
        int y = 8;
        for (Toasts.Toast t : toasts) {
            long age = now - t.shownAt();
            float alpha = age > Toasts.TTL_MS - 600 ? (Toasts.TTL_MS - age) / 600f : 1f;
            if (alpha < 0.05f) continue;
            int w = r.width(t.message()) + 12;
            int h = r.height() + 8;
            Rect box = new Rect(screenW - w - 8, y, w, h);
            r.fill(box, fade(theme.surfaceRaised(), alpha));
            r.border(box, fade(theme.accent(), alpha), 1);
            r.text(t.message(), box.x() + 6, box.y() + 4, fade(theme.text(), alpha));
            y += h + 4;
        }
    }

    private static int fade(int argb, float alpha) {
        int a = Math.round((argb >>> 24) * alpha);
        return (Math.max(a, 8) << 24) | (argb & 0xFFFFFF);
    }
}
