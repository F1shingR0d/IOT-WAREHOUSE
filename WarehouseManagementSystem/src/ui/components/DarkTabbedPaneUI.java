package ui.components;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import ui.UITheme;

/**
 * Flat dark tabs: the selected tab is written in amber and underlined,
 * the others are gray. Registered for every JTabbedPane in UITheme.install().
 */
public class DarkTabbedPaneUI extends BasicTabbedPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new DarkTabbedPaneUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabPane.setOpaque(false);
        tabPane.setFont(UITheme.FONT_BOLD.deriveFont(17f));
        tabInsets = new Insets(8, 18, 10, 18);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(10, 0, 0, 0);
    }

    @Override
    protected void paintTabBackground(Graphics g, int placement, int index, int x, int y, int w, int h,
                                      boolean selected) {
        if (selected) {
            g.setColor(UITheme.PRIMARY_LIGHT);
            g.fillRect(x, y, w, h);
        }
    }

    @Override
    protected void paintTabBorder(Graphics g, int placement, int index, int x, int y, int w, int h,
                                  boolean selected) {
        if (selected) {
            g.setColor(UITheme.BRAND);
            g.fillRect(x, y + h - 3, w, 3);
        }
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects, int index,
                                       Rectangle iconRect, Rectangle textRect, boolean selected) {
        // the amber underline already shows the selected tab
    }

    @Override
    protected void paintText(Graphics graphics, int placement, Font font, FontMetrics metrics, int index,
                             String title, Rectangle textRect, boolean selected) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(selected ? UITheme.BRAND : tabPane.isEnabledAt(index) ? UITheme.TEXT_MUTED : UITheme.TEXT_DIM);
        g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        g.dispose();
    }

    @Override
    protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
        Rectangle tabs = getTabBounds(tabPane, Math.max(0, selectedIndex));
        g.setColor(UITheme.BORDER);
        g.fillRect(0, tabs.y + tabs.height - 1, tabPane.getWidth(), 1);
    }
}
