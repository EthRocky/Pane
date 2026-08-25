package net.ethrocky.pane.core.style;

/** Token set every widget paints from. Swapping the theme restyles the whole tree. */
public record Theme(int surface, int surfaceRaised, int text, int textDim,
                    int accent, int border, int padding, int gap) {

    /** Dark tool aesthetic: the default for editor-style overlays. */
    public static final Theme STUDIO = new Theme(
            0xE61E1F24, 0xF2292A31, 0xFFE8E8EC, 0xFF9A9AA5,
            0xFF4FA3FF, 0xFF3A3B44, 6, 4);

    /** Matches vanilla Minecraft screens for mods that want to blend in. */
    public static final Theme VANILLA = new Theme(
            0xE8303030, 0xF2404040, 0xFFFFFFFF, 0xFFAAAAAA,
            0xFFFFFF55, 0xFF8B8B8B, 6, 4);

    /** Light companion to STUDIO — same geometry, inverted values — so a mod can
     *  ship a dark/light pair by swapping one token set. */
    public static final Theme PAPER = new Theme(
            0xF2F1F2F5, 0xFAFFFFFF, 0xFF1D1E24, 0xFF70717C,
            0xFF2B6FD9, 0xFFC8C9D2, 6, 4);
}
