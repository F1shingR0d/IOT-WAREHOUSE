package ui.components;

import java.awt.Color;
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
 * Thin scroll bar with a rounded thumb and no arrow buttons.
 * Registered for every JScrollBar in UITheme.install(); the sidebar uses the
 * dark version (chrome()).
 */
public class DarkScrollBarUI extends BasicScrollBarUI {

    private final Color track;
    private final Color thumb;
    private final Color thumbHover;

    public DarkScrollBarUI() {
        this(UITheme.CARD, UITheme.BUTTON_BORDER, UITheme.TEXT_DIM);
    }

    private DarkScrollBarUI(Color track, Color thumb, Color thumbHover) {
        this.track = track;
        this.thumb = thumb;
        this.thumbHover = thumbHover;
    }

    public static ComponentUI createUI(JComponent c) {
        return new DarkScrollBarUI();
    }

    /** Scroll bar for the dark sidebar. */
    public static DarkScrollBarUI chrome() {
        return new DarkScrollBarUI(UITheme.SIDEBAR, new Color(52, 60, 70), new Color(84, 94, 106));
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor = thumb;
        trackColor = track;
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
        g.setColor(track);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent c, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isThumbRollover() || isDragging ? thumbHover : thumb);
        int inset = 3;
        g.fill(new RoundRectangle2D.Float(bounds.x + inset, bounds.y + inset,
                bounds.width - inset * 2f, bounds.height - inset * 2f, 8, 8));
        g.dispose();
    }
}
