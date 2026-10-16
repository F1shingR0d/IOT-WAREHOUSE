package ui;

import dao.StockRequestDAO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Status;
import model.StockRequest;
import ui.components.FlatButton;
import ui.components.ModuleIcon;
import ui.components.RoundedPanel;
import ui.components.StatCard;
import util.ErrorHandler;
import util.Formats;
import util.Session;
import util.TableUtil;

/**
 * Customer Portal home: request counts per status, the latest requests and
 * a short explanation of how requesting stock works.
 */
public class CustomerHomePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final int RECENT_LIMIT = 10;

    private final StockRequestDAO requestDAO = new StockRequestDAO();
    private final StatCard cardPending = new StatCard("Pending", UITheme.WARNING);
    private final StatCard cardApproved = new StatCard("Approved", UITheme.INFO);
    private final StatCard cardFulfilled = new StatCard("Fulfilled", UITheme.SUCCESS);
    private final StatCard cardRejected = new StatCard("Rejected", UITheme.DANGER);
    private final DefaultTableModel recentModel = TableUtil.createModel(
            new String[]{"Reference No.", "Request Date", "Warehouse", "Items", "Units", "Status"},
            new Class<?>[]{String.class, String.class, String.class, Integer.class, Integer.class, String.class});
    private final JLabel lblWelcome = UITheme.mutedLabel(" ");
    private final JLabel lblRecentCount = UITheme.mutedLabel(" ");

    public CustomerHomePanel(CustomerDashboardFrame frame) {
        super(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        FlatButton btnNew = new FlatButton("New Request", UITheme.PRIMARY, ModuleIcon.Type.ADD);
        FlatButton btnRefresh = FlatButton.outline("Refresh", ModuleIcon.Type.CLEAR);
        btnNew.addActionListener(e -> frame.showModule(CustomerDashboardFrame.NEW_REQUEST));
        btnRefresh.addActionListener(e -> refreshData());
        add(UITheme.pageHeader("Home", lblWelcome, btnNew, btnRefresh), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 16));
        cards.setOpaque(false);
        cards.add(cardPending);
        cards.add(cardApproved);
        cards.add(cardFulfilled);
        cards.add(cardRejected);

        JTable recentTable = TableUtil.createTable(recentModel);
        TableUtil.setColumnWidths(recentTable, 130, 95, 170, 50, 60, 105);
        RoundedPanel recentCard = new RoundedPanel(new BorderLayout(0, 10));
        recentCard.add(UITheme.headingLabel("Recent Requests"), BorderLayout.NORTH);
        recentCard.add(UITheme.scroll(recentTable), BorderLayout.CENTER);
        recentCard.add(lblRecentCount, BorderLayout.SOUTH);

        RoundedPanel helpCard = new RoundedPanel(new BorderLayout(0, 12));
        helpCard.add(UITheme.headingLabel("How It Works"), BorderLayout.NORTH);
        String step = "<p style='margin-bottom:8px'><b style='color:#fbab06'>%d.</b>&nbsp; %s</p>";
        JLabel steps = new JLabel("<html><div style='width:235px'>"
                + String.format(step, 1, "Browse the <b>Product Catalog</b> to see what each warehouse has in stock.")
                + String.format(step, 2, "Create a <b>New Request</b>: choose a warehouse, then add the products "
                        + "and quantities you need.")
                + String.format(step, 3, "The warehouse reviews it. It becomes <b>APPROVED</b>, or <b>REJECTED</b> "
                        + "with a reason.")
                + String.format(step, 4, "When the stock is released to you, the request becomes <b>FULFILLED</b>.")
                + "<p style='color:#a5aeb8'>Tip: you can cancel a request while it is still PENDING "
                + "(My Requests).</p></div></html>");
        steps.setFont(UITheme.FONT);
        steps.setForeground(UITheme.TEXT);
        steps.setVerticalAlignment(JLabel.TOP);
        helpCard.add(steps, BorderLayout.CENTER);
        helpCard.setPreferredSize(new Dimension(360, 10));

        JPanel lower = new JPanel(new BorderLayout(16, 0));
        lower.setOpaque(false);
        lower.add(recentCard, BorderLayout.CENTER);
        lower.add(helpCard, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(cards, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        int customerId = Session.getCustomerId();
        lblWelcome.setText("Hello, " + Session.getCurrentCustomer().getCustomerName() + ".");
        try {
            Map<String, Integer> counts = requestDAO.countByStatus(customerId);
            cardPending.setValue(String.valueOf(counts.get(Status.PENDING)), "Waiting for the warehouse");
            cardApproved.setValue(String.valueOf(counts.get(Status.APPROVED)), "Accepted, to be released");
            cardFulfilled.setValue(String.valueOf(counts.get(Status.FULFILLED)), "Stock released to you");
            cardRejected.setValue(String.valueOf(counts.get(Status.REJECTED)),
                    counts.get(Status.CANCELLED) + " cancelled by you");

            List<StockRequest> requests = requestDAO.searchRequests("", null, null, null, customerId);
            List<Object[]> rows = new ArrayList<>();
            for (StockRequest r : requests.subList(0, Math.min(RECENT_LIMIT, requests.size()))) {
                rows.add(new Object[]{r.getReferenceNo(), Formats.date(r.getRequestDate()), r.getWarehouseName(),
                    r.getItemCount(), r.getTotalQuantity(), r.getStatus()});
            }
            TableUtil.setRows(recentModel, rows);
            lblRecentCount.setText(requests.isEmpty() ? "You have not sent any requests yet. Click New Request to start."
                    : "Showing the latest " + rows.size() + " of " + requests.size() + " request(s).");
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load your requests");
        }
    }
}
