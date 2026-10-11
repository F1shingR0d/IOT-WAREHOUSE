package ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import ui.UITheme;

/**
 * A dashboard card showing one number: a title, a big value and a caption,
 * with a colored accent bar on the left.
 */
public class StatCard extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Color accent;
    private final JLabel lblValue = new JLabel("-");
    private final JLabel lblCaption = new JLabel(" ");

    public StatCard(String title, Color accent) {
        super(new BorderLayout());
        this.accent = accent;
        setOpaque(false);
        setBorder(new EmptyBorder(14, 20, 14, 16));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(UITheme.FONT_SECTION);
        lblTitle.setForeground(UITheme.TEXT_MUTED);
        lblValue.setFont(UITheme.FONT_STAT);
        lblValue.setForeground(UITheme.TEXT);
        lblCaption.setFont(UITheme.FONT_SMALL);
        lblCaption.setForeground(UITheme.TEXT_MUTED);
        texts.add(lblTitle);
        texts.add(javax.swing.Box.createVerticalStrut(4));
        texts.add(lblValue);
        texts.add(javax.swing.Box.createVerticalStrut(2));
        texts.add(lblCaption);
        add(texts, BorderLayout.CENTER);
    }

    public void setValue(String value, String caption) {
        lblValue.setText(value);
        lblCaption.setText(caption);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D card = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1.5f, getHeight() - 1.5f, 12, 12);
        g.setColor(UITheme.CARD);
        g.fill(card);
        g.setColor(UITheme.CARD_BORDER);
        g.draw(card);
        g.setColor(accent);
        g.fill(new RoundRectangle2D.Float(0, 10, 4, getHeight() - 20, 4, 4));
        g.dispose();
        super.paintComponent(graphics);
    }
}
