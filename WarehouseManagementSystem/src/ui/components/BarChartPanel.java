package ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import model.Warehouse;
import ui.UITheme;
import util.Formats;

/**
 * Horizontal bar chart "stock vs capacity" per warehouse, drawn entirely with
 * AWT Graphics2D. Bar colors: amber below 70 %, orange 70 - 90 %, red above 90 %.
 */
public class BarChartPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private List<Warehouse> warehouses = new ArrayList<>();

    public BarChartPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(360, 240));
    }

    public void setWarehouses(List<Warehouse> warehouses) {
        this.warehouses = new ArrayList<>(warehouses);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (warehouses.isEmpty()) {
            g.setFont(UITheme.FONT);
            g.setColor(UITheme.TEXT_MUTED);
            g.drawString("No active warehouses.", 4, 24);
            g.dispose();
            return;
        }

        int width = getWidth();
        int legendHeight = 26;
        int space = (getHeight() - legendHeight) / warehouses.size();
        // when the dashboard is short (small screen, or the customer-request banner is shown),
        // the "x of y units" line is left out so the rows never overlap the legend
        boolean compact = space < 70;
        int rowHeight = compact ? Math.max(48, space) : Math.min(96, space);
        int barHeight = 16;
        int y = 4;
        for (Warehouse w : warehouses) {
            double percent = w.getUtilization();
            // line 1: warehouse name on the left, percentage on the right
            g.setFont(UITheme.FONT_BOLD);
            FontMetrics fm = g.getFontMetrics();
            String percentText = Formats.percent(percent);
            int percentWidth = fm.stringWidth(percentText);
            g.setColor(UITheme.TEXT);
            g.drawString(fit(w.getWarehouseName(), fm, width - percentWidth - 16), 0, y + fm.getAscent());
            g.setColor(percent >= 70 ? UITheme.readable(colorFor(percent)) : UITheme.PRIMARY_TEXT);
            g.drawString(percentText, width - percentWidth - 2, y + fm.getAscent());

            // line 2: background track = full capacity, colored part = stock on hand
            int barY = y + fm.getHeight() + 4;
            g.setColor(UITheme.TABLE_GRID);
            g.fill(new RoundRectangle2D.Float(0, barY, width - 2, barHeight, barHeight, barHeight));
            int filled = (int) Math.round((width - 2) * Math.min(100, percent) / 100.0);
            if (filled > 0) {
                g.setColor(colorFor(percent));
                g.fill(new RoundRectangle2D.Float(0, barY, Math.max(filled, barHeight), barHeight, barHeight, barHeight));
            }

            // line 3: units on hand and capacity
            if (!compact) {
                g.setFont(UITheme.FONT_SMALL);
                fm = g.getFontMetrics();
                g.setColor(UITheme.TEXT_MUTED);
                g.drawString(Formats.number(w.getStockOnHand()) + " of " + Formats.number(w.getCapacity()) + " units",
                        0, barY + barHeight + 4 + fm.getAscent());
            }
            y += rowHeight;
        }

        // legend
        int ly = getHeight() - 10;
        int lx = 0;
        g.setFont(UITheme.FONT_SMALL);
        FontMetrics fm = g.getFontMetrics();
        lx = legend(g, fm, lx, ly, UITheme.PRIMARY, "Below 70%");
        lx = legend(g, fm, lx, ly, UITheme.WARNING, "70% - 90%");
        legend(g, fm, lx, ly, UITheme.DANGER, "Above 90%");
        g.dispose();
    }

    private static int legend(Graphics2D g, FontMetrics fm, int x, int baseline, Color color, String text) {
        g.setColor(color);
        g.fillOval(x, baseline - 9, 9, 9);
        g.setColor(UITheme.TEXT_MUTED);
        g.drawString(text, x + 14, baseline);
        return x + 14 + fm.stringWidth(text) + 18;
    }

    /** Shortens a text with "..." so that it fits in the given width. */
    private static String fit(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String shortened = text;
        while (shortened.length() > 1 && fm.stringWidth(shortened + "...") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "...";
    }

    private static Color colorFor(double percent) {
        if (percent > 90) {
            return UITheme.DANGER;
        }
        return percent >= 70 ? UITheme.WARNING : UITheme.PRIMARY;
    }
}
