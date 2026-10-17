package ui;

import database.DatabaseConnection;
import database.DatabaseInitializer;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import util.ErrorHandler;

/**
 * Start-up window. Connects to the Apache Derby database in a background
 * thread (SwingWorker) and shows the progress; then opens the login screen.
 */
public class SplashScreen extends JWindow {

    private static final long serialVersionUID = 1L;

    private final JLabel lblStatus = new JLabel("Starting...");
    private final JProgressBar progress = new JProgressBar();

    public SplashScreen() {
        setIconImages(UITheme.appIcons());
        LoginFrame.BrandPanel background = new LoginFrame.BrandPanel();
        background.removeAll();
        background.setLayout(new BorderLayout());
        background.setBorder(new EmptyBorder(28, 32, 24, 32));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 14, 0);
        center.add(UITheme.logoLabel(150), c);
        JLabel name = new JLabel(UITheme.APP_SUBTITLE);
        name.setFont(UITheme.FONT_TITLE.deriveFont(24f));
        name.setForeground(UITheme.BRAND);
        c.gridy++;
        c.insets = new Insets(0, 0, 4, 0);
        center.add(name, c);
        JLabel subtitle = new JLabel("Warehouses  |  Products  |  Receiving  |  Releasing  |  Stock Transfers");
        subtitle.setFont(UITheme.FONT);
        subtitle.setForeground(UITheme.CHROME_TEXT);
        c.gridy++;
        center.add(subtitle, c);
        background.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.CHROME_MUTED);
        progress.setIndeterminate(true);
        progress.setBorderPainted(false);
        progress.setForeground(UITheme.BRAND);
        progress.setBackground(UITheme.CHROME_BORDER);
        progress.setPreferredSize(new java.awt.Dimension(10, 6));
        bottom.add(lblStatus, BorderLayout.NORTH);
        bottom.add(progress, BorderLayout.SOUTH);
        background.add(bottom, BorderLayout.SOUTH);

        setContentPane(background);
        setSize(700, 420);
        setLocationRelativeTo(null);
    }

    /** Shows the splash screen and prepares the database. */
    public void start() {
        setVisible(true);
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                DatabaseInitializer.initialize(message -> publish(message));
                publish("Connected to " + DatabaseConnection.getUrl());
                Thread.sleep(600);   // let the user read the connection message
                return null;
            }

            @Override
            protected void process(List<String> messages) {
                lblStatus.setText(messages.get(messages.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    get();
                    dispose();
                    new LoginFrame().setVisible(true);
                } catch (ExecutionException e) {
                    showStartupError(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void showStartupError(Throwable cause) {
        ErrorHandler.log(cause);
        String friendly = cause instanceof SQLException
                ? ErrorHandler.friendlyMessage((SQLException) cause, "start the database") : "";
        String message = "The database could not be started.\n\n"
                + (friendly.isEmpty() ? "" : friendly + "\n\n")
                + "Details: " + cause.getMessage();
        int choice = JOptionPane.showOptionDialog(this, message, "Database Connection Error",
                JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, new String[]{"Retry", "Exit"}, "Retry");
        if (choice == 0) {
            lblStatus.setText("Retrying...");
            start();
        } else {
            dispose();
            System.exit(1);
        }
    }
}
