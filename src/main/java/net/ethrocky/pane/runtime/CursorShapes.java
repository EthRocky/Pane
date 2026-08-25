package net.ethrocky.pane.runtime;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Hardware cursor shapes via GLFW standard cursors — created lazily, always set from
 *  the render thread (which is GLFW's main thread). Shape 0 restores the default arrow.
 *  No callbacks are registered.
 *
 *  <p>A resize shape is RE-ASSERTED every frame while it's wanted, because Minecraft resets
 *  the OS cursor to the plain arrow on its own each frame while the pointer is unlocked. If we
 *  only set on change, MC's reset wins and the resize cursor flickers for a frame then vanishes.
 *  The default arrow is only re-set once, on the transition away from a resize shape, so we
 *  otherwise leave MC's cursor alone. */
final class CursorShapes {
    private static final Map<Integer, Long> CURSORS = new HashMap<>();
    private static int currentShape;

    private CursorShapes() {
    }

    static void set(long windowHandle, int shape) {
        if (shape != 0) {
            long cursor = resolve(shape);
            if (cursor != 0L) GLFW.glfwSetCursor(windowHandle, cursor);   // beat MC's per-frame reset
            currentShape = shape;
        } else if (currentShape != 0) {
            GLFW.glfwSetCursor(windowHandle, 0L);   // restore the arrow once, then hands off
            currentShape = 0;
        }
    }

    private static long resolve(int shape) {
        long cursor = CURSORS.computeIfAbsent(shape, GLFW::glfwCreateStandardCursor);
        if (cursor == 0L) {
            // Platform lacks this standard cursor (some Linux setups miss the
            // diagonals) — fall back to a plain resize arrow.
            cursor = CURSORS.computeIfAbsent(GLFW.GLFW_RESIZE_EW_CURSOR, GLFW::glfwCreateStandardCursor);
        }
        return cursor;
    }
}
