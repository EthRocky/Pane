package net.ethrocky.pane.markup;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.style.Style;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** CSS-subset stylesheet: `selector[, selector] { prop: value; ... }` with block
 *  comments. Selectors combine [type][#id][.class]; specificity id > class > type,
 *  later rules win ties; the sheet always merges UNDER a component's inline style. */
public final class Pss {
    private Pss() {
    }

    public record Selector(String type, String id, String cssClass) {
        public int specificity() {
            return (id != null ? 100 : 0) + (cssClass != null ? 10 : 0) + (type != null ? 1 : 0);
        }

        public boolean matches(Component c) {
            if (type != null && !type.equals(c.tag())) return false;
            if (id != null && !id.equals(c.id())) return false;
            return cssClass == null || c.cssClasses().contains(cssClass);
        }

        static Selector parse(String raw) {
            if (raw.isEmpty()) throw new PssException("Empty selector");
            String type = null, id = null, cls = null;
            char mode = 't';
            StringBuilder cur = new StringBuilder();
            for (int i = 0; i <= raw.length(); i++) {
                char ch = i < raw.length() ? raw.charAt(i) : '\0';
                if (ch == '#' || ch == '.' || ch == '\0') {
                    String part = cur.toString().trim();
                    if (!part.isEmpty()) {
                        switch (mode) {
                            case 't' -> type = part;
                            case 'i' -> id = part;
                            default -> cls = part;
                        }
                    }
                    cur.setLength(0);
                    mode = ch == '#' ? 'i' : 'c';
                } else {
                    cur.append(ch);
                }
            }
            if (type == null && id == null && cls == null) throw new PssException("Empty selector: " + raw);
            return new Selector(type, id, cls);
        }
    }

    public record Rule(Selector selector, Style style, int order) {
    }

    public static List<Rule> parse(String source) {
        String stripped = source.replaceAll("(?s)/\\*.*?\\*/", "");
        List<Rule> rules = new ArrayList<>();
        int order = 0;
        for (String block : stripped.split("}")) {
            if (block.trim().isEmpty()) continue;
            int brace = block.indexOf('{');
            if (brace < 0) throw new PssException("Expected '{' in: " + block.trim());
            String selectorPart = block.substring(0, brace).trim();
            Style style = parseBody(block.substring(brace + 1));
            for (String sel : selectorPart.split(",")) {
                rules.add(new Rule(Selector.parse(sel.trim()), style, order++));
            }
        }
        return rules;
    }

    private static Style parseBody(String body) {
        Style st = Style.empty();
        for (String decl : body.split(";")) {
            decl = decl.trim();
            if (decl.isEmpty()) continue;
            int colon = decl.indexOf(':');
            if (colon < 0) throw new PssException("Bad declaration: " + decl);
            String prop = decl.substring(0, colon).trim().toLowerCase();
            String val = decl.substring(colon + 1).trim();
            switch (prop) {
                case "bg" -> st.bg(color(val));
                case "fg" -> st.fg(color(val));
                case "border-color" -> st.borderColor(color(val));
                case "border-width" -> st.borderWidth(intVal(prop, val));
                case "padding" -> st.padding(intVal(prop, val));
                case "gap" -> st.gap(intVal(prop, val));
                case "width" -> st.width(intVal(prop, val));
                case "height" -> st.height(intVal(prop, val));
                case "grow" -> st.grow(intVal(prop, val));
                default -> throw new PssException("Unknown property: " + prop);
            }
        }
        return st;
    }

    static int color(String v) {
        if (!v.startsWith("#")) throw new PssException("Expected #hex color, got: " + v);
        String hex = v.substring(1);
        long parsed;
        try {
            parsed = Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            throw new PssException("Bad hex color: " + v);
        }
        return switch (hex.length()) {
            case 6 -> (int) (0xFF000000L | parsed);
            case 8 -> (int) parsed;
            default -> throw new PssException("Color must be #RRGGBB or #AARRGGBB: " + v);
        };
    }

    private static int intVal(String prop, String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new PssException("Property " + prop + " expects an integer, got: " + v);
        }
    }

    /** Cascades the rules onto the whole tree. Matches merge lowest specificity
     *  first (order breaks ties), and each component's inline style wins last. */
    public static void apply(Component root, List<Rule> rules) {
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparingInt((Rule r) -> r.selector().specificity())
                .thenComparingInt(Rule::order));
        applyTo(root, sorted);
    }

    private static void applyTo(Component c, List<Rule> sorted) {
        Style sheet = null;
        for (Rule r : sorted) {
            if (r.selector().matches(c)) {
                sheet = sheet == null ? r.style() : r.style().mergedOver(sheet);
            }
        }
        if (sheet != null) c.style(c.style().mergedOver(sheet));
        for (int i = 0; i < c.childCount(); i++) applyTo(c.childAt(i), sorted);
    }
}
