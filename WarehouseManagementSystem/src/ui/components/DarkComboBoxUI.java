package ui.components;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import ui.UITheme;

/**
 * Combo box painted with Java 2D: a white rounded field with a chevron, an
 * amber outline while it has the focus, and a white drop-down list.
 * Registered for every JComboBox in UITheme.install().
 */
public class DarkComboBoxUI extends BasicComboBoxUI {

    private static final int ARC = 10;

    public static ComponentUI createUI(JComponent c) {
        return new DarkComboBoxUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        comboBox.setOpaque(false);
        comboBox.setBorder(new EmptyBorder(5, 10, 5, 4));
        comboBox.setFont(UITheme.FONT);
    }

    @Override
    protected JButton createArrowButton() {
        JButton arrow = new JButton() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(comboBox.isEnabled() ? UITheme.TEXT : UITheme.TEXT_DIM);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2 - 2;
                int cy = getHeight() / 2;
                Path2D chevron = new Path2D.Float();
                chevron.moveTo(cx - 5, cy - 2.5);
                chevron.lineTo(cx, cy + 2.5);
                chevron.lineTo(cx + 5, cy - 2.5);
                g.draw(chevron);
                g.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(30, 20);
            }
        };
        arrow.setBorder(BorderFactory.createEmptyBorder());
        arrow.setContentAreaFilled(false);
        arrow.setOpaque(false);
        arrow.setFocusable(false);
        arrow.setName("ComboBox.arrowButton");
        return arrow;
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox);
        popup.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        return popup;
    }

    @Override
    public void paint(Graphics graphics, JComponent c) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D shape = new RoundRectangle2D.Float(0.8f, 0.8f, c.getWidth() - 2.6f, c.getHeight() - 2.6f, ARC, ARC);
        g.setColor(comboBox.isEnabled() ? UITheme.INPUT : UITheme.INPUT_DISABLED);
        g.fill(shape);
        boolean focused = comboBox.hasFocus() || isPopupVisible(comboBox);
        g.setColor(focused ? UITheme.BRAND : comboBox.isEnabled() ? UITheme.INPUT_BORDER : UITheme.BORDER);
        g.setStroke(new BasicStroke(focused ? 1.6f : 1.2f));
        g.draw(shape);
        g.dispose();
        if (!comboBox.isEditable()) {
            paintCurrentValue(graphics, rectangleForCurrentValue(), false);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        ListCellRenderer<Object> renderer = (ListCellRenderer<Object>) comboBox.getRenderer();
        Object value = comboBox.getSelectedItem();
        Component cell = renderer.getListCellRendererComponent(listBox, value, -1, false, false);
        cell.setFont(comboBox.getFont());
        cell.setBackground(UITheme.INPUT);
        if (value != null || !comboBox.isEnabled()) {
            cell.setForeground(comboBox.isEnabled() ? UITheme.TEXT : UITheme.TEXT_DIM);
        }
        boolean wasOpaque = cell instanceof JComponent && ((JComponent) cell).isOpaque();
        if (cell instanceof JComponent) {
            JComponent component = (JComponent) cell;
            component.setOpaque(false);
            component.setBorder(BorderFactory.createEmptyBorder());
        }
        currentValuePane.paintComponent(g, cell, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, true);
        if (cell instanceof JComponent) {
            ((JComponent) cell).setOpaque(wasOpaque);   // the same renderer also paints the drop-down list
        }
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // the rounded background is painted in paint()
    }
}
