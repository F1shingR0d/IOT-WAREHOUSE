package ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import ui.UITheme;

/**
 * Look of the standard JButtons (the buttons of message dialogs, the file
 * chooser and the calendar): light gray rounded buttons, and an amber one for the
 * default button. The program's own buttons are FlatButtons.
 */
public class DarkButtonUI extends BasicButtonUI {

    private static final DarkButtonUI INSTANCE = new DarkButtonUI();

    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        if (b.getBorder() == null || b.getBorder() instanceof UIResource) {
            b.setBorder(new BorderResource(8, 18, 8, 18));
        }
    }

    @Override
    public void paint(Graphics graphics, JComponent c) {
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();
        boolean isDefault = button instanceof JButton && ((JButton) button).isDefaultButton();
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, c.getWidth() - 1.5f, c.getHeight() - 1.5f, 10, 10);
        if (button.isContentAreaFilled()) {
            Color fill = isDefault ? UITheme.BRAND : UITheme.BUTTON_DARK;
            if (model.isPressed()) {
                fill = fill.darker();
            } else if (model.isRollover() && isDefault) {
                fill = UITheme.BRAND_LIGHT;
            }
            g.setColor(button.isEnabled() ? fill : UITheme.INPUT_DISABLED);
            g.fill(shape);
            if (!isDefault) {
                g.setColor(model.isRollover() || button.hasFocus() ? UITheme.BRAND : UITheme.BUTTON_BORDER);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(shape);
            }
        }
        g.dispose();
        super.paint(graphics, c);
    }

    @Override
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        boolean isDefault = b instanceof JButton && ((JButton) b).isDefaultButton();
        Color color = !b.isEnabled() ? UITheme.TEXT_DIM
                : isDefault ? Color.BLACK
                : b.getForeground() instanceof UIResource ? UITheme.TEXT : b.getForeground();
        g.setColor(color);
        FontMetrics fm = b.getFontMetrics(b.getFont());
        BasicGraphicsUtils.drawStringUnderlineCharAt(b, (Graphics2D) g, text, b.getDisplayedMnemonicIndex(),
                textRect.x + getTextShiftOffset(), textRect.y + fm.getAscent() + getTextShiftOffset());
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
        // the amber outline shows the focus
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        // the darker fill shows the pressed state
    }

    private static final class BorderResource extends EmptyBorder implements UIResource {
        private static final long serialVersionUID = 1L;

        BorderResource(int top, int left, int bottom, int right) {
            super(top, left, bottom, right);
        }
    }
}
