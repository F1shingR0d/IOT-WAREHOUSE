package ui.components;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;
import ui.UITheme;

/**
 * The check box and radio button pictures of the theme, drawn with
 * Java 2D: a gray outline that becomes amber when selected.
 */
public final class ToggleIcon implements Icon, UIResource {

    private static final int SIZE = 20;
    private final boolean radio;

    private ToggleIcon(boolean radio) {
        this.radio = radio;
    }

    public static Icon checkBox() {
        return new ToggleIcon(false);
    }

    public static Icon radioButton() {
        return new ToggleIcon(true);
    }

    @Override
    public void paintIcon(Component c, Graphics graphics, int x, int y) {
        ButtonModel model = ((AbstractButton) c).getModel();
        boolean selected = model.isSelected();
        boolean enabled = model.isEnabled();
        boolean hover = model.isRollover();

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        java.awt.Color line = !enabled ? UITheme.BORDER
                : selected || hover ? UITheme.BRAND : UITheme.INPUT_BORDER;
        java.awt.Color accent = enabled ? UITheme.BRAND : UITheme.TEXT_DIM;

        if (radio) {
            g.setColor(UITheme.INPUT);
            g.fill(new Ellipse2D.Float(1, 1, SIZE - 2, SIZE - 2));
            g.setColor(selected ? accent : line);
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Float(2, 2, SIZE - 4, SIZE - 4));
            if (selected) {
                g.fill(new Ellipse2D.Float(6, 6, SIZE - 12, SIZE - 12));
            }
        } else {
            RoundRectangle2D box = new RoundRectangle2D.Float(2, 2, SIZE - 4, SIZE - 4, 5, 5);
            g.setColor(selected ? accent : UITheme.INPUT);
            g.fill(box);
            g.setColor(selected ? accent : line);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(box);
            if (selected) {
                g.setColor(java.awt.Color.BLACK);
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D check = new Path2D.Float();
                check.moveTo(6, 10.5);
                check.lineTo(9, 13.5);
                check.lineTo(14.5, 7);
                g.draw(check);
            }
        }
        g.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
