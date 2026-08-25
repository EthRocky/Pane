package net.ethrocky.pane;

//? if neoforge {
/*import com.mojang.blaze3d.platform.InputConstants;
import net.ethrocky.pane.runtime.PaneOverlay;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Client-only mod: the constructor never runs on a dedicated server.
@Mod(value = "pane", dist = Dist.CLIENT)
public final class PaneNeoForge {
    public static final Logger LOGGER = LoggerFactory.getLogger("pane");
    public static KeyMapping TOGGLE_KEY;

    public PaneNeoForge(IEventBus modBus) {
        LOGGER.info("Pane initialised.");
        PaneBootstrap.initClient();

        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            TOGGLE_KEY = new KeyMapping(
                    "key.pane.toggle",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_P,
                    PaneBootstrap.toggleCategory()
            );
            event.register(TOGGLE_KEY);
        });

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            while (TOGGLE_KEY != null && TOGGLE_KEY.consumeClick()) {
                if (PaneBootstrap.devEnvironment()) net.ethrocky.pane.gallery.Gallery.toggle();
                else PaneOverlay.toggleCursor();
            }
        });

        // Post fires after the whole HUD, so windows draw above it.
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) ->
                PaneOverlay.render(event.getGuiGraphics()));
    }
}
*///?}
