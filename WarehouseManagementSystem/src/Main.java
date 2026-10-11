import database.DatabaseConnection;
import javax.swing.SwingUtilities;
import ui.SplashScreen;
import ui.UITheme;
import util.ErrorHandler;

/**
 * I.O.T Warehouse (Warehouse Management System) - program entry point.
 *
 * Start-up sequence: install the error handler and the look and feel, show
 * the splash screen (which starts the Apache Derby database), then the login
 * screen. When the program ends, the database is shut down cleanly.
 */
public class Main {

    public static void main(String[] args) {
        ErrorHandler.installGlobalHandler();
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::shutdown, "database-shutdown"));
        SwingUtilities.invokeLater(() -> {
            UITheme.install();
            new SplashScreen().start();
        });
    }
}
