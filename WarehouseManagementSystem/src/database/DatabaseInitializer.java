package database;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Prepares the database when the program starts:
 *  1. makes sure the Derby server is running and the database exists,
 *  2. on the first run, creates the 13 tables (schema.sql) and loads
 *     the sample data (user accounts, sample_data.sql, sample transactions),
 *  3. creates the 3 Customer Portal tables (customer_portal.sql) if they
 *     are missing, so older databases are upgraded automatically.
 */
public class DatabaseInitializer {

    /** Receives progress messages (shown on the splash screen). */
    public interface ProgressListener {
        void update(String message);
    }

    private DatabaseInitializer() {
    }

    public static void initialize(ProgressListener listener) throws Exception {
        report(listener, "Starting the Apache Derby database server...");
        DatabaseConnection.start();
        report(listener, "Connected to " + DatabaseConnection.getUrl());

        boolean firstRun;
        try (Connection conn = DatabaseConnection.getConnection()) {
            firstRun = !tableExists(conn, "USERS");
            if (firstRun) {
                report(listener, "First run: creating the database tables...");
                conn.setAutoCommit(false);   // tables + master data are created all-or-nothing
                try {
                    runScript(conn, "schema.sql");
                    report(listener, "Creating user accounts and master data...");
                    SampleDataSeeder.insertUsers(conn);
                    runScript(conn, "sample_data.sql");
                    conn.commit();
                } catch (SQLException | IOException | RuntimeException e) {
                    conn.rollback();
                    throw e;
                }
            }
        }
        if (firstRun) {
            report(listener, "Loading sample transactions...");
            try {
                SampleDataSeeder.insertTransactions();
            } catch (Exception e) {
                // The system still works without the sample transactions.
                System.err.println("Sample transactions could not be loaded: " + e.getMessage());
                e.printStackTrace();
            }
        }
        createCustomerPortalTables(listener);
        report(listener, "Database ready.");
    }

    /**
     * Creates the 3 Customer Portal tables (customer_portal.sql) when they do
     * not exist yet: on a new database, and once on a database created by an
     * earlier version of the program. Then loads the demo customer accounts.
     */
    private static void createCustomerPortalTables(ProgressListener listener) throws SQLException, IOException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (tableExists(conn, "CUSTOMER_ACCOUNTS")) {
                return;
            }
            report(listener, "Creating the Customer Portal tables...");
            conn.setAutoCommit(false);
            try {
                runScript(conn, "customer_portal.sql");
                conn.commit();
            } catch (SQLException | IOException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
        try {
            SampleDataSeeder.insertCustomerPortalData();
        } catch (Exception e) {
            // The portal still works without the demo accounts and requests.
            System.err.println("Sample customer accounts could not be loaded: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void report(ProgressListener listener, String message) {
        if (listener != null) {
            listener.update(message);
        }
    }

    /** Checks the database catalog for a table in the APP schema. */
    public static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, "APP", tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * Runs an SQL script stored next to this class. A plain Statement is used
     * because the script is fixed text inside the program (no user input).
     */
    static void runScript(Connection conn, String resourceName) throws IOException, SQLException {
        try (Statement statement = conn.createStatement()) {
            for (String sql : splitStatements(readResource(resourceName))) {
                statement.execute(sql);
            }
        }
    }

    private static String readResource(String resourceName) throws IOException {
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Missing SQL script: " + resourceName);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    /**
     * Splits a script into statements at ';' while ignoring "--" comments and
     * semicolons inside 'quoted text'.
     */
    static List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < script.length()) {
            char c = script.charAt(i);
            if (!inQuotes && c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;   // skip the comment until the end of the line
                }
                continue;
            }
            if (c == '\'') {
                inQuotes = !inQuotes;
            }
            if (c == ';' && !inQuotes) {
                addStatement(statements, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
            i++;
        }
        addStatement(statements, current);
        return statements;
    }

    private static void addStatement(List<String> statements, StringBuilder text) {
        String sql = text.toString().trim();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
    }
}
