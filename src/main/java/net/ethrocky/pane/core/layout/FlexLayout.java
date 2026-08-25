package net.ethrocky.pane.core.layout;

import net.ethrocky.pane.core.Rect;

import java.util.ArrayList;
import java.util.List;

/** Single-run flexbox subset. Sizes are resolved by the caller (measure pass);
 *  this only positions them and distributes free space to growers. */
public final class FlexLayout {
    private FlexLayout() {
    }

    /** One child as the layout sees it. mainSize/crossSize are along/across
     *  {@code dir}; grow 0 means fixed. */
    public record Item(int mainSize, int crossSize, int grow) {
        public Item {
            if (mainSize < 0 || crossSize < 0 || grow < 0)
                throw new IllegalArgumentException("negative size or grow");
        }
    }

    public static List<Rect> layout(Rect inner, Flex.Direction dir, Flex.Justify justify,
                                    Flex.Align align, int gap, List<Item> items) {
        int n = items.size();
        List<Rect> out = new ArrayList<>(n);
        if (n == 0) return out;

        boolean row = dir == Flex.Direction.ROW;
        int mainExtent = row ? inner.w() : inner.h();
        int crossExtent = row ? inner.h() : inner.w();

        int sumMain = 0, sumGrow = 0;
        for (Item it : items) {
            sumMain += it.mainSize();
            sumGrow += it.grow();
        }
        int free = mainExtent - sumMain - gap * (n - 1);

        // Growers split positive free space proportionally; integer division
        // truncates, so the last grower absorbs the remainder to fill exactly.
        int[] extra = new int[n];
        if (free > 0 && sumGrow > 0) {
            int distributed = 0, lastGrower = -1;
            for (int i = 0; i < n; i++) {
                if (items.get(i).grow() > 0) lastGrower = i;
            }
            for (int i = 0; i < n; i++) {
                int g = items.get(i).grow();
                if (g > 0) {
                    extra[i] = free * g / sumGrow;
                    distributed += extra[i];
                }
            }
            extra[lastGrower] += free - distributed;
        }

        // Justify only positions a packed run — with growers the space is consumed above.
        int offset = 0, interGap = 0;
        if (sumGrow == 0 && free > 0) {
            switch (justify) {
                case START -> offset = 0;
                case CENTER -> offset = free / 2;
                case END -> offset = free;
                case SPACE_BETWEEN -> interGap = n > 1 ? free / (n - 1) : 0;
            }
        }

        int pos = (row ? inner.x() : inner.y()) + offset;
        for (int i = 0; i < n; i++) {
            Item it = items.get(i);
            int mainSize = it.mainSize() + extra[i];
            int crossSize = align == Flex.Align.STRETCH ? crossExtent : it.crossSize();
            int crossOff = switch (align) {
                case START, STRETCH -> 0;
                case CENTER -> (crossExtent - crossSize) / 2;
                case END -> crossExtent - crossSize;
            };
            out.add(row
                    ? new Rect(pos, inner.y() + crossOff, mainSize, crossSize)
                    : new Rect(inner.x() + crossOff, pos, crossSize, mainSize));
            pos += mainSize + gap + interGap;
        }
        return out;
    }
}
