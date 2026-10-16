package ui;

import database.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import model.User;
import ui.components.ModuleIcon;
import ui.components.SidebarButton;
import util.Session;

/**
 * The main window after login: header, menu bar, sidebar navigation and a
 * content area that switches between the module screens with CardLayout.
 * Modules the user's role may not use are not created at all.
 */
public class DashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String DASHBOARD = "Dashboard";
    public static final String INVENTORY = "Inventory";
    public static final String RECEIVING = "Receiving";
    public static final String RELEASING = "Releasing";
    public static final String TRANSFERS = "Stock Transfers";
    public static final String PRODUCTS = "Products";
    public static final String CATEGORIES = "Categories";
    public static final String WAREHOUSES = "Warehouses";
    public static final String SUPPLIERS = "Suppliers";
    public static final String CUSTOMERS = "Customers";
    public static final String REPORTS = "Reports";
    public static final String USERS = "Users";
    public static final String REQUESTS = "Customer Requests";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, JComponent> modules = new LinkedHashMap<>();
    private final Map<String, SidebarButton> navButtons = new LinkedHashMap<>();
    private final JLabel lblClock = UITheme.mutedLabel(" ");
    private final Timer clock;
    private String currentModule;

    public DashboardFrame() {
        super(UITheme.APP_NAME);
        setIconImages(UITheme.appIcons());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {      // WindowListener: confirm before closing
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        createModules();
        setJMenuBar(createMenuBar());

        JPanel root = new JPanel(new BorderLayout());
        root.add(FrameChrome.header(UITheme.APP_SUBTITLE, lblClock, this::logout), BorderLayout.NORTH);
        root.add(createSidebar(), BorderLayout.WEST);
        content.setBackground(UITheme.BACKGROUND);
        root.add(content, BorderLayout.CENTER);
        root.add(createStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);

        // F5 refreshes the current screen
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        root.getActionMap().put("refresh", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                refreshCurrentModule();
            }
        });

        clock = new Timer(1000, e -> updateClock());   // javax.swing.Timer runs on the event thread
        clock.start();
        updateClock();

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1100, screen.width), Math.min(660, screen.height)));
        setSize(Math.min(1366, screen.width), Math.min(800, screen.height));
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);
        showModule(DASHBOARD);
    }

    // ------------------------------------------------------------------ modules

    private void createModules() {
        addModule(DASHBOARD, new DashboardPanel(this::showModule));
        addModule(INVENTORY, new InventoryPanel());
        addModule(RECEIVING, new ReceivingPanel());
        addModule(RELEASING, new ReleasingPanel());
        addModule(TRANSFERS, new TransferPanel());
        addModule(PRODUCTS, new ProductPanel());
        addModule(CATEGORIES, new CategoryPanel());
        addModule(WAREHOUSES, new WarehousePanel());
        addModule(SUPPLIERS, new SupplierPanel());
        addModule(CUSTOMERS, new CustomerPanel());
        if (Session.canProcessRequests()) {
            addModule(REQUESTS, new CustomerRequestsPanel());
        }
        if (Session.canViewReports()) {
            addModule(REPORTS, new ReportsPanel());
        }
        if (Session.canManageUsers()) {
            addModule(USERS, new UserPanel());
        }
    }

    private void addModule(String name, JComponent panel) {
        modules.put(name, panel);
        content.add(panel, name);
    }

    /** Shows a module screen and reloads its data. */
    public void showModule(String name) {
        JComponent panel = modules.get(name);
        if (panel == null) {
            return;
        }
        cardLayout.show(content, name);
        currentModule = name;
        for (Map.Entry<String, SidebarButton> entry : navButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(name));
        }
        setTitle(UITheme.APP_NAME + " - " + name);
        if (panel instanceof Refreshable) {
            ((Refreshable) panel).refreshData();
        }
    }

    public void refreshCurrentModule() {
        if (currentModule != null) {
            showModule(currentModule);
        }
    }

    public String getCurrentModule() {
        return currentModule;
    }

    public JComponent getModule(String name) {
        return modules.get(name);
    }

    // ------------------------------------------------------------------ menu bar

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(menuItem("Dashboard", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK),
                () -> showModule(DASHBOARD)));
        file.add(menuItem("Refresh Screen", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this::refreshCurrentModule));
        file.addSeparator();
        file.add(menuItem("Change Password...", null, () -> new ChangePasswordDialog(this).setVisible(true)));
        file.add(menuItem("Logout", KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), this::logout));
        file.add(menuItem("Exit", null, this::exitApplication));
        bar.add(file);

        JMenu transactions = new JMenu("Transactions");
        transactions.setMnemonic(KeyEvent.VK_T);
        transactions.add(menuItem("Receiving", null, () -> showModule(RECEIVING)));
        transactions.add(menuItem("Releasing", null, () -> showModule(RELEASING)));
        transactions.add(menuItem("Stock Transfers", null, () -> showModule(TRANSFERS)));
        if (modules.containsKey(REQUESTS)) {
            transactions.addSeparator();
            transactions.add(menuItem("Customer Requests", null, () -> showModule(REQUESTS)));
        }
        bar.add(transactions);

        JMenu inventory = new JMenu("Inventory");
        inventory.setMnemonic(KeyEvent.VK_I);
        inventory.add(menuItem("Stock on Hand", null, () -> showModule(INVENTORY)));
        bar.add(inventory);

        JMenu masterData = new JMenu("Master Data");
        masterData.setMnemonic(KeyEvent.VK_M);
        masterData.add(menuItem("Products", null, () -> showModule(PRODUCTS)));
        masterData.add(menuItem("Categories", null, () -> showModule(CATEGORIES)));
        masterData.add(menuItem("Warehouses", null, () -> showModule(WAREHOUSES)));
        masterData.add(menuItem("Suppliers", null, () -> showModule(SUPPLIERS)));
        masterData.add(menuItem("Customers", null, () -> showModule(CUSTOMERS)));
        bar.add(masterData);

        if (Session.canViewReports()) {
            JMenu reports = new JMenu("Reports");
            reports.setMnemonic(KeyEvent.VK_R);
            reports.add(menuItem("View Reports", null, () -> showModule(REPORTS)));
            bar.add(reports);
        }
        if (Session.canManageUsers()) {
            JMenu admin = new JMenu("Admin");
            admin.setMnemonic(KeyEvent.VK_A);
            admin.add(menuItem("User Management", null, () -> showModule(USERS)));
            bar.add(admin);
        }

        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        help.add(menuItem("About", null, () -> new AboutDialog(this).setVisible(true)));
        bar.add(help);
        return bar;
    }

    private static JMenuItem menuItem(String text, KeyStroke accelerator, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        item.addActionListener(e -> action.run());
        return item;
    }

    // ------------------------------------------------------------------ header, sidebar, status bar

    private JComponent createSidebar() {
        JPanel sidebar = FrameChrome.sidebarPanel();
        User user = Session.getCurrentUser();
        sidebar.add(FrameChrome.userCard(user.getFullName(), user.getRole().getDisplayName()));
        FrameChrome.section(sidebar, "MAIN");
        navButton(sidebar, DASHBOARD, ModuleIcon.Type.DASHBOARD);
        navButton(sidebar, INVENTORY, ModuleIcon.Type.INVENTORY);
        FrameChrome.section(sidebar, "TRANSACTIONS");
        navButton(sidebar, RECEIVING, ModuleIcon.Type.RECEIVING);
        navButton(sidebar, RELEASING, ModuleIcon.Type.RELEASING);
        navButton(sidebar, TRANSFERS, ModuleIcon.Type.TRANSFER);
        if (modules.containsKey(REQUESTS)) {
            navButton(sidebar, REQUESTS, ModuleIcon.Type.REQUESTS);
        }
        FrameChrome.section(sidebar, "MASTER DATA");
        navButton(sidebar, PRODUCTS, ModuleIcon.Type.PRODUCTS);
        navButton(sidebar, CATEGORIES, ModuleIcon.Type.CATEGORIES);
        navButton(sidebar, WAREHOUSES, ModuleIcon.Type.WAREHOUSES);
        navButton(sidebar, SUPPLIERS, ModuleIcon.Type.SUPPLIERS);
        navButton(sidebar, CUSTOMERS, ModuleIcon.Type.CUSTOMERS);
        if (modules.containsKey(REPORTS)) {
            FrameChrome.section(sidebar, "ANALYTICS");
            navButton(sidebar, REPORTS, ModuleIcon.Type.REPORTS);
        }
        if (modules.containsKey(USERS)) {
            FrameChrome.section(sidebar, "ADMINISTRATION");
            navButton(sidebar, USERS, ModuleIcon.Type.USERS);
        }
        return FrameChrome.sidebarScroll(sidebar);
    }

    private void navButton(JPanel sidebar, String module, ModuleIcon.Type icon) {
        SidebarButton button = new SidebarButton(module, icon);
        button.addActionListener(e -> showModule(module));
        navButtons.put(module, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(2));
    }

    private JComponent createStatusBar() {
        User user = Session.getCurrentUser();
        return FrameChrome.statusBar("Logged in as " + user.getUsername() + " (" + user.getRole().getDisplayName()
                + ") since " + Session.getLoginTime().format(DateTimeFormatter.ofPattern("hh:mm a")));
    }

    private void updateClock() {
        lblClock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy   hh:mm:ss a")));
    }

    // ------------------------------------------------------------------ logout and exit

    /** Ends the session and shows the login screen again. */
    public void logout() {
        if (!Dialogs.confirm(this, "Do you want to log out?", "Logout")) {
            return;
        }
        clock.stop();
        Session.end();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void exitApplication() {
        if (Dialogs.confirm(this, "Exit " + UITheme.APP_NAME + "?", "Exit")) {
            clock.stop();
            dispose();
            DatabaseConnection.shutdown();
            System.exit(0);
        }
    }
}
