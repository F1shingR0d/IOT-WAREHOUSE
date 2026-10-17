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
import model.CustomerAccount;
import model.Warehouse;
import ui.components.ModuleIcon;
import ui.components.SidebarButton;
import util.Session;

/**
 * The Customer Portal: the main window of a logged-in customer. Same layout
 * as the staff dashboard (header, menu bar, sidebar, CardLayout content),
 * but with the customer screens only: home, product catalog, new request,
 * request history and profile. A customer only ever sees his or her own data.
 */
public class CustomerDashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final String HOME = "Home";
    public static final String CATALOG = "Product Catalog";
    public static final String NEW_REQUEST = "New Request";
    public static final String MY_REQUESTS = "My Requests";
    public static final String PROFILE = "My Profile";

    private static final String TITLE = UITheme.APP_NAME + " - Customer Portal";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final Map<String, JComponent> modules = new LinkedHashMap<>();
    private final Map<String, SidebarButton> navButtons = new LinkedHashMap<>();
    private final CustomerNewRequestPanel newRequestPanel = new CustomerNewRequestPanel();
    private final JLabel lblClock = UITheme.mutedLabel(" ");
    private final Timer clock;
    private String currentModule;

    public CustomerDashboardFrame() {
        super(TITLE);
        setIconImages(UITheme.appIcons());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {      // WindowListener: confirm before closing
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        addModule(HOME, new CustomerHomePanel(this));
        addModule(CATALOG, new CustomerCatalogPanel(this));
        addModule(NEW_REQUEST, newRequestPanel);
        addModule(MY_REQUESTS, new CustomerRequestHistoryPanel());
        addModule(PROFILE, new CustomerProfilePanel());
        setJMenuBar(createMenuBar());

        JPanel root = new JPanel(new BorderLayout());
        root.add(FrameChrome.header("Customer Portal", lblClock, this::logout), BorderLayout.NORTH);
        root.add(createSidebar(), BorderLayout.WEST);
        content.setBackground(UITheme.BACKGROUND);
        root.add(content, BorderLayout.CENTER);
        CustomerAccount account = Session.getCurrentCustomer();
        root.add(FrameChrome.statusBar("Logged in as " + account.getUsername() + " (Customer) since "
                + Session.getLoginTime().format(DateTimeFormatter.ofPattern("hh:mm a"))), BorderLayout.SOUTH);
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

        clock = new Timer(1000, e -> updateClock());
        clock.start();
        updateClock();

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1100, screen.width), Math.min(660, screen.height)));
        setSize(Math.min(1366, screen.width), Math.min(800, screen.height));
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);
        showModule(HOME);
    }

    private void addModule(String name, JComponent panel) {
        modules.put(name, panel);
        content.add(panel, name);
    }

    /** Shows a screen and reloads its data. */
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
        setTitle(TITLE + " - " + name);
        if (panel instanceof Refreshable) {
            ((Refreshable) panel).refreshData();
        }
    }

    public void refreshCurrentModule() {
        if (currentModule != null) {
            showModule(currentModule);
        }
    }

    /** Opens New Request with a product from the catalog already selected. */
    public void requestProduct(Warehouse warehouse, int productId) {
        showModule(NEW_REQUEST);
        newRequestPanel.preselect(warehouse, productId);
    }

    // ------------------------------------------------------------------ menu bar and sidebar

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        file.add(menuItem("Home", KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK),
                () -> showModule(HOME)));
        file.add(menuItem("Refresh Screen", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), this::refreshCurrentModule));
        file.addSeparator();
        file.add(menuItem("Change Password...", null, () -> new ChangePasswordDialog(this).setVisible(true)));
        file.add(menuItem("Logout", KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), this::logout));
        file.add(menuItem("Exit", null, this::exitApplication));
        bar.add(file);

        JMenu requests = new JMenu("Requests");
        requests.setMnemonic(KeyEvent.VK_R);
        requests.add(menuItem("Product Catalog", null, () -> showModule(CATALOG)));
        requests.add(menuItem("New Request", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK),
                () -> showModule(NEW_REQUEST)));
        requests.add(menuItem("My Requests", null, () -> showModule(MY_REQUESTS)));
        bar.add(requests);

        JMenu account = new JMenu("Account");
        account.setMnemonic(KeyEvent.VK_A);
        account.add(menuItem("My Profile", null, () -> showModule(PROFILE)));
        bar.add(account);

        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        help.add(menuItem("About", null, () -> new AboutDialog(this).setVisible(true)));
        bar.add(help);
        FrameChrome.styleMenuBar(bar);
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

    private JComponent createSidebar() {
        JPanel sidebar = FrameChrome.sidebarPanel();
        CustomerAccount account = Session.getCurrentCustomer();
        sidebar.add(FrameChrome.userCard(account.getCustomerName(), "Customer  |  " + account.getUsername()));
        FrameChrome.section(sidebar, "CUSTOMER PORTAL");
        navButton(sidebar, HOME, ModuleIcon.Type.DASHBOARD);
        navButton(sidebar, CATALOG, ModuleIcon.Type.PRODUCTS);
        FrameChrome.section(sidebar, "STOCK REQUESTS");
        navButton(sidebar, NEW_REQUEST, ModuleIcon.Type.ADD);
        navButton(sidebar, MY_REQUESTS, ModuleIcon.Type.REQUESTS);
        FrameChrome.section(sidebar, "ACCOUNT");
        navButton(sidebar, PROFILE, ModuleIcon.Type.CUSTOMERS);
        return FrameChrome.sidebarScroll(sidebar);
    }

    private void navButton(JPanel sidebar, String module, ModuleIcon.Type icon) {
        SidebarButton button = new SidebarButton(module, icon);
        button.addActionListener(e -> showModule(module));
        navButtons.put(module, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(2));
    }

    private void updateClock() {
        lblClock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy   hh:mm:ss a")));
    }

    // ------------------------------------------------------------------ logout and exit

    /** Ends the customer session and shows the login screen again. */
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
