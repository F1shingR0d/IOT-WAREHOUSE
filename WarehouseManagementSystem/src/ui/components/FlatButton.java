package ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import ui.UITheme;

/**
 * A modern flat button painted with Java 2D, optionally with a small icon.
 * Two styles: filled (amber or another color, gets lighter on hover) and
 * outline (dark button with a gray line that turns amber on hover).
 * The text is black or white, whichever is readable on the button color.
 */
public class FlatButton extends JButton {

    private static final long serialVersionUID = 1L;
    private static final int ARC = 10;
    private static final int ICON_SIZE = 18;
    private static final int ICON_GAP = 9;

    private final Color color;
    private final boolean outline;
    private ModuleIcon icon;
    private Color accent;

    public FlatButton(String text, Color color) {
        this(text, color, false, null);
    }

    public FlatButton(String text, Color color, ModuleIcon.Type iconType) {
        this(text, color, false, iconType);
    }

    private FlatButton(String text, Color color, boolean outline, ModuleIcon.Type iconType) {
        super(text);
        this.color = color;
        this.outline = outline;
        if (iconType != null) {
            icon = new ModuleIcon(iconType, ICON_SIZE, Color.WHITE);
        }
        setFont(UITheme.FONT_BOLD);
        setForeground(outline ? UITheme.TEXT : UITheme.textOn(color));   // black text on amber
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setBorder(new EmptyBorder(9, 20, 9, 20));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Dark button with a gray outline, for secondary actions. */
    public static FlatButton outline(String text) {
        return new FlatButton(text, UITheme.BUTTON_DARK, true, null);
    }

    /** Dark outline button with an icon. */
    public static FlatButton outline(String text, ModuleIcon.Type iconType) {
        return new FlatButton(text, UITheme.BUTTON_DARK, true, iconType);
    }

    /**
     * Gives an outline button a warning color (e.g. red for Delete): the icon
     * uses it and the outline turns that color on hover.
     */
    public FlatButton withAccent(Color accentColor) {
        this.accent = accentColor;
        repaint();
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int iconWidth = icon == null ? 0 : ICON_SIZE + ICON_GAP;
        return new Dimension(fm.stringWidth(getText()) + iconWidth + 40, Math.max(42, fm.getHeight() + 18));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        boolean pressed = getModel().isPressed() && getModel().isArmed();
        boolean hover = getModel().isRollover();
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.75f, 0.75f, w - 2.5f, h - 2.5f, ARC, ARC);

        Color fill;
        Color line = null;
        Color text;
        Color iconColor;
        if (!isEnabled()) {
            fill = outline ? UITheme.CARD : UITheme.INPUT_DISABLED;
            line = outline ? UITheme.BORDER : null;
            text = UITheme.TEXT_DIM;
            iconColor = text;
        } else if (outline) {
            Color hoverLine = accent != null ? accent : UITheme.BRAND;
            fill = pressed ? UITheme.SIDEBAR_HOVER : hover ? new Color(28, 35, 44) : UITheme.BUTTON_DARK;
            line = hover || isFocusOwner() ? hoverLine : UITheme.BUTTON_BORDER;
            text = getForeground();
            iconColor = accent != null ? accent : text;
        } else {
            fill = pressed ? color.darker() : hover ? blend(color, Color.WHITE, 0.18f) : color;
            text = getForeground();
            iconColor = text;
            if (isFocusOwner()) {
                line = UITheme.TEXT;
            }
        }
        g.setColor(fill);
        g.fill(shape);
        if (line != null) {
            g.setColor(line);
            g.setStroke(new BasicStroke(1.3f));
            g.draw(shape);
        }

        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        int iconWidth = icon == null ? 0 : ICON_SIZE + ICON_GAP;
        int contentWidth = fm.stringWidth(getText()) + iconWidth;
        int x = (w - contentWidth) / 2;
        if (icon != null) {
            icon.setColor(iconColor);
            icon.paintIcon(this, g, x, (h - ICON_SIZE) / 2);
            x += iconWidth;
        }
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g.setColor(text);
        g.drawString(getText(), x, y);
        g.dispose();
    }

    private static Color blend(Color a, Color b, float ratio) {
        float r = 1 - ratio;
        return new Color(Math.round(a.getRed() * r + b.getRed() * ratio),
                Math.round(a.getGreen() * r + b.getGreen() * ratio),
                Math.round(a.getBlue() * r + b.getBlue() * ratio));
    }
}
