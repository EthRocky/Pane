package net.ethrocky.pane.core;

public record Size(int w, int h) {
    public static final Size ZERO = new Size(0, 0);
}
