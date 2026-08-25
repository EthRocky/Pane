package net.ethrocky.pane.gallery;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.Toasts;
import net.ethrocky.pane.core.layout.Flex;
import net.ethrocky.pane.core.style.Style;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.markup.PaneMarkup;
import net.ethrocky.pane.markup.PmlContext;
import net.ethrocky.pane.runtime.PaneOverlay;
import net.minecraft.client.Minecraft;
import net.ethrocky.pane.widget.Button;
import net.ethrocky.pane.widget.Checkbox;
import net.ethrocky.pane.widget.ColorPicker;
import net.ethrocky.pane.widget.Dropdown;
import net.ethrocky.pane.widget.Label;
import net.ethrocky.pane.widget.ListView;
import net.ethrocky.pane.widget.Panel;
import net.ethrocky.pane.widget.ProgressBar;
import net.ethrocky.pane.widget.ScrollContainer;
import net.ethrocky.pane.widget.Slider;
import net.ethrocky.pane.widget.Tabs;
import net.ethrocky.pane.widget.TextInput;
import net.ethrocky.pane.widget.Window;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Dev-only window exercising every widget — the living acceptance test.
 *  Hidden until the toggle key shows it; showing unlocks the cursor, hiding
 *  (key or close button) relocks. */
public final class Gallery {
    private static Window window;

    private Gallery() {
    }

    public static void toggle() {
        if (window == null) window = build();
        if (!PaneOverlay.hasWindow(window)) {
            PaneOverlay.addWindow(window);
            if (!PaneOverlay.cursorActive()) PaneOverlay.toggleCursor();
        } else if (!PaneOverlay.cursorActive()) {
            // Window is up but the game took the cursor (clicked the world) — reclaim
            // the mouse instead of hiding.
            PaneOverlay.toggleCursor();
        } else {
            hide();
        }
    }

    private static void hide() {
        PaneOverlay.removeWindow(window);
        if (PaneOverlay.cursorActive()) PaneOverlay.toggleCursor();
    }

    private static Window build() {
        Window win = Window.of("Pane Gallery").at(30, 30);
        win.closable(Gallery::hide);
        win.add(Tabs.of()
                .tab("Basics", basics())
                .tab("Lists", lists())
                .tab("Color", color())
                .tab("Markup", markup()));
        return win;
    }

    private static Component basics() {
        State<String> message = State.of("Hello from Pane");
        State<Boolean> enabled = State.of(true);
        State<Float> value = State.of(0.5f);
        State<String> readout = State.of(format(value.get()));
        value.onChange(v -> readout.set(format(v)));
        int[] clicks = {0};

        State<String> themeName = State.of("Studio");
        themeName.onChange(n ->
                PaneOverlay.setTheme("Vanilla".equals(n) ? Theme.VANILLA : Theme.STUDIO));

        Panel p = Panel.column();
        p.style(Style.empty().padding(0));
        p.add(Label.of(message));
        p.add(TextInput.of(message));           // typing live-updates the label above
        p.add(Button.of("Click me")
                .onClick(() -> message.set("Clicked ×" + ++clicks[0]))
                .tooltip("Rewrites the label"));
        p.add(Dropdown.of(themeName, List.of("Studio", "Vanilla")));
        p.add(Checkbox.of("Enabled", enabled).tooltip("Hover works on any widget"));
        p.add(Slider.of(value, 0f, 1f));
        p.add(Label.of(readout).dim(true));
        return p;
    }

    private static Component lists() {
        State<String> pick = State.of("");
        State<String> pickLabel = State.of("Selected: nothing yet");
        pick.onChange(v -> pickLabel.set("Selected: " + v));

        List<String> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) items.add("Item " + i);

        ScrollContainer scroll = ScrollContainer.of(90);
        scroll.align(Flex.Align.STRETCH);
        scroll.add(ListView.of(pick, items));

