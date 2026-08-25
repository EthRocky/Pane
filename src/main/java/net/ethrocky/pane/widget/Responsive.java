package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.layout.Flex;
import net.ethrocky.pane.core.style.Style;

import java.util.ArrayList;
import java.util.List;

/** Media-query for component trees: shows the variant whose min-width threshold
 *  best fits the width this component is actually given — breakpoints are the
 *  consumer's numbers, like CSS. Variants are separate subtrees; bind them to the
 *  same State objects and they stay in sync while only one is alive on screen. */
public class Responsive extends Component {
    private final List<Integer> thresholds = new ArrayList<>();
    private final List<Component> variants = new ArrayList<>();
    private int active = -1;

    private Responsive() {
        style(Style.empty().padding(0));
        align(Flex.Align.STRETCH);
    }

    public static Responsive of() {
        return new Responsive();
    }

    /** Variant used when the available width is at least minWidth. Register in
     *  ascending order; the first should use 0 as its threshold. */
    public Responsive when(int minWidth, Component variant) {
        thresholds.add(minWidth);
        variants.add(variant);
        if (active < 0) activate(0);
        return this;
    }

    public int activeIndex() {
        return active;
    }

    private void activate(int index) {
        if (index == active) return;
        if (active >= 0) children.remove(variants.get(active));
        active = index;
        add(variants.get(index));
        invalidate();
    }

    private int pick(int width) {
        int best = 0;
        for (int i = 0; i < thresholds.size(); i++) {
            if (width >= thresholds.get(i)) best = i;
        }
        return best;
    }

    @Override
    public void layout(Rect b, TextMeasurer tm) {
        if (!variants.isEmpty()) activate(pick(b.w()));
        super.layout(b, tm);
    }
}
