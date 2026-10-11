package util;

import database.DatabaseConnection;
import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDateTime;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Turns errors into clear messages for the user instead of crashing the
 * program. Technical details are written to data/error.log.
 */
public final class ErrorHandler {

    private ErrorHandler() {
    }

    /**
     * Converts a database error (identified by its SQLState code) into a
     * message the user can understand.
     *
     * @param action what the user was doing, e.g. "delete the product"
     */
    public static String friendlyMessage(SQLException e, String action) {
        String state = findSqlState(e);
        switch (state) {
            case "23505":
                return "Duplicate record: another record already uses this value.\n"
                        + "Please enter a unique value (for example a different code, name or username).";
            case "23503":
                return action.startsWith("delete")
                        ? "This record cannot be deleted because other records are linked to it\n"
                          + "(for example products, stock or transactions).\n\n"
                          + "Tip: set its status to INACTIVE instead."
                        : "A related record no longer exists. Please refresh the screen and try again.";
            case "23513":
                return "The data breaks a database rule (for example a negative quantity or\n"
                        + "a transfer to the same warehouse). Please check the values.";
            case "23502":
                return "A required value is missing. Please fill in all required fields.";
            case "22001":
                return "One of the values is too long for its field. Please shorten it.";
            case "22003":
                return "A number is too large. Please enter a smaller value.";
            case "40001":
            case "40XL1":
            case "40XL2":
                return "The database is busy right now. Please try again in a moment.";
            default:
                if (state.startsWith("08") || state.startsWith("58")) {
                    return "Cannot connect to the database.\n"
                            + "Please try again. If the problem continues, restart the program.";
                }
                return "A database error occurred while trying to " + action + ".\n\nDetails: " + e.getMessage();
        }
    }

    /** Derby may wrap the real error; prefer a constraint/data error code (23xxx / 22xxx) in the chain. */
    private static String findSqlState(SQLException e) {
        String first = null;
        for (SQLException s = e; s != null; s = s.getNextException()) {
            String state = s.getSQLState();
            if (state == null) {
                continue;
            }
            if (state.startsWith("23") || state.startsWith("22")) {
                return state;
            }
            if (first == null) {
                first = state;
            }
        }
        return first == null ? "" : first;
    }

    public static void showDatabaseError(Component parent, SQLException e, String action) {
        String state = findSqlState(e);
        if (!state.startsWith("23") && !state.startsWith("22")) {
            log(e);   // constraint messages are expected; only real problems are logged
        }
        JOptionPane.showMessageDialog(parent, friendlyMessage(e, action), "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }

    /** Shows a business-rule message (not enough stock, capacity full, ...). */
    public static void showRuleViolation(Component parent, BusinessRuleException e) {
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Cannot Continue", JOptionPane.WARNING_MESSAGE);
    }

    public static void showUnexpectedError(Component parent, Throwable t) {
        log(t);
        JOptionPane.showMessageDialog(parent,
                "An unexpected error occurred:\n" + t + "\n\nThe program will keep running.\n"
                + "Details were saved to data/error.log.",
                "Unexpected Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Any error that nobody caught is shown in a dialog instead of crashing the program. */
    public static void installGlobalHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                SwingUtilities.invokeLater(() -> showUnexpectedError(null, error)));
    }

    /** Appends the error details to data/error.log (and the console). */
    public static synchronized void log(Throwable t) {
        t.printStackTrace();
        File dir = DatabaseConnection.getDataDirectory();
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(new File(dir, "error.log"), true))) {
            out.println("=== " + LocalDateTime.now() + " ===");
            t.printStackTrace(out);
        } catch (IOException ignored) {
            // logging must never cause another error
        }
    }
}
