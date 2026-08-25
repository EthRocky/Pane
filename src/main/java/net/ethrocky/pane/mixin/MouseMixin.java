package net.ethrocky.pane.mixin;

import net.ethrocky.pane.runtime.PaneOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonInfo;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the overlay owns the mouse: clicks must not reach game keybinds and must
 * not re-grab the cursor mid-interaction. Clicks on the world (no window hovered)
 * behave exactly as vanilla. A vanilla Screen being open bypasses all of this.
 */
@Mixin(MouseHandler.class)
public class MouseMixin {

    /** Over a window the re-grab is refused; anywhere else it proceeds AND the overlay
     *  is told, so its cursor state tracks the real cursor (clicking the world hands
     *  the mouse back to the game without desyncing the toggle key). */
    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void pane$trackGrab(CallbackInfo ci) {
        // Only guard OUR unlock: an externally freed cursor (Flashback) may re-grab freely.
        if (PaneOverlay.cursorActive() && PaneOverlay.wantsMouse()) {
            ci.cancel();
            return;
        }
        PaneOverlay.onCursorLocked();
    }

    /** Presses over a window are swallowed before keybinds (or an open Screen) see
     *  them; releases always pass so a held key can never get stuck. The overlay
     *  receives every transition while it could own the mouse — a release belongs to
     *  it even when the drag ended off-window. */
    //? if >=1.21.9 {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void pane$captureButtons(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() != null) {
            if (PaneOverlay.wantsMouseOnScreen()) {
                PaneOverlay.mouseButton(info.button(), info.modifiers(), action);
                if (action != 0) ci.cancel();
            }
            return;
        }
        if (!PaneOverlay.pointerActive()) return;
        boolean owned = PaneOverlay.wantsMouse();
        PaneOverlay.mouseButton(info.button(), info.modifiers(), action);
        if (action != 0 && owned) {
            ci.cancel();
            return;
        }
        // Not on a window: a consumer (in-world gizmo) may still claim it. Unclaimed clicks fall
        // through to vanilla, so clicking empty space regrabs the cursor as it always has.
        if (!owned && PaneOverlay.handleWorldClick(info.button(), action)) ci.cancel();
    }
    //?} else {
    /*@Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void pane$captureButtons(long window, int button, int action, int mods, CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() != null) {
            if (PaneOverlay.wantsMouseOnScreen()) {
                PaneOverlay.mouseButton(button, mods, action);
                if (action != 0) ci.cancel();
            }
            return;
        }
        if (!PaneOverlay.pointerActive()) return;
        boolean owned = PaneOverlay.wantsMouse();
        PaneOverlay.mouseButton(button, mods, action);
        if (action != 0 && owned) {
            ci.cancel();
            return;
        }
        // Not on a window: a consumer (in-world gizmo) may still claim it. Unclaimed clicks fall
        // through to vanilla, so clicking empty space regrabs the cursor as it always has.
        if (!owned && PaneOverlay.handleWorldClick(button, action)) ci.cancel();
    }
    *///?}

    /** Scrolling a window must not switch the hotbar slot (or scroll the chat). */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void pane$captureScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        boolean owned = Minecraft.getInstance().gui.screen() == null
                ? PaneOverlay.wantsMouse()
                : PaneOverlay.wantsMouseOnScreen();
        if (owned) {
            PaneOverlay.mouseScroll(vertical);
            ci.cancel();
        }
    }
}
