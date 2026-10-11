package ui;

import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

/**
 * Consistent message boxes (JOptionPane) for the whole program.
 */
public final class Dialogs {

    private Dialogs() {
    }

    public static void success(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Validation message; moves the cursor to the field that needs fixing. */
    public static void invalid(Component parent, String message, JComponent fieldToFix) {
        JOptionPane.showMessageDialog(parent, message, "Please Check Your Input", JOptionPane.WARNING_MESSAGE);
        if (fieldToFix != null) {
            fieldToFix.requestFocusInWindow();
        }
    }

    public static void warning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Yes/No question; returns true for Yes. */
    public static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Confirmation before deleting; nothing is deleted without it. */
    public static boolean confirmDelete(Component parent, String recordDescription) {
        return JOptionPane.showConfirmDialog(parent,
                "Are you sure you want to delete this record?\n\n" + recordDescription
                + "\n\nThis cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
