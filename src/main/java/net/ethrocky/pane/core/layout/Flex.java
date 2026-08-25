package net.ethrocky.pane.core.layout;

/** Enums for the flexbox subset. Phase 1 has no wrap: one run per container. */
public final class Flex {
    private Flex() {
    }

    public enum Direction { ROW, COLUMN }

    public enum Justify { START, CENTER, END, SPACE_BETWEEN }

    public enum Align { START, CENTER, END, STRETCH }
}
