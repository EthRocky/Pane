package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;

/** Typed numeric entry bound to a State&lt;Float&gt;, with drag-to-scrub and
 *  scroll-to-nudge like a drag-float:
 *  - clean click focuses for typing (Enter commits, bad input reverts)
 *  - press and drag horizontally to scrub the value (dragStep per pixel)
 *  - mouse wheel nudges by scrollStep while unfocused
 *  Shows the live value while unfocused, so a paired Slider updates it in real
 *  time. Typed values are unbounded unless a range is given — they may exceed a
 *  paired slider's drag range (e.g. negative brightness). */
public class NumberField extends TextInput {
    private static final double DRAG_THRESHOLD = 3;

    private final State<String> buffer;
    private final State<Float> value;
    private final Float min, max;
    private float dragStep, scrollStep;
    private boolean pressed, scrubbing;
    private double pressX;
    private float pressValue;

    private NumberField(State<String> buffer, State<Float> value, Float min, Float max) {
        super(buffer);
        this.buffer = buffer;
        this.value = value;
        this.min = min;
        this.max = max;
        this.dragStep = min != null && max != null ? (max - min) / 150f : 0.05f;
        this.scrollStep = dragStep * 5f;
        tag("numberfield");
        value.onChange(v -> {
            if (!focused()) buffer.set(fmt(v));
        });
        onSubmit(this::commitTyped);
    }

    /** Unbounded field: typed/scrubbed values can go anywhere (negative, huge).
     *  Named differently from of() because TextInput.of(State&lt;String&gt;) is
     *  inherited and Java forbids the same-erasure overload. */
    public static NumberField unbounded(State<Float> value) {
        return new NumberField(State.of(fmt(value.get())), value, null, null);
    }

    public static NumberField of(State<Float> value, float min, float max) {
        return new NumberField(State.of(fmt(value.get())), value, min, max);
    }

    /** Value change per horizontal drag pixel. Also rescales the scroll step. */
    public NumberField dragStep(float step) {
        this.dragStep = step;
        this.scrollStep = step * 5f;
        return this;
    }

    /** Value change per scroll tick. */
    public NumberField scrollStep(float step) {
        this.scrollStep = step;
        return this;
    }

    private void commitTyped() {
        try {
            value.set(clamp(Float.parseFloat(buffer.get().trim())));
            buffer.set(fmt(value.get()));   // canonical form even when value was already equal
        } catch (NumberFormatException e) {
            buffer.set(fmt(value.get()));
        }
    }

    private float clamp(float v) {
        if (min != null) v = Math.max(min, v);
        if (max != null) v = Math.min(max, v);
        return v;
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        switch (e.kind()) {
            case DOWN -> {
                if (e.inside(bounds)) {
                    pressed = true;
                    scrubbing = false;
                    pressX = e.x();
                    pressValue = value.get();
                    return true;
                }
            }
            case MOVE -> {
                if (pressed) {
                    if (!scrubbing && Math.abs(e.x() - pressX) > DRAG_THRESHOLD) scrubbing = true;
                    if (scrubbing && !focused()) {
                        value.set(clamp(pressValue + (float) (e.x() - pressX) * dragStep));
                    }
                    return true;
                }
            }
            case UP -> {
                if (pressed) {
                    pressed = false;
                    if (!scrubbing) takeFocus();    // clean click = edit by typing
                    scrubbing = false;
                    return true;
                }
            }
            case SCROLL -> {
                if (!focused() && e.inside(bounds)) {
                    value.set(clamp(value.get() + (float) e.scrollY() * scrollStep));
                    return true;
                }
            }
        }
        return false;
    }

    private static String fmt(float v) {
        return String.format("%.2f", v);
    }
}