        Panel p = Panel.column();
        p.style(Style.empty().padding(0));
        p.add(scroll);
        p.add(Label.of(pickLabel).dim(true));
        return p;
    }

    private static Component color() {
        State<Integer> rgb = State.of(0x4FA3FF);
        State<Float> progress = State.of(0.3f);

        Panel p = Panel.column();
        p.style(Style.empty().padding(0));
        p.add(ColorPicker.of(rgb));
        p.add(Slider.of(progress, 0f, 1f));
        p.add(ProgressBar.of(progress));
        p.add(Button.of("Show toast").onClick(() -> Toasts.show("Hello from a toast!")));
        return p;
    }

    private static String format(float v) {
        return String.format("%.2f", v);
    }

    // ---- markup demo: a second window built from watched run-dir files ----

    private static AutoCloseable markupWatch;
    private static Window markupWindow;

    private static Component markup() {
        State<String> status = State.of("Files: pane-dev.pml / .pss");
        Panel p = Panel.column();
        p.style(Style.empty().padding(0));
        p.add(Label.of("A window built from files in the"));
        p.add(Label.of("run dir, hot-reloading on save."));
        p.add(Label.of(status).dim(true));
        p.add(Button.of("Open markup window").onClick(() -> toggleMarkup(status)));
        return p;
    }

    private static void toggleMarkup(State<String> status) {
        if (markupWatch != null) {
            closeMarkup();
            status.set("Closed.");
            return;
        }
        try {
            Path pml = Path.of("pane-dev.pml");
            Path pss = Path.of("pane-dev.pss");
            if (!Files.exists(pml)) Files.writeString(pml, STARTER_PML);
            if (!Files.exists(pss)) Files.writeString(pss, STARTER_PSS);

            State<String> greeting = State.of("Hello, markup!");
            State<Float> amount = State.of(0.4f);
            PmlContext ctx = new PmlContext()
                    .state("greeting", greeting)
                    .state("amount", amount)
                    .action("toast", () -> Toasts.show(greeting.get()));

            markupWatch = PaneMarkup.watch(pml, pss, ctx,
                    fresh -> Minecraft.getInstance().execute(() -> swapMarkupWindow(fresh)),
                    err -> Toasts.show("Markup error: " + firstLine(err.getMessage())));
            status.set("Watching pane-dev.pml");
        } catch (Exception e) {
            Toasts.show("Markup error: " + firstLine(e.getMessage()));
        }
    }

    private static void swapMarkupWindow(Window fresh) {
        if (markupWindow != null) {
            fresh.at(markupWindow.posX(), markupWindow.posY());
            PaneOverlay.removeWindow(markupWindow);
        }
        markupWindow = fresh;
        fresh.closable(Gallery::closeMarkup);
        PaneOverlay.addWindow(fresh);
    }

    private static void closeMarkup() {
        if (markupWatch != null) {
            try {
                markupWatch.close();
            } catch (Exception ignored) {
            }
            markupWatch = null;
        }
        if (markupWindow != null) {
            PaneOverlay.removeWindow(markupWindow);
            markupWindow = null;
        }
    }

    private static String firstLine(String message) {
        if (message == null || message.isBlank()) return "see log";
        int nl = message.indexOf('\n');
        return nl < 0 ? message : message.substring(0, nl);
    }

    private static final String STARTER_PML = """
            <window title="Markup Demo" x="270" y="30" width="200">
              <label text="Edit pane-dev.pml + .pss"/>
              <label bind="greeting" class="dim"/>
              <textinput bind="greeting"/>
              <button id="cta" text="Toast it" onclick="toast"/>
              <slider bind="amount"/>
              <progress bind="amount"/>
            </window>
            """;

    private static final String STARTER_PSS = """
            /* Pane stylesheet — save this file and watch the window restyle. */
            window { padding: 8; }
            #cta { width: 100; }
            .dim { fg: #9A9AA5; }
            """;
}
