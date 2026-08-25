package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Modals;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.layout.Flex;
import net.ethrocky.pane.core.style.Style;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

/** Modal dialog: title, body text, and a row of buttons that each close the dialog
 *  before running their action. Opens on the modal layer — the host centers it,
 *  dims everything behind it, and gives it exclusive input; ESCAPE closes it
 *  (call {@code dismissable(false)} for dialogs that must be answered).
 *
 *  <pre>
 *  Dialog.confirm("Delete light?", "This cannot be undone.", () -> delete());
 *
 *  Dialog.of("Export")
 *        .body("Overwrite stage.json?")
 *        .button("Overwrite", () -> save(true))
 *        .button("Keep both", () -> save(false))
 *        .button("Cancel", null)
 *        .open();
 *  </pre> */
public class Dialog extends Component {
    private static final int MIN_WIDTH = 160;

    private final Panel buttons;
    private boolean dismissable = true;

    private Dialog(String title) {
        style(Style.empty().padding(10).gap(8));
        align(Flex.Align.STRETCH);
        add(Label.of(title));
        buttons = Panel.row();
        buttons.style(Style.empty().padding(0).gap(6));
        buttons.justify(Flex.Justify.END);
    }

    public static Dialog of(String title) {
        return new Dialog(title);
    }

    /** One-call yes/no. "Confirm" runs the action; "Cancel" just closes. */
    public static Dialog confirm(String title, String message, Runnable onConfirm) {
        return of(title).body(message).button("Confirm", onConfirm).button("Cancel", null).open();
    }

    /** Adds a line of body text (call repeatedly for multiple lines). */
    public Dialog body(String text) {
        add(Label.of(text).dim(true));
        return this;
    }

    /** Adds a custom component between the body and the buttons (inputs, etc.). */
    public Dialog content(Component c) {
        add(c);
        return this;
    }

    /** Adds a button. Every button closes the dialog first, then runs its action
     *  (null action = plain close, i.e. a Cancel button). */
    public Dialog button(String label, Runnable action) {
        buttons.add(Button.of(label).onClick(() -> {
            Modals.close();
            if (action != null) action.run();
        }));
        return this;
    }

    /** Off: ESCAPE no longer closes the dialog — a button must be pressed. Default on. */
    public Dialog dismissable(boolean value) {
        this.dismissable = value;
        return this;
    }

    public boolean isDismissable() {
        return dismissable;
    }

    /** Shows the dialog on the modal layer (replacing any previous modal). */
    public Dialog open() {
        if (childCount() > 0 && childAt(childCount() - 1) != buttons) add(buttons);
        Modals.open(this);
        return this;
    }

    public void close() {
        if (Modals.current() == this) Modals.close();
    }

    @Override
    public Size measure(TextMeasurer tm) {
        Size s = super.measure(tm);
        return s.w() >= MIN_WIDTH ? s : new Size(MIN_WIDTH, s.h());
    }

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        r.fill(bounds, theme.surfaceRaised());
        r.border(bounds, theme.border(), 1);
    }
}
