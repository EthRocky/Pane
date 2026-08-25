package net.ethrocky.pane.markup;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.widget.Button;
import net.ethrocky.pane.widget.Checkbox;
import net.ethrocky.pane.widget.ColorPicker;
import net.ethrocky.pane.widget.Dropdown;
import net.ethrocky.pane.widget.Label;
import net.ethrocky.pane.widget.ListView;
import net.ethrocky.pane.widget.Panel;
import net.ethrocky.pane.widget.ProgressBar;
import net.ethrocky.pane.widget.ScrollContainer;
import net.ethrocky.pane.widget.Slider;
import net.ethrocky.pane.widget.Tabs;
import net.ethrocky.pane.widget.TextInput;
import net.ethrocky.pane.widget.Window;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Builds a widget tree from PML (XML). Every element maps to one widget; bind=
 *  and onclick=/onsubmit= reference the PmlContext by name. Each component's
 *  selector tag is the element name, so PSS applies naturally afterwards. */
public final class Pml {
    private Pml() {
    }

    public static Window window(String xml, PmlContext ctx) {
        Element root = parse(xml);
        if (!root.getTagName().equalsIgnoreCase("window")) {
            throw new PmlException("Root element must be <window>, got <" + root.getTagName() + ">");
        }
        Window win = Window.of(attr(root, "title", "Window"));
        win.at(intAttr(root, "x", 40), intAttr(root, "y", 40));
        if (attrOrNull(root, "width") != null) win.style().width(intAttr(root, "width", 220));
        win.tag("window");
        common(win, root);
        for (Element child : children(root)) win.add(component(child, ctx));
        return win;
    }

    public static Component component(Element el, PmlContext ctx) {
        String tag = el.getTagName().toLowerCase();
        Component c = switch (tag) {
            case "label" -> {
                String bind = attrOrNull(el, "bind");
                yield bind != null ? Label.of(ctx.state(bind, String.class, tag))
                        : Label.of(attr(el, "text", ""));
            }
            case "textinput" -> {
                TextInput t = TextInput.of(ctx.state(bind(el, tag), String.class, tag));
                String submit = attrOrNull(el, "onsubmit");
                if (submit != null) t.onSubmit(ctx.action(submit, tag));
                yield t;
            }
            case "button" -> {
                Button b = Button.of(attr(el, "text", "Button"));
                String click = attrOrNull(el, "onclick");
                if (click != null) b.onClick(ctx.action(click, tag));
                yield b;
            }
            case "checkbox" -> Checkbox.of(attr(el, "text", ""), ctx.state(bind(el, tag), Boolean.class, tag));
            case "slider" -> Slider.of(ctx.state(bind(el, tag), Float.class, tag),
                    floatAttr(el, "min", 0f), floatAttr(el, "max", 1f));
            case "dropdown" -> Dropdown.of(ctx.state(bind(el, tag), String.class, tag),
                    csv(attr(el, "options", "")));
            case "listview" -> ListView.of(ctx.state(bind(el, tag), String.class, tag),
                    csv(attr(el, "items", "")));
            case "scroll" -> {
                ScrollContainer sc = ScrollContainer.of(intAttr(el, "height", 150));
                for (Element child : children(el)) sc.add(component(child, ctx));
                yield sc;
            }
            case "colorpicker" -> ColorPicker.of(ctx.state(bind(el, tag), Integer.class, tag));
            case "progress" -> ProgressBar.of(ctx.state(bind(el, tag), Float.class, tag));
            case "row", "column" -> {
                Panel p = tag.equals("row") ? Panel.row() : Panel.column();
                for (Element child : children(el)) p.add(component(child, ctx));
                yield p;
            }
            case "tabs" -> buildTabs(el, ctx);
            default -> throw new PmlException("Unknown element <" + tag + ">");
        };
        c.tag(tag);
        common(c, el);
        return c;
    }

    private static Tabs buildTabs(Element el, PmlContext ctx) {
        Tabs tabs = Tabs.of();
        for (Element child : children(el)) {
            if (!child.getTagName().equalsIgnoreCase("tab")) {
                throw new PmlException("<tabs> may only contain <tab>, got <" + child.getTagName() + ">");
            }
            List<Element> content = children(child);
            Component body;
            if (content.size() == 1) {
                body = component(content.get(0), ctx);
            } else {
                Panel p = Panel.column();
                for (Element e : content) p.add(component(e, ctx));
                body = p;
            }
            tabs.tab(attr(child, "title", "Tab"), body);
        }
        return tabs;
    }

    private static void common(Component c, Element el) {
        String id = attrOrNull(el, "id");
        if (id != null) c.id(id);
        String classes = attrOrNull(el, "class");
        if (classes != null) {
            for (String cls : classes.trim().split("\\s+")) {
                if (!cls.isEmpty()) c.cssClass(cls);
            }
        }
        String grow = attrOrNull(el, "grow");
        if (grow != null) c.grow(Integer.parseInt(grow));
    }

    // ---- XML helpers ----

    private static Element parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            Document doc = f.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            return doc.getDocumentElement();
        } catch (PmlException e) {
            throw e;
        } catch (Exception e) {
            throw new PmlException("Malformed PML: " + e.getMessage(), e);
        }
    }

    private static List<Element> children(Element el) {
        List<Element> out = new ArrayList<>();
        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n instanceof Element child) out.add(child);
        }
        return out;
    }

    private static String bind(Element el, String tag) {
        String bind = attrOrNull(el, "bind");
        if (bind == null) throw new PmlException("<" + tag + "> requires bind=\"stateName\"");
        return bind;
    }

    private static String attrOrNull(Element el, String name) {
        return el.hasAttribute(name) ? el.getAttribute(name) : null;
    }

    private static String attr(Element el, String name, String fallback) {
        String v = attrOrNull(el, name);
        return v != null ? v : fallback;
    }

    private static int intAttr(Element el, String name, int fallback) {
        String v = attrOrNull(el, name);
        return v != null ? Integer.parseInt(v) : fallback;
    }

    private static float floatAttr(Element el, String name, float fallback) {
        String v = attrOrNull(el, name);
        return v != null ? Float.parseFloat(v) : fallback;
    }

    private static List<String> csv(String v) {
        List<String> out = new ArrayList<>();
        for (String part : v.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }
}
