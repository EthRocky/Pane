package net.ethrocky.pane.mixin;

import net.ethrocky.pane.runtime.PaneOverlay;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a Pane widget holds keyboard focus, keys belong to it: typing in a field
 * must not move the player, and ESC unfocuses instead of opening the pause menu
 * (the widget clears focus on ESC, so the next ESC reaches the game normally).
 */
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    //? if >=1.21.9 {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void pane$captureKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (PaneOverlay.wantsKeyboard() && Minecraft.getInstance().gui.screen() == null) {
            PaneOverlay.keyPress(event.key(), event.modifiers(), action);
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void pane$captureChar(long window, CharacterEvent event, CallbackInfo ci) {
        if (PaneOverlay.wantsKeyboard() && Minecraft.getInstance().gui.screen() == null) {
            PaneOverlay.charTyped(event.codepoint());
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void pane$captureKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (PaneOverlay.wantsKeyboard() && Minecraft.getInstance().gui.screen() == null) {
            PaneOverlay.keyPress(key, modifiers, action);
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void pane$captureChar(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (PaneOverlay.wantsKeyboard() && Minecraft.getInstance().gui.screen() == null) {
            PaneOverlay.charTyped(codePoint);
            ci.cancel();
        }
    }
    *///?}
}
