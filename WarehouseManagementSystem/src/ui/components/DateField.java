package ui.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import ui.UITheme;
import util.Validator;

/**
 * A date input in the yyyy-MM-dd format with a small calendar button.
 * Dates can be typed (only digits and '-' are accepted) or picked from a
 * pop-up month calendar built with GridLayout.
 */
public class DateField extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final JTextField text = new JTextField(9);
    private final JButton btnCalendar = new JButton(new CalendarIcon());
    private final List<Runnable> changeListeners = new ArrayList<>();
    private JPopupMenu popup;
    private YearMonth shownMonth;

    public DateField() {
        this(LocalDate.now());
    }

    public DateField(LocalDate initialDate) {
        super(new BorderLayout(4, 0));
        setOpaque(false);
        text.setToolTipText("Date format: yyyy-MM-dd (example: 2026-09-28)");
        ((AbstractDocument) text.getDocument()).setDocumentFilter(new DateCharacterFilter());
        btnCalendar.setToolTipText("Pick a date from the calendar");
        btnCalendar.setFocusable(false);
        btnCalendar.setPreferredSize(new Dimension(40, 34));
        btnCalendar.setBorder(BorderFactory.createEmptyBorder());
        btnCalendar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCalendar.addActionListener(e -> showCalendar());
        add(text, BorderLayout.CENTER);
        add(btnCalendar, BorderLayout.EAST);
        setDate(initialDate);
    }

    /** The typed date, or null when the text is empty or not a valid yyyy-MM-dd date. */
    public LocalDate getDate() {
        return Validator.parseDate(text.getText());
    }

    public void setDate(LocalDate date) {
        text.setText(date == null ? "" : date.toString());
    }

    public String getText() {
        return text.getText().trim();
    }

    public boolean isEmpty() {
        return getText().isEmpty();
    }

    public JTextField getTextField() {
        return text;
    }

    /** Called when a date is picked from the calendar. */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        text.setEnabled(enabled);
        btnCalendar.setEnabled(enabled);
    }

    @Override
    public void requestFocus() {
        text.requestFocusInWindow();
    }

    private void showCalendar() {
        LocalDate current = getDate();
        shownMonth = YearMonth.from(current != null ? current : LocalDate.now());
        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        popup.add(buildCalendar(), BorderLayout.CENTER);
        popup.show(text, 0, text.getHeight());
    }

    private void changeMonth(int delta) {
        shownMonth = shownMonth.plusMonths(delta);
        popup.removeAll();
        popup.add(buildCalendar(), BorderLayout.CENTER);
        popup.pack();
        popup.revalidate();
        popup.repaint();
    }

    private JPanel buildCalendar() {
        JPanel calendar = new JPanel(new BorderLayout(0, 6));
        calendar.setBackground(UITheme.CARD);
        calendar.setBorder(new EmptyBorder(8, 8, 8, 8));

        // month title with previous / next buttons
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel previous = clickableLabel("‹", () -> changeMonth(-1));
        JLabel next = clickableLabel("›", () -> changeMonth(1));
        JLabel title = new JLabel(shownMonth.format(MONTH_TITLE), SwingConstants.CENTER);
        title.setFont(UITheme.FONT_BOLD);
        title.setForeground(UITheme.TEXT);
        top.add(previous, BorderLayout.WEST);
        top.add(title, BorderLayout.CENTER);
        top.add(next, BorderLayout.EAST);
        calendar.add(top, BorderLayout.NORTH);

        // 7 columns: Sun .. Sat, then the days of the month
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setOpaque(false);
        for (String day : new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"}) {
            JLabel header = new JLabel(day, SwingConstants.CENTER);
            header.setFont(UITheme.FONT_SMALL_BOLD);
            header.setForeground(UITheme.TEXT_MUTED);
            grid.add(header);
        }
        LocalDate first = shownMonth.atDay(1);
        int blanks = first.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : first.getDayOfWeek().getValue();
        for (int i = 0; i < blanks; i++) {
            grid.add(new JLabel(""));
        }
        LocalDate selected = getDate();
        for (int day = 1; day <= shownMonth.lengthOfMonth(); day++) {
            grid.add(dayCell(shownMonth.atDay(day), selected));
        }
        calendar.add(grid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        bottom.setOpaque(false);
        bottom.add(clickableLabel("Today", () -> pick(LocalDate.now())));
        calendar.add(bottom, BorderLayout.SOUTH);
        return calendar;
    }

    private JLabel dayCell(LocalDate date, LocalDate selected) {
        JLabel cell = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setPreferredSize(new Dimension(36, 30));
        cell.setFont(date.equals(LocalDate.now()) ? UITheme.FONT_BOLD : UITheme.FONT);
        boolean isSelected = date.equals(selected);
        Color normalBackground = isSelected ? UITheme.PRIMARY : UITheme.CARD;
        cell.setBackground(normalBackground);
        cell.setForeground(isSelected ? UITheme.textOn(UITheme.PRIMARY)
                : date.equals(LocalDate.now()) ? UITheme.PRIMARY_TEXT : UITheme.TEXT);
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                pick(date);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    cell.setBackground(UITheme.PRIMARY_LIGHT);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cell.setBackground(normalBackground);
            }
        });
        return cell;
    }

    private JLabel clickableLabel(String textValue, Runnable action) {
        JLabel label = new JLabel(textValue, SwingConstants.CENTER);
        label.setFont(UITheme.FONT_HEADING);
        label.setForeground(UITheme.PRIMARY_TEXT);
        label.setBorder(new EmptyBorder(0, 8, 0, 8));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return label;
    }

    private void pick(LocalDate date) {
        setDate(date);
        if (popup != null) {
            popup.setVisible(false);
        }
        text.requestFocusInWindow();
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    /** Only digits and dashes, at most 10 characters (yyyy-MM-dd). */
    private static final class DateCharacterFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String value, AttributeSet attrs)
                throws BadLocationException {
            String insert = value == null ? "" : value;
            if (!insert.matches("[0-9-]*") || fb.getDocument().getLength() - length + insert.length() > 10) {
                java.awt.Toolkit.getDefaultToolkit().beep();
                return;
            }
            super.replace(fb, offset, length, insert, attrs);
        }
    }

    /** A tiny calendar picture drawn with Java 2D. */
    private static final class CalendarIcon implements Icon {
        @Override
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(c.isEnabled() ? UITheme.BRAND : UITheme.TEXT_DIM);
            g.setStroke(new BasicStroke(1.3f));
            g.drawRoundRect(x + 1, y + 3, 13, 11, 3, 3);
            g.drawLine(x + 1, y + 7, x + 14, y + 7);
            g.drawLine(x + 5, y + 1, x + 5, y + 4);
            g.drawLine(x + 10, y + 1, x + 10, y + 4);
            g.fillRect(x + 4, y + 9, 2, 2);
            g.fillRect(x + 8, y + 9, 2, 2);
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
}
