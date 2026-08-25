package net.ethrocky.pane;

//? if fabric {
import com.mojang.blaze3d.platform.InputConstants;
import net.ethrocky.pane.gallery.Gallery;
import net.ethrocky.pane.runtime.PaneOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}
//? if >=1.21.9 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?}
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class PaneClient implements ClientModInitializer {
    public static KeyMapping TOGGLE_KEY;

    @Override
    public void onInitializeClient() {
        PaneBootstrap.initClient();

        KeyMapping toggle = new KeyMapping(
                "key.pane.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                PaneBootstrap.toggleCategory()
        );
        //? if >=26.1 {
        TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(toggle);
        //?} else {
        /*TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(toggle);
        *///?}

        boolean dev = FabricLoader.getInstance().isDevelopmentEnvironment();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                if (dev) Gallery.toggle();
                else PaneOverlay.toggleCursor();
            }
        });

        //? if >=1.21.9 {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("pane", "overlay"),
                (ctx, tickCounter) -> PaneOverlay.render(ctx));
        //?} else {
        /*HudRenderCallback.EVENT.register((ctx, tickCounter) -> PaneOverlay.render(ctx));
        *///?}
    }
}
//?}
