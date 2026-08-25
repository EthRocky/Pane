package net.ethrocky.pane.markup;

import net.ethrocky.pane.core.State;

import java.util.HashMap;
import java.util.Map;

/** Behavior stays in Java: markup references these states and actions by name.
 *  This is the whole scripting story for v1 — no embedded language. */
public final class PmlContext {
    private final Map<String, State<?>> states = new HashMap<>();
    private final Map<String, Runnable> actions = new HashMap<>();

    public PmlContext state(String name, State<?> state) {
        states.put(name, state);
        return this;
    }

    public PmlContext action(String name, Runnable action) {
        actions.put(name, action);
        return this;
    }

    @SuppressWarnings("unchecked")
    <T> State<T> state(String name, Class<T> type, String element) {
        State<?> s = states.get(name);
        if (s == null) {
            throw new PmlException("<" + element + "> binds '" + name + "' but the context has no such state");
        }
        Object v = s.get();
        if (v != null && !type.isInstance(v)) {
            throw new PmlException("<" + element + "> binds '" + name + "' expecting " + type.getSimpleName()
                    + " but it holds " + v.getClass().getSimpleName());
        }
        return (State<T>) s;
    }

    Runnable action(String name, String element) {
        Runnable a = actions.get(name);
        if (a == null) {
            throw new PmlException("<" + element + "> references action '" + name + "' but the context has no such action");
        }
        return a;
    }
}
