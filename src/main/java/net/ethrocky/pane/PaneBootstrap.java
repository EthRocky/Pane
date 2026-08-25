package net.ethrocky.pane;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Loader-agnostic client wiring shared by the Fabric and NeoForge entry points. */
public final class PaneBootstrap {

    private PaneBootstrap() {
    }

    /** The toggle keybind's category — even its type changed across versions. */
    //? if >=1.21.9 {
    public static KeyMapping.Category toggleCategory() {
        return KeyMapping.Category.MISC;
    }
    //?} else {
    /*public static String toggleCategory() {
        return "key.categories.pane";
    }
    *///?}

    /** OS clipboard + localized key names. Lazy lambdas: the window need not exist yet. */
    public static void initClient() {
        net.ethrocky.pane.core.PaneClipboard.install(
                () -> org.lwjgl.glfw.GLFW.glfwGetClipboardString(windowHandle()),
                s -> org.lwjgl.glfw.GLFW.glfwSetClipboardString(windowHandle(), s));
        net.ethrocky.pane.widget.KeybindField.setKeyNames(code -> code < 0 ? "None"
                : InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString());
    }

    /** True in a development run (gradle runClient) — gates the widget gallery. */
    public static boolean devEnvironment() {
        //? if fabric {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        //?} else {
        /*try {
            // Reflection dodges FMLLoader's static-to-instance rework across versions.
            Class<?> c = Class.forName("net.neoforged.fml.loading.FMLLoader");
            return !((Boolean) c.getMethod("isProduction").invoke(null));
        } catch (Throwable t) {
            return false;   // unknown loader era: behave like production
        }
        *///?}
    }

    /** The GLFW window pointer — the accessor was renamed in the 1.21.9 input rework. */
    public static long windowHandle() {
        //? if >=1.21.9 {
        return Minecraft.getInstance().getWindow().handle();
        //?} else {
        /*return Minecraft.getInstance().getWindow().getWindow();
        *///?}
    }
}
