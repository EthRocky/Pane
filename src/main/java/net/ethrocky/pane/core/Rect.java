package net.ethrocky.pane.core;

/** Integer rectangle in GUI-scaled coordinates. Half-open: contains() includes the left/top
 *  edges and excludes the right/bottom ones, so adjacent rects never both claim a point. */
public record Rect(int x, int y, int w, int h) {

    public boolean contains(double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public int right()  { return x + w; }
    public int bottom() { return y + h; }

    /** This rect shrunk by {@code amount} on every side (never inverting). */
    public Rect inset(int amount) {
        int nw = Math.max(0, w - amount * 2);
        int nh = Math.max(0, h - amount * 2);
        return new Rect(x + amount, y + amount, nw, nh);
    }
}
