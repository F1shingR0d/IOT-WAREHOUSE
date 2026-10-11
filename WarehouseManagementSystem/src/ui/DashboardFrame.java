package ui;

import database.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import model.User;
import ui.components.FlatButton;
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
        root.add(createHeader(), BorderLayout.NORTH);
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

    private JComponent createHeader() {
        // black bar with an amber line underneath that fades out to the right
        JPanel header = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                int y = getHeight() - 3;
                g.setPaint(new java.awt.GradientPaint(0, 0, UITheme.BRAND, getWidth(), 0,
                        new Color(251, 171, 6, 60)));
                g.fillRect(0, y, getWidth(), 3);
                g.dispose();
            }
        };
        header.setBackground(UITheme.BACKGROUND);
        header.setBorder(new EmptyBorder(6, 22, 9, 18));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(UITheme.logoLabel(64));
        JComponent divider = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(UITheme.BRAND);
                g.fillRect(getWidth() / 2 - 1, 8, 2, getHeight() - 16);
            }
        };
        divider.setPreferredSize(new Dimension(44, 64));
        brand.add(divider);
        JLabel title = new JLabel(UITheme.APP_SUBTITLE.toUpperCase());
        title.setFont(new java.awt.Font(UITheme.FONT_NAME, java.awt.Font.BOLD, 30));
        title.setForeground(UITheme.BRAND);
        title.setPreferredSize(new Dimension(title.getPreferredSize().width, 64));
        brand.add(title);
        header.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        right.setOpaque(false);
        lblClock.setFont(UITheme.FONT_NAV_BOLD);
        lblClock.setForeground(UITheme.TEXT);
        lblClock.setIcon(new ModuleIcon(ModuleIcon.Type.CALENDAR, 24, UITheme.BRAND));
        lblClock.setIconTextGap(10);
        right.add(lblClock);
        right.add(Box.createHorizontalStrut(6));
        FlatButton btnLogout = new FlatButton("Logout", UITheme.BRAND, ModuleIcon.Type.USER);
        btnLogout.addActionListener(e -> logout());
        right.add(btnLogout);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent createSidebar() {
        JPanel sidebar = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                paintStripes((Graphics2D) graphics.create(), getWidth(), getHeight());
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.SIDEBAR);
        sidebar.setBorder(new EmptyBorder(16, 12, 70, 12));   // room for the amber stripes at the bottom

        sidebar.add(createUserCard());
        section(sidebar, "MAIN");
        navButton(sidebar, DASHBOARD, ModuleIcon.Type.DASHBOARD);
        navButton(sidebar, INVENTORY, ModuleIcon.Type.INVENTORY);
        section(sidebar, "TRANSACTIONS");
        navButton(sidebar, RECEIVING, ModuleIcon.Type.RECEIVING);
        navButton(sidebar, RELEASING, ModuleIcon.Type.RELEASING);
        navButton(sidebar, TRANSFERS, ModuleIcon.Type.TRANSFER);
        section(sidebar, "MASTER DATA");
        navButton(sidebar, PRODUCTS, ModuleIcon.Type.PRODUCTS);
        navButton(sidebar, CATEGORIES, ModuleIcon.Type.CATEGORIES);
        navButton(sidebar, WAREHOUSES, ModuleIcon.Type.WAREHOUSES);
        navButton(sidebar, SUPPLIERS, ModuleIcon.Type.SUPPLIERS);
        navButton(sidebar, CUSTOMERS, ModuleIcon.Type.CUSTOMERS);
        if (modules.containsKey(REPORTS)) {
            section(sidebar, "ANALYTICS");
            navButton(sidebar, REPORTS, ModuleIcon.Type.REPORTS);
        }
        if (modules.containsKey(USERS)) {
            section(sidebar, "ADMINISTRATION");
            navButton(sidebar, USERS, ModuleIcon.Type.USERS);
        }
        sidebar.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(sidebar, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new MatteBorder(0, 0, 0, 1, new Color(70, 52, 22)));
        scroll.getViewport().setBackground(UITheme.SIDEBAR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(262, 100));
        return scroll;
    }

    /** Two diagonal amber bands in the bottom-left corner of the sidebar (decoration). */
    private static void paintStripes(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[][] bands = {{0, 22, 150}, {0, 52, 230}};   // {x, height above the bottom, width}
        g.setColor(new Color(251, 171, 6, 210));
        for (int[] band : bands) {
            int top = height - band[1];
            java.awt.geom.Path2D shape = new java.awt.geom.Path2D.Float();
            shape.moveTo(band[0], top + 16);
            shape.lineTo(band[2], top - 14);
            shape.lineTo(band[2] + 26, top - 14);
            shape.lineTo(band[0], top + 34);
            shape.closePath();
            g.fill(shape);
        }
        g.dispose();
    }

    private JComponent createUserCard() {
        User user = Session.getCurrentUser();
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(new EmptyBorder(0, 6, 10, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        String initials = initialsOf(user.getFullName());
        JComponent avatar = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(UITheme.BRAND);
                g.fillOval(0, 0, 46, 46);
                g.setColor(Color.BLACK);
                g.setFont(UITheme.FONT_BOLD.deriveFont(18f));
                int w = g.getFontMetrics().stringWidth(initials);
                g.drawString(initials, 23 - w / 2, 23 + g.getFontMetrics().getAscent() / 2 - 3);
                g.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(46, 46));
        card.add(avatar, BorderLayout.WEST);

        JLabel name = new JLabel("<html><b>" + escape(user.getFullName()) + "</b><br>"
                + "<span style='color:#eef1f4; font-size:12px'>" + user.getRole().getDisplayName() + "</span></html>");
        name.setFont(UITheme.FONT_BOLD);
        name.setForeground(UITheme.BRAND);
        card.add(name, BorderLayout.CENTER);
        return card;
    }

    private void section(JPanel sidebar, String title) {
        JLabel label = new JLabel(title);
        label.setFont(UITheme.FONT_SECTION);
        label.setForeground(UITheme.SIDEBAR_SECTION);
        label.setBorder(new EmptyBorder(12, 8, 4, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(label);
    }

    private void navButton(JPanel sidebar, String module, ModuleIcon.Type icon) {
        SidebarButton button = new SidebarButton(module, icon);
        button.addActionListener(e -> showModule(module));
        navButtons.put(module, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(2));
    }

    private JComponent createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(UITheme.SIDEBAR);
        status.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, UITheme.BORDER), new EmptyBorder(5, 16, 5, 16)));
        JLabel db = new JLabel("<html><span style='color:#4ac868'>&#9679;</span>&nbsp; Connected to Apache Derby"
                + " &nbsp;|&nbsp; " + DatabaseConnection.getUrl() + "</html>");
        db.setFont(UITheme.FONT_SMALL);
        db.setForeground(UITheme.TEXT_MUTED);
        status.add(db, BorderLayout.WEST);
        User user = Session.getCurrentUser();
        JLabel who = UITheme.mutedLabel("Logged in as " + user.getUsername() + " (" + user.getRole().getDisplayName()
                + ") since " + Session.getLoginTime().format(DateTimeFormatter.ofPattern("hh:mm a")));
        status.add(who, BorderLayout.EAST);
        return status;
    }

    private void updateClock() {
        lblClock.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy   hh:mm:ss a")));
    }

    private static String initialsOf(String fullName) {
        StringBuilder initials = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.length() == 0 ? "?" : initials.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
