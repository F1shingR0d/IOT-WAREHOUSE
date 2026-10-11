package database;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.derby.drda.NetworkServerControl;

/**
 * The single place where the program gets its JDBC connections.
 *
 * The system uses the Apache Derby Network Server, the same set-up NetBeans
 * uses for "Java DB": jdbc:derby://localhost:1527/WarehouseDB (user app/app).
 * If no Derby server is running yet, start() launches one inside this program
 * and keeps the database files in the "data" folder of the project. While the
 * program runs, the tables can also be opened from NetBeans
 * (Services &gt; Databases) with the same URL.
 */
public class DatabaseConnection {

    private static final String HOST = "localhost";
    private static final int PORT = Integer.getInteger("wms.db.port", 1527);
    private static final String DATABASE_NAME = System.getProperty("wms.db.name", "WarehouseDB");

    private static final String URL = "jdbc:derby://" + HOST + ":" + PORT + "/" + DATABASE_NAME;
    private static final String USER = "app";
    private static final String PASSWORD = "app";

    /** Folder that holds the Derby database files when this program starts the server. */
    private static final File DATA_DIR =
            new File(System.getProperty("wms.data.dir", "data")).getAbsoluteFile();

    /** Not null only when THIS program started the Derby Network Server. */
    private static NetworkServerControl server;

    private DatabaseConnection() {
    }

    /**
     * Returns a new connection to the database. Always close it
     * (try-with-resources) after use.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Makes sure a Derby Network Server is running on localhost:1527 and that
     * the database exists. Called once when the program starts.
     */
    public static synchronized void start() throws Exception {
        if (!isServerRunning()) {
            startServer();
        }
        // ";create=true" creates the database on the first run; afterwards it simply connects.
        try (Connection conn = DriverManager.getConnection(URL + ";create=true", USER, PASSWORD)) {
            conn.getMetaData(); // connection test
        }
    }

    /** true if a Derby Network Server answers on the configured host and port. */
    public static boolean isServerRunning() {
        try {
            NetworkServerControl probe = new NetworkServerControl(InetAddress.getByName(HOST), PORT);
            probe.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void startServer() throws Exception {
        // Another program may already be using the port.
        try (ServerSocket test = new ServerSocket(PORT, 1, InetAddress.getByName(HOST))) {
            test.setReuseAddress(true);
        } catch (IOException e) {
            throw new SQLException("Port " + PORT + " is already used by another program, "
                    + "so the Derby database server cannot start.", "08001", e);
        }
        if (!DATA_DIR.exists() && !DATA_DIR.mkdirs()) {
            throw new IOException("Cannot create the database folder " + DATA_DIR);
        }
        // Must be set before the Derby engine boots.
        System.setProperty("derby.system.home", DATA_DIR.getPath());
        System.setProperty("derby.stream.error.file", new File(DATA_DIR, "derby.log").getPath());

        server = new NetworkServerControl(InetAddress.getByName(HOST), PORT);
        server.start(null); // null = no console messages

        // Wait until the server answers (at most about 15 seconds).
        for (int attempt = 0; attempt < 75; attempt++) {
            try {
                server.ping();
                return;
            } catch (Exception notReadyYet) {
                Thread.sleep(200);
            }
        }
        throw new SQLException("The Derby Network Server did not start on port " + PORT + ".", "08001");
    }

    /** true when the Derby server runs inside this program. */
    public static boolean isServerStartedByApp() {
        return server != null;
    }

    /**
     * Stops the Derby server and engine cleanly when the program exits.
     * A server started by someone else (for example NetBeans) is left running.
     */
    public static synchronized void shutdown() {
        if (server == null) {
            return;
        }
        try {
            server.shutdown();
        } catch (Exception ignored) {
            // the server is already stopped
        }
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException expected) {
            // Derby always reports a successful engine shutdown as SQLState XJ015.
        }
        server = null;
    }

    public static String getUrl() {
        return URL;
    }

    public static String getDatabaseName() {
        return DATABASE_NAME;
    }

    public static int getPort() {
        return PORT;
    }

    public static File getDataDirectory() {
        return DATA_DIR;
    }
}
