package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.layout.Flex;

/** Bare flex container for free-form composition. */
public class Panel extends Component {
    private Panel() {
    }

    public static Panel row() {
        Panel p = new Panel();
        p.direction(Flex.Direction.ROW);
        return p;
    }

    public static Panel column() {
        return new Panel();
    }
}
