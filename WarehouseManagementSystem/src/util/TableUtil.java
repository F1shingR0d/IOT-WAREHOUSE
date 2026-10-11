package util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import ui.UITheme;

/**
 * Creates consistently styled JTables and provides printing and CSV export.
 * Numbers are right-aligned, money gets the peso sign and status words
 * (ACTIVE, VOIDED, LOW, ...) are colored.
 */
public final class TableUtil {

    private TableUtil() {
    }

    /** A read-only table model; the column types decide sorting and formatting. */
    public static DefaultTableModel createModel(String[] columns, Class<?>[] types) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return types != null && column < types.length ? types[column] : Object.class;
            }
        };
    }

    /**
     * A styled table that fills the available width (columns may shrink a
     * little) and scrolls sideways only when the columns really do not fit.
     */
    public static JTable createTable(TableModel model) {
        return createTable(model, false);
    }

    /** @param alwaysFit true = never scroll sideways, always shrink the columns to the width */
    public static JTable createTable(TableModel model, boolean alwaysFit) {
        JTable table = new JTable(model) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                Container parent = getParent();
                if (alwaysFit || parent == null) {
                    return true;
                }
                int minimum = 0;
                for (int c = 0; c < getColumnCount(); c++) {
                    minimum += getColumnModel().getColumn(c).getMinWidth();
                }
                return minimum <= parent.getWidth() && getPreferredSize().width <= parent.getWidth() * 1.2;
            }
        };
        table.setFont(UITheme.FONT_TABLE);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(UITheme.TABLE_GRID);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(UITheme.TABLE_SELECTION);
        table.setSelectionForeground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setBackground(UITheme.TABLE_ROW);
        table.setForeground(UITheme.TEXT);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(UITheme.TABLE_HEADER);
        header.setDefaultRenderer(new HeaderRenderer());

        CellRenderer renderer = new CellRenderer();
        for (Class<?> type : new Class<?>[]{Object.class, String.class, Number.class, Integer.class,
            Long.class, Double.class, BigDecimal.class}) {
            table.setDefaultRenderer(type, renderer);
        }
        return table;
    }

    /**
     * Preferred column widths. Short columns (codes, numbers, status) keep
     * almost their full width when the table is squeezed; long text columns
     * give up the space instead.
     */
    public static void setColumnWidths(JTable table, int... widths) {
        double scale = UITheme.FONT_TABLE.getSize2D() / 13.0;   // the widths below were designed for a 13 px font
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            int heading = headerWidth(table, i);
            if ("Status".equals(column.getHeaderValue())) {
                heading = Math.max(heading, 112);   // room for the status badges (ACTIVE, INACTIVE, VOIDED...)
            }
            int width = Math.max((int) Math.round(widths[i] * scale), heading);
            column.setPreferredWidth(width);
            if (widths[i] <= 140) {   // short columns (codes, dates, references, numbers) are never cut off
                column.setMinWidth(Math.max(width - 6, heading));
            }
        }
    }

    /** Width needed to show a column heading completely, including the sort arrow. */
    private static int headerWidth(JTable table, int viewColumn) {
        TableColumn column = table.getColumnModel().getColumn(viewColumn);
        Component header = table.getTableHeader().getDefaultRenderer()
                .getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, viewColumn);
        return header.getPreferredSize().width + 14;
    }

    /** Sizes every column to fit its heading and contents (checks up to 300 rows). */
    public static void autoSizeColumns(JTable table) {
        for (int c = 0; c < table.getColumnCount(); c++) {
            TableColumn column = table.getColumnModel().getColumn(c);
            Component header = table.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, c);
            int width = header.getPreferredSize().width + 16;
            for (int r = 0; r < Math.min(table.getRowCount(), 300); r++) {
                Component cell = table.prepareRenderer(table.getCellRenderer(r, c), r, c);
                width = Math.max(width, cell.getPreferredSize().width + 12);
            }
            column.setPreferredWidth(Math.min(width, 320));
            column.setMinWidth(Math.min(width, 140));   // short columns (dates, codes, numbers) never get cut off
        }
    }

    /** Hides a column (e.g. an ID) from the view; the value stays in the model. */
    public static void hideColumn(JTable table, int modelIndex) {
        int viewIndex = table.convertColumnIndexToView(modelIndex);
        if (viewIndex >= 0) {
            TableColumn column = table.getColumnModel().getColumn(viewIndex);
            table.removeColumn(column);
        }
    }

    /** Model index of the selected row, or -1 when nothing is selected. */
    public static int getSelectedModelRow(JTable table) {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    public static void setRows(DefaultTableModel model, List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    // ------------------------------------------------------------------ export and print

    /** Saves the visible rows (in the current sort order) to a CSV file that Excel can open. */
    public static void exportToCsv(Component parent, JTable table, String baseName) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent, "There are no rows to export.", "Export",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export to CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File(baseName.replaceAll("[^A-Za-z0-9]+", "_") + "_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getPath() + ".csv");
        }
        if (file.exists() && JOptionPane.showConfirmDialog(parent, file.getName() + " already exists. Replace it?",
                "Export", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            out.write('﻿');   // byte order mark so Excel reads UTF-8 correctly
            for (int c = 0; c < table.getColumnCount(); c++) {
                out.write((c > 0 ? "," : "") + csv(table.getColumnName(c)));
            }
            out.write("\r\n");
            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    out.write((c > 0 ? "," : "") + csv(table.getValueAt(r, c)));
                }
                out.write("\r\n");
            }
            JOptionPane.showMessageDialog(parent, "Exported " + table.getRowCount() + " row(s) to\n" + file.getPath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            ErrorHandler.log(e);
            JOptionPane.showMessageDialog(parent, "The file could not be saved:\n" + e.getMessage(),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    /** Prints the table (the system print dialog also allows "Microsoft Print to PDF"). */
    public static void print(Component parent, JTable table, String title) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent, "There are no rows to print.", "Print",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String printed = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
        printing = true;   // print black text on white paper instead of the dark screen colors
        try {
            table.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(escape(title)),
                    new MessageFormat(escape(UITheme.APP_NAME + "  |  Printed " + printed + "  |  Page ") + "{0}"),
                    true, null, true);
        } catch (PrinterException e) {
            ErrorHandler.log(e);
            JOptionPane.showMessageDialog(parent, "Printing failed:\n" + e.getMessage(), "Print",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            printing = false;
        }
    }

    /** true while a table is being printed (the renderers then use paper colors). */
    private static volatile boolean printing;

    /** MessageFormat treats ' { } as special characters. */
    private static String escape(String text) {
        return text.replace("'", "''").replace("{", "'{'").replace("}", "'}'");
    }

    // ------------------------------------------------------------------ renderers

    /** Colors used for status words in any table. */
    static Color statusColor(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String text = (String) value;
        switch (text) {
            case "ACTIVE":
            case "POSTED":
            case "OK":
            case "Receiving":
                return UITheme.SUCCESS;
            case "INACTIVE":
                return UITheme.NEUTRAL;
            case "VOIDED":
            case "OUT":
                return UITheme.DANGER;
            case "LOW":
            case "Release":
                return UITheme.WARNING;
            case "Transfer":
                return UITheme.CHARCOAL;
            default:
                if (text.startsWith("IN - ")) {
                    return UITheme.SUCCESS;
                }
                return text.startsWith("OUT - ") ? UITheme.DANGER : null;
        }
    }

    /** Status words shown as a colored badge ("pill") instead of plain text. */
    private static boolean isBadge(Object value) {
        if (!(value instanceof String)) {
            return false;
        }
        switch ((String) value) {
            case "ACTIVE":
            case "INACTIVE":
            case "POSTED":
            case "VOIDED":
            case "OK":
            case "LOW":
            case "OUT":
                return true;
            default:
                return false;
        }
    }

    /**
     * Formats numbers and money, stripes the rows, colors type words and
     * draws status words (ACTIVE, VOIDED, LOW...) as colored badges.
     */
    private static final class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final EmptyBorder padding = new EmptyBorder(0, 10, 0, 10);
        private Color badge;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Object display = value;
            int alignment = LEFT;
            if (value instanceof BigDecimal) {
                display = Formats.money((BigDecimal) value);
                alignment = RIGHT;
            } else if (value instanceof Double) {
                display = Formats.percent((Double) value);
                alignment = RIGHT;
            } else if (value instanceof Integer || value instanceof Long) {
                display = Formats.number(((Number) value).longValue());
                alignment = RIGHT;
            }
            super.getTableCellRendererComponent(table, display, isSelected, false, row, column);
            setHorizontalAlignment(alignment);
            setBorder(padding);
            // the full text appears as a tooltip when the column is too narrow to show it
            String text = display == null ? null : display.toString();
            boolean cut = text != null && getFontMetrics(UITheme.FONT_TABLE_BOLD).stringWidth(text) + 20
                    > table.getColumnModel().getColumn(column).getWidth();
            setToolTipText(cut ? text : null);

            Color status = statusColor(value);
            badge = !printing && isBadge(value) ? status : null;
            if (printing) {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
                setFont(status != null ? UITheme.FONT_TABLE_BOLD : UITheme.FONT_TABLE);
                return this;
            }
            if (!isSelected) {
                setBackground(row % 2 == 0 ? UITheme.TABLE_ROW : UITheme.TABLE_STRIPE);
            }
            if (badge != null) {
                setFont(UITheme.FONT_SECTION.deriveFont(12.5f));
                setForeground(UITheme.textOn(badge));
            } else if (status != null) {
                setForeground(status);
                setFont(UITheme.FONT_TABLE_BOLD);
            } else {
                setForeground(isSelected ? Color.WHITE : UITheme.TEXT);
                setFont(UITheme.FONT_TABLE);
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (badge == null) {
                super.paintComponent(graphics);
                return;
            }
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int pillWidth = fm.stringWidth(getText()) + 22;
            int pillHeight = Math.min(getHeight() - 10, fm.getHeight() + 6);
            int x = 10;
            int y = (getHeight() - pillHeight) / 2;
            g.setColor(badge);
            g.fill(new RoundRectangle2D.Float(x, y, pillWidth, pillHeight, pillHeight, pillHeight));
            g.setColor(getForeground());
            g.drawString(getText(), x + 11, y + (pillHeight - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Dark gold header with light bold text and a sort arrow. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final CompoundBorder border = new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(126, 94, 42)), new EmptyBorder(9, 10, 9, 10));

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(UITheme.FONT_TABLE_BOLD);
            setForeground(printing ? Color.BLACK : UITheme.TABLE_HEADER_TEXT);
            setBackground(printing ? Color.WHITE : UITheme.TABLE_HEADER);
            setOpaque(true);
            setBorder(border);
            boolean numeric = Number.class.isAssignableFrom(table.getColumnClass(column));
            setHorizontalAlignment(numeric ? RIGHT : LEFT);
            setHorizontalTextPosition(numeric ? RIGHT : LEFT);
            setIcon(sortIcon(table, column));
            return this;
        }

        private static Icon sortIcon(JTable table, int viewColumn) {
            RowSorter<?> sorter = table.getRowSorter();
            if (sorter == null || sorter.getSortKeys().isEmpty()) {
                return null;
            }
            RowSorter.SortKey key = sorter.getSortKeys().get(0);
            if (key.getColumn() != table.convertColumnIndexToModel(viewColumn)
                    || key.getSortOrder() == SortOrder.UNSORTED) {
                return null;
            }
            return new SortArrow(key.getSortOrder() == SortOrder.ASCENDING);
        }
    }

    /** Small triangle showing the sort direction. */
    private static final class SortArrow implements Icon {
        private final boolean ascending;

        SortArrow(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(UITheme.PRIMARY);
            int[] xs = {x, x + 8, x + 4};
            int[] ys = ascending ? new int[]{y + 6, y + 6, y + 1} : new int[]{y + 1, y + 1, y + 6};
            g.fillPolygon(xs, ys, 3);
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 10;
        }

        @Override
        public int getIconHeight() {
            return 7;
        }
    }
}
