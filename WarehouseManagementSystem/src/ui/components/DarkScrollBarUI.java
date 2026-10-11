package ui.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import ui.UITheme;

/**
 * Thin dark scroll bar with a rounded thumb and no arrow buttons.
 * Registered for every JScrollBar in UITheme.install().
 */
public class DarkScrollBarUI extends BasicScrollBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new DarkScrollBarUI();
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor = UITheme.BUTTON_BORDER;
        trackColor = UITheme.CARD;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return emptyButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return emptyButton();
    }

    private static JButton emptyButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        button.setFocusable(false);
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        g.setColor(UITheme.CARD);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent c, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isThumbRollover() || isDragging ? UITheme.TEXT_DIM : UITheme.BUTTON_BORDER);
        int inset = 3;
        g.fill(new RoundRectangle2D.Float(bounds.x + inset, bounds.y + inset,
                bounds.width - inset * 2f, bounds.height - inset * 2f, 8, 8));
        g.dispose();
    }
}
