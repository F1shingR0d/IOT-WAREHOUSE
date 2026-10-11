package ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import ui.UITheme;

/**
 * Rounded border of the input fields, drawn with Java 2D. The line is gray,
 * amber while the field has the keyboard focus, and the border can also show
 * a gray hint ("Search product...") and a small icon inside an empty field.
 */
public class InputBorder extends AbstractBorder implements UIResource {

    private static final long serialVersionUID = 1L;

    /** Client property with the hint text of an empty field. */
    public static final String PLACEHOLDER = "iot.placeholder";
    /** Client property with an Icon shown at the left side of the field. */
    public static final String LEADING_ICON = "iot.leadingIcon";

    private static final int ARC = 10;
    private final Insets padding;

    public InputBorder() {
        this(new Insets(6, 10, 6, 10));
    }

    public InputBorder(Insets padding) {
        this.padding = padding;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(padding.top, padding.left + iconSpace(c), padding.bottom, padding.right);
        return insets;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, new Insets(0, 0, 0, 0));
    }

    private static int iconSpace(Component c) {
        Icon icon = leadingIcon(c);
        return icon == null ? 0 : icon.getIconWidth() + 8;
    }

    private static Icon leadingIcon(Component c) {
        if (c instanceof JComponent) {
            Object icon = ((JComponent) c).getClientProperty(LEADING_ICON);
            return icon instanceof Icon ? (Icon) icon : null;
        }
        return null;
    }

    @Override
    public void paintBorder(Component c, Graphics graphics, int x, int y, int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // fill the corners outside the rounded line with the color behind the field
        Color behind = c.getParent() != null ? c.getParent().getBackground() : UITheme.CARD;
        if (c.getParent() != null && !c.getParent().isOpaque()) {
            behind = UITheme.CARD;
        }
        java.awt.geom.Area corners = new java.awt.geom.Area(new java.awt.Rectangle(x, y, width, height));
        corners.subtract(new java.awt.geom.Area(new RoundRectangle2D.Float(x, y, width - 1f, height - 1f, ARC, ARC)));
        g.setColor(behind);
        g.fill(corners);

        g.setStroke(new BasicStroke(hasFocus(c) ? 1.6f : 1.2f));
        g.setColor(lineColor(c));
        g.draw(new RoundRectangle2D.Float(x + 0.8f, y + 0.8f, width - 2.6f, height - 2.6f, ARC, ARC));

        Icon icon = leadingIcon(c);
        if (icon != null) {
            icon.paintIcon(c, g, x + padding.left, y + (height - icon.getIconHeight()) / 2);
        }
        if (c instanceof JTextComponent) {
            paintPlaceholder((JTextComponent) c, g, x, y, height);
        }
        g.dispose();
    }

    private void paintPlaceholder(JTextComponent field, Graphics2D g, int x, int y, int height) {
        Object hint = field.getClientProperty(PLACEHOLDER);
        if (hint == null || field.getDocument().getLength() > 0) {
            return;
        }
        g.setFont(field.getFont());
        g.setColor(UITheme.TEXT_DIM);
        FontMetrics fm = g.getFontMetrics();
        int textX = x + padding.left + iconSpace(field) + 2;
        int textY = y + (height - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(hint.toString(), textX, textY);
    }

    private static boolean hasFocus(Component c) {
        if (c instanceof JScrollPane) {   // a text area inside a scroll pane
            Component view = ((JScrollPane) c).getViewport().getView();
            return view != null && view.hasFocus();
        }
        return c.hasFocus();
    }

    private static Color lineColor(Component c) {
        if (hasFocus(c)) {
            return UITheme.BRAND;
        }
        boolean editable = !(c instanceof JTextComponent) || ((JTextComponent) c).isEditable();
        return c.isEnabled() && editable ? UITheme.INPUT_BORDER : UITheme.BORDER;
    }
}
