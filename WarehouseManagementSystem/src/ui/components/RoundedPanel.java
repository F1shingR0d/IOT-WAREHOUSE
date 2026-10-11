package ui.components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import ui.UITheme;

/**
 * A dark "card" with rounded corners and a thin amber-brown border. Most
 * screens place their forms and tables on these cards.
 */
public class RoundedPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int ARC = 12;

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(14, 18, 14, 18));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1.5f, getHeight() - 1.5f, ARC, ARC);
        g.setColor(UITheme.CARD);
        g.fill(shape);
        g.setColor(UITheme.CARD_BORDER);
        g.draw(shape);
        g.dispose();
        super.paintComponent(graphics);
    }
}
