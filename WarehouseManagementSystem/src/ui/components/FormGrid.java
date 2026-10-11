package ui.components;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import ui.UITheme;

/**
 * Builds forms with GridBagLayout: labels in the left column, input fields
 * in the right column, all neatly aligned. Required fields get a red *.
 * Inside a scroll pane the form always uses the full width.
 */
public class FormGrid extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    private int row;

    public FormGrid() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 64;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
    }

    public void addField(String label, JComponent field) {
        addField(label, field, false);
    }

    public void addField(String label, JComponent field, boolean required) {
        keepNarrow(field);
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.gridy = row;
        lc.anchor = GridBagConstraints.NORTHWEST;
        lc.insets = new Insets(9, 0, 4, 12);
        add(createLabel(label, required), lc);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1;
        fc.gridy = row;
        fc.weightx = 1;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.insets = new Insets(4, 0, 4, 0);
        add(field, fc);
        row++;
    }

    /** A component that spans both columns (e.g. a note or a group of buttons). */
    public void addWide(JComponent component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        add(component, c);
    }

    /** Pushes all rows to the top when the form is taller than its content. */
    public void addFiller() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.weighty = 1;
        add(new JLabel(), c);
    }

    /**
     * GridBagLayout switches every component to its minimum size when the
     * preferred sizes do not fit, which would squash the labels. Fields get a
     * small preferred width (they are stretched anyway) and labels never shrink.
     */
    public static void keepNarrow(JComponent field) {
        if (field instanceof javax.swing.JTextField && ((javax.swing.JTextField) field).getColumns() == 0) {
            ((javax.swing.JTextField) field).setColumns(8);
        } else if (field instanceof javax.swing.JComboBox) {
            Dimension size = field.getPreferredSize();
            field.setPreferredSize(new Dimension(Math.min(size.width, 140), size.height));
        } else if (field instanceof javax.swing.JScrollPane) {   // text areas keep their full height
            Dimension size = field.getPreferredSize();
            field.setPreferredSize(new Dimension(120, size.height));
            field.setMinimumSize(new Dimension(60, size.height));
        }
    }

    public static JLabel createLabel(String text, boolean required) {
        String html = "<html><nobr>" + text + (required ? " <font color='#f87171'>*</font>" : "") + "</nobr></html>";
        JLabel label = new JLabel(html);
        label.setFont(UITheme.FONT);
        label.setForeground(UITheme.TEXT);
        label.setMinimumSize(label.getPreferredSize());
        return label;
    }
}
