package net.ethrocky.pane.core;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Observable value holder. Widgets bind to a State and repaint when it changes;
 *  application code reads and writes it without knowing who is listening. */
public final class State<T> {
    private T value;
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    private State(T initial) {
        this.value = initial;
    }

    public static <T> State<T> of(T initial) {
        return new State<>(initial);
    }

    public T get() {
        return value;
    }

    /** Sets the value and notifies listeners. Equal values (by equals) are a no-op. */
    public void set(T newValue) {
        if (Objects.equals(value, newValue)) return;
        value = newValue;
        for (Consumer<T> l : listeners) l.accept(newValue);
    }

    /** Registers a listener; returns a Runnable that unsubscribes it. */
    public Runnable onChange(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
