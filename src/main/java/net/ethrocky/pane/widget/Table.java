package net.ethrocky.pane.widget;

import net.ethrocky.pane.core.Component;
import net.ethrocky.pane.core.Rect;
import net.ethrocky.pane.core.Size;
import net.ethrocky.pane.core.State;
import net.ethrocky.pane.core.UiEvent;
import net.ethrocky.pane.core.style.Theme;
import net.ethrocky.pane.render.PaneRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Text table: a header row plus data rows, columns sized to their widest cell.
 *  Rows are clickable; bind a {@code State<Integer>} with {@link #selectable} to
 *  highlight the picked row, or just observe clicks with {@link #onRowClick}.
 *  Nest inside a ScrollContainer when the row count outgrows the panel.
 *
 *  <pre>
 *  Table.of("Light", "Group", "Radius")
 *       .row("key1", "stage", "16")
 *       .row("fill1", "stage", "24")
 *       .selectable(selectedRow);
 *  </pre> */
public class Table extends Component {
    private static final int CELL_PAD_X = 6;
    private static final int ROW_PAD_Y = 3;

    private final String[] headers;
    private final List<String[]> data = new ArrayList<>();
    private State<Integer> selected;    // null = no selection highlight
    private IntConsumer onRowClick;

    private Table(String[] headers) {
        this.headers = headers;
    }

    public static Table of(String... headers) {
        return new Table(headers);
    }

    /** Appends a row; missing cells paint empty, extras are ignored. */
    public Table row(String... cells) {
        data.add(cells);
        invalidate();
        return this;
    }

    /** Replaces every row (e.g. when the backing data changes). */
    public Table setRows(List<String[]> rows) {
        data.clear();
        data.addAll(rows);
        invalidate();
        return this;
    }

    public int rowCount() {
        return data.size();
    }

    /** Opt in to row selection: clicking a row stores its index (−1 = none). */
    public Table selectable(State<Integer> selectedRow) {
        this.selected = selectedRow;
        return this;
    }

    public Table onRowClick(IntConsumer handler) {
        this.onRowClick = handler;
        return this;
    }

    private int[] columnWidths(TextMeasurer tm) {
        int[] w = new int[headers.length];
        for (int c = 0; c < headers.length; c++) w[c] = tm.width(headers[c]);
        for (String[] row : data) {
            for (int c = 0; c < headers.length && c < row.length; c++) {
                if (row[c] != null) w[c] = Math.max(w[c], tm.width(row[c]));
            }
        }
        for (int c = 0; c < w.length; c++) w[c] += CELL_PAD_X * 2;
        return w;
    }

    private int rowHeight(TextMeasurer tm) {
        return tm.height() + ROW_PAD_Y * 2;
    }

    @Override
    public Size measure(TextMeasurer tm) {
        int total = 0;
        for (int w : columnWidths(tm)) total += w;
        int h = rowHeight(tm) * (data.size() + 1);   // +1 header
        if (style.width != null) total = style.width;
        if (style.height != null) h = style.height;
        return new Size(total, h);
    }

    @Override
    protected boolean handleMouse(UiEvent.Mouse e) {
        if (e.kind() != UiEvent.Kind.DOWN || !e.inside(bounds)) return false;
        int rh = lastRowHeight > 0 ? lastRowHeight : 12;
        int index = (int) ((e.y() - bounds.y()) / rh) - 1;   // −1: header row doesn't select
        if (index < 0 || index >= data.size()) return true;  // header/empty click still consumed
        if (selected != null) selected.set(index);
        if (onRowClick != null) onRowClick.accept(index);
        return true;
    }

    private int lastRowHeight;

    @Override
    protected void paintSelf(PaneRenderer r, Theme theme) {
        int[] cols = columnWidths(r);
        int rh = rowHeight(r);
        lastRowHeight = rh;
        int y = bounds.y();

        // header
        int x = bounds.x();
        for (int c = 0; c < headers.length; c++) {
            r.text(headers[c], x + CELL_PAD_X, y + ROW_PAD_Y, theme.textDim());
            x += cols[c];
        }
        r.fill(new Rect(bounds.x(), y + rh - 1, bounds.w(), 1), theme.border());
        y += rh;

        // rows: zebra stripes, selection accent
        int sel = selected != null ? selected.get() : -1;
        for (int i = 0; i < data.size(); i++, y += rh) {
            Rect rowRect = new Rect(bounds.x(), y, bounds.w(), rh);
            if (i == sel) r.fill(rowRect, theme.accent());
            else if ((i & 1) == 1) r.fill(rowRect, theme.surfaceRaised());
            String[] row = data.get(i);
            x = bounds.x();
            for (int c = 0; c < headers.length; c++) {
                if (c < row.length && row[c] != null) {
                    r.text(row[c], x + CELL_PAD_X, y + ROW_PAD_Y, i == sel ? theme.surface() : theme.text());
                }
                x += cols[c];
            }
        }
    }
}
