package net.ethrocky.pane.core.style;

/** Nullable-field property bag. A null field means "inherit" — resolution walks
 *  override → base → theme default, so a Style only ever states what it changes.
 *  Colors are ARGB ints. */
public final class Style {
    public Integer bg;
    public Integer fg;
    public Integer borderColor;
    public Integer borderWidth;
    public Integer padding;
    public Integer gap;
    public Integer width;
    public Integer height;
    public Integer grow;

    private Style() {
    }

    public static Style empty() {
        return new Style();
    }

    public Style bg(int argb)          { this.bg = argb; return this; }
    public Style fg(int argb)          { this.fg = argb; return this; }
    public Style borderColor(int argb) { this.borderColor = argb; return this; }
    public Style borderWidth(int px)   { this.borderWidth = px; return this; }
    public Style padding(int px)       { this.padding = px; return this; }
    public Style gap(int px)           { this.gap = px; return this; }
    public Style width(int px)         { this.width = px; return this; }
    public Style height(int px)        { this.height = px; return this; }
    public Style grow(int weight)      { this.grow = weight; return this; }

    /** New Style where this wins wherever it has a value, falling back to base. */
    public Style mergedOver(Style base) {
        Style out = new Style();
        out.bg          = bg          != null ? bg          : base.bg;
        out.fg          = fg          != null ? fg          : base.fg;
        out.borderColor = borderColor != null ? borderColor : base.borderColor;
        out.borderWidth = borderWidth != null ? borderWidth : base.borderWidth;
        out.padding     = padding     != null ? padding     : base.padding;
        out.gap         = gap         != null ? gap         : base.gap;
        out.width       = width       != null ? width       : base.width;
        out.height      = height      != null ? height      : base.height;
        out.grow        = grow        != null ? grow        : base.grow;
        return out;
    }
}
