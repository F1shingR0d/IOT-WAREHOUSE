package ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import ui.UITheme;

/**
 * A navigation button of the sidebar. A MouseListener (MouseAdapter)
 * highlights the button while the mouse is over it; the selected module is
 * shown in amber with black text (the logo colors).
 */
public class SidebarButton extends JButton {

    private static final long serialVersionUID = 1L;

    private final ModuleIcon icon;
    private boolean hover;
    private boolean selected;

    public SidebarButton(String text, ModuleIcon.Type iconType) {
        super(text);
        this.icon = new ModuleIcon(iconType, 22, UITheme.BRAND);
        setFont(UITheme.FONT_NAV);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAlignmentX(LEFT_ALIGNMENT);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(226, 44);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 44);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (selected || hover) {
            g.setColor(selected ? UITheme.PRIMARY : UITheme.SIDEBAR_HOVER);
            g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
        }
        Color foreground = selected ? Color.BLACK : hover ? UITheme.BRAND : UITheme.SIDEBAR_TEXT;
        icon.setColor(selected ? Color.BLACK : UITheme.BRAND);   // amber icons, black on the selected item
        int iconY = (getHeight() - icon.getIconHeight()) / 2;
        icon.paintIcon(this, g, 12, iconY);

        g.setFont(selected ? UITheme.FONT_NAV_BOLD : UITheme.FONT_NAV);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(foreground);
        g.drawString(getText(), 50, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        if (isFocusOwner()) {
            g.setColor(UITheme.BRAND_LIGHT);
            g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1.5f, getHeight() - 1.5f, 10, 10));
        }
        g.dispose();
    }
}
