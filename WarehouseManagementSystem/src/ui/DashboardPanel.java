package ui;

import dao.DashboardDAO;
import dao.WarehouseDAO;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.DashboardStats;
import ui.components.BarChartPanel;
import ui.components.FlatButton;
import ui.components.ModuleIcon;
import ui.components.RoundedPanel;
import ui.components.StatCard;
import util.ErrorHandler;
import util.Formats;
import util.Session;
import util.TableUtil;

/**
 * The home screen: summary cards, a stock-vs-capacity chart, the latest
 * transactions and low-stock alerts, plus shortcuts to the common tasks.
 */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();

    private final StatCard cardWarehouses = new StatCard("Active Warehouses", UITheme.PRIMARY);
    private final StatCard cardProducts = new StatCard("Active Products", UITheme.CHARCOAL);
    private final StatCard cardUnits = new StatCard("Units on Hand", UITheme.INFO);
    private final StatCard cardValue = new StatCard("Stock Value", UITheme.SUCCESS);
    private final StatCard cardLowStock = new StatCard("Low-Stock Products", UITheme.DANGER);
    private final StatCard cardReceiving = new StatCard("Today's Receiving", UITheme.SUCCESS);
    private final StatCard cardReleasing = new StatCard("Today's Releasing", UITheme.WARNING);
    private final StatCard cardTransfers = new StatCard("Today's Transfers", UITheme.CHARCOAL);

    private final BarChartPanel chart = new BarChartPanel();
    private final DefaultTableModel recentModel = TableUtil.createModel(
            new String[]{"Date", "Reference", "Type", "Details", "Units", "Status"},
            new Class<?>[]{String.class, String.class, String.class, String.class, Integer.class, String.class});
    private final DefaultTableModel lowStockModel = TableUtil.createModel(
            new String[]{"SKU", "Product", "On Hand", "Reorder Level"},
            new Class<?>[]{String.class, String.class, Integer.class, Integer.class});
    private final JTabbedPane activityTabs = new JTabbedPane();
    private final javax.swing.JLabel lblWelcome = UITheme.mutedLabel(" ");
    /** Shown only when customer requests or registrations are waiting (admins and managers). */
    private final RoundedPanel requestBanner = new RoundedPanel(new BorderLayout(14, 0));
    private final javax.swing.JLabel lblBanner = new javax.swing.JLabel(" ");

    public DashboardPanel(Consumer<String> navigator) {
        super(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        FlatButton btnReceive = new FlatButton("Receive Stock", UITheme.SUCCESS, ModuleIcon.Type.RECEIVING);
        FlatButton btnRelease = new FlatButton("Release Stock", UITheme.WARNING, ModuleIcon.Type.RELEASING);
        FlatButton btnTransfer = new FlatButton("Transfer Stock", UITheme.PRIMARY, ModuleIcon.Type.TRANSFER);
        FlatButton btnRefresh = FlatButton.outline("Refresh", ModuleIcon.Type.CLEAR);
        btnReceive.addActionListener(e -> navigator.accept(DashboardFrame.RECEIVING));
        btnRelease.addActionListener(e -> navigator.accept(DashboardFrame.RELEASING));
        btnTransfer.addActionListener(e -> navigator.accept(DashboardFrame.TRANSFERS));
        btnRefresh.addActionListener(e -> refreshData());

        lblWelcome.setFont(UITheme.FONT);
        add(UITheme.pageHeader("Dashboard", lblWelcome, btnReceive, btnRelease, btnTransfer, btnRefresh),
                BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 4, 16, 16));
        cards.setOpaque(false);
        cards.add(cardWarehouses);
        cards.add(cardProducts);
        cards.add(cardUnits);
        cards.add(cardValue);
        cards.add(cardLowStock);
        cards.add(cardReceiving);
        cards.add(cardReleasing);
        cards.add(cardTransfers);

        // chart card (left) and activity card (right)
        RoundedPanel chartCard = new RoundedPanel(new BorderLayout(0, 12));
        chartCard.add(UITheme.headingLabel("Stock vs Capacity by Warehouse"), BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);

        JTable recentTable = TableUtil.createTable(recentModel);
        TableUtil.setColumnWidths(recentTable, 92, 132, 84, 200, 56, 72);
                JTable lowStockTable = TableUtil.createTable(lowStockModel);
        TableUtil.setColumnWidths(lowStockTable, 80, 240, 80, 100);
        activityTabs.setFont(UITheme.FONT_BOLD);
        activityTabs.addTab("Recent Transactions", UITheme.scroll(recentTable));
        activityTabs.addTab("Low Stock Alerts", UITheme.scroll(lowStockTable));
        RoundedPanel activityCard = new RoundedPanel(new BorderLayout());
        activityCard.add(activityTabs, BorderLayout.CENTER);

        // the chart gets a fixed width, the transaction list gets the rest
        chartCard.setPreferredSize(new java.awt.Dimension(380, 10));
        JPanel lower = new JPanel(new BorderLayout(16, 0));
        lower.setOpaque(false);
        lower.add(chartCard, BorderLayout.WEST);
        lower.add(activityCard, BorderLayout.CENTER);

        // banner above the cards: customer requests waiting for review
        lblBanner.setFont(UITheme.FONT_BOLD);
        lblBanner.setForeground(UITheme.TEXT);
        lblBanner.setIcon(new ModuleIcon(ModuleIcon.Type.REQUESTS, 26, UITheme.WARNING));
        lblBanner.setIconTextGap(12);
        FlatButton btnOpenRequests = new FlatButton("Open Customer Requests", UITheme.WARNING, ModuleIcon.Type.REQUESTS);
        btnOpenRequests.addActionListener(e -> navigator.accept(DashboardFrame.REQUESTS));
        requestBanner.setBorder(new EmptyBorder(6, 16, 6, 8));   // slimmer than a normal card
        requestBanner.add(lblBanner, BorderLayout.CENTER);
        requestBanner.add(btnOpenRequests, BorderLayout.EAST);
        requestBanner.setVisible(false);

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.setOpaque(false);
        top.add(requestBanner, BorderLayout.NORTH);
        top.add(cards, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        String name = Session.getCurrentUser() == null ? "" : Session.getCurrentUser().getFullName();
        lblWelcome.setText("Welcome, " + name + ". Here is today's summary.");
        try {
            DashboardStats s = dashboardDAO.getStats();
            cardWarehouses.setValue(String.valueOf(s.getActiveWarehouses()), "Storage locations in use");
            cardProducts.setValue(String.valueOf(s.getActiveProducts()), "Items in the catalog");
            cardUnits.setValue(Formats.number(s.getUnitsOnHand()), "In all warehouses");
            cardValue.setValue(Formats.money(s.getStockValue()), "Valued at standard unit cost");
            cardLowStock.setValue(String.valueOf(s.getLowStockProducts()), "At or below reorder level");
            cardReceiving.setValue(String.valueOf(s.getTodayReceivings()),
                    Formats.number(s.getTodayReceivedUnits()) + " units received");
            cardReleasing.setValue(String.valueOf(s.getTodayReleases()),
                    Formats.number(s.getTodayReleasedUnits()) + " units released");
            cardTransfers.setValue(String.valueOf(s.getTodayTransfers()),
                    Formats.number(s.getTodayTransferredUnits()) + " units moved");

            chart.setWarehouses(warehouseDAO.getActiveWarehouses());
            TableUtil.setRows(recentModel, dashboardDAO.getRecentTransactions(15));
            List<Object[]> lowStock = dashboardDAO.getLowStockProducts();
            TableUtil.setRows(lowStockModel, lowStock);
            activityTabs.setTitleAt(1, "Low Stock Alerts (" + lowStock.size() + ")");
            updateRequestBanner();
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the dashboard");
        }
    }

    private void updateRequestBanner() throws SQLException {
        if (!Session.canProcessRequests()) {
            requestBanner.setVisible(false);
            return;
        }
        int[] pending = CustomerRequestsPanel.pendingCounts();
        List<String> parts = new java.util.ArrayList<>();
        if (pending[0] > 0) {
            parts.add(pending[0] + (pending[0] == 1 ? " customer stock request" : " customer stock requests"));
        }
        if (pending[1] > 0) {
            parts.add(pending[1] + (pending[1] == 1 ? " new customer account" : " new customer accounts"));
        }
        requestBanner.setVisible(!parts.isEmpty());
        lblBanner.setText(String.join(" and ", parts) + (pending[0] + pending[1] == 1 ? " is" : " are")
                + " waiting for your review.");
    }
}
