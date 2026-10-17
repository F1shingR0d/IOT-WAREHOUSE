package ui;

import dao.CustomerAccountDAO;
import dao.StockRequestDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.CustomerAccount;
import model.Release;
import model.Status;
import model.StockRequest;
import model.StockRequestItem;
import ui.components.DateField;
import ui.components.FlatButton;
import ui.components.InputBorder;
import ui.components.ModuleIcon;
import ui.components.RoundedPanel;
import ui.components.WrapLayout;
import util.BusinessRuleException;
import util.ErrorHandler;
import util.Formats;
import util.Session;
import util.TableUtil;

/**
 * Staff side of the Customer Portal (administrators and managers).
 *
 * Tab 1 "Stock Requests"    : approve, reject or release customer requests.
 *                             Releasing creates a normal release document.
 * Tab 2 "Customer Accounts" : approve new registrations, deactivate or
 *                             reactivate customer logins.
 */
public class CustomerRequestsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final int NOTE_LENGTH = 255;
    private static final int REQUEST_STATUS_COLUMN = 8;
    private static final int ACCOUNT_STATUS_COLUMN = 7;

    private final StockRequestDAO requestDAO = new StockRequestDAO();
    private final CustomerAccountDAO accountDAO = new CustomerAccountDAO();
    private final JTabbedPane tabs = new JTabbedPane();

    // ---------------- "Stock Requests" tab
    private final JTextField txtSearch = new JTextField(16);
    private final DateField dateFrom = new DateField(null);
    private final DateField dateTo = new DateField(null);
    private final JComboBox<String> cboStatus = new JComboBox<>(new String[]{"All statuses", Status.PENDING,
        Status.APPROVED, Status.FULFILLED, Status.REJECTED, Status.CANCELLED});
    private final FlatButton btnSearch = new FlatButton("Search", UITheme.PRIMARY, ModuleIcon.Type.SEARCH);
    private final FlatButton btnReset = FlatButton.outline("Reset", ModuleIcon.Type.CLEAR);
    private final FlatButton btnView = FlatButton.outline("View Details", ModuleIcon.Type.VIEW);
    private final FlatButton btnApprove = FlatButton.outline("Approve", ModuleIcon.Type.EDIT).withAccent(UITheme.INFO);
    private final FlatButton btnRelease = new FlatButton("Release Stock", UITheme.SUCCESS, ModuleIcon.Type.RELEASING);
    private final FlatButton btnReject = FlatButton.outline("Reject", ModuleIcon.Type.VOID).withAccent(UITheme.DANGER);
    private final FlatButton btnPrint = FlatButton.outline("Print List", ModuleIcon.Type.PRINT);
    private final DefaultTableModel requestModel = TableUtil.createModel(
            new String[]{"ID", "Reference No.", "Request Date", "Needed By", "Customer", "Warehouse", "Items", "Units",
                "Status", "Release No."},
            new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, String.class,
                Integer.class, Integer.class, String.class, String.class});
    private final JTable requestTable = TableUtil.createTable(requestModel);
    private final JLabel lblRequestCount = UITheme.mutedLabel(" ");

    // ---------------- "Customer Accounts" tab
    private final JTextField txtAccountSearch = new JTextField(16);
    private final JComboBox<String> cboAccountStatus = new JComboBox<>(new String[]{"All statuses", Status.PENDING,
        Status.ACTIVE, Status.INACTIVE});
    private final FlatButton btnAccountSearch = new FlatButton("Search", UITheme.PRIMARY, ModuleIcon.Type.SEARCH);
    private final FlatButton btnAccountShowAll = FlatButton.outline("Show All");
    private final FlatButton btnActivate = new FlatButton("Approve Account", UITheme.SUCCESS, ModuleIcon.Type.USER);
    private final FlatButton btnDeactivate = FlatButton.outline("Deactivate", ModuleIcon.Type.VOID)
            .withAccent(UITheme.DANGER);
    private final DefaultTableModel accountModel = TableUtil.createModel(
            new String[]{"ID", "Username", "Customer", "Contact No.", "E-mail", "Registered On", "Last Login",
                "Status"},
            new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, String.class,
                String.class, String.class});
    private final JTable accountTable = TableUtil.createTable(accountModel);
    private final JLabel lblAccountCount = UITheme.mutedLabel(" ");

    public CustomerRequestsPanel() {
        super(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader("Customer Requests",
                "Review stock requests sent through the Customer Portal and approve new customer accounts."),
                BorderLayout.NORTH);

        tabs.setFont(UITheme.FONT_BOLD);
        tabs.addTab("Stock Requests", buildRequestsTab());
        tabs.addTab("Customer Accounts", buildAccountsTab());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                loadAccounts();
            } else {
                loadRequests();
            }
        });
        add(tabs, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------ layout

    private JComponent buildRequestsTab() {
        txtSearch.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.SEARCH, 18, UITheme.TEXT_MUTED));
        UITheme.setPlaceholder(txtSearch, "Reference, customer, product...");
        JPanel filters = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        filters.setOpaque(false);
        filters.add(UITheme.accentLabel("Search:"));
        filters.add(txtSearch);
        filters.add(UITheme.accentLabel("From:"));
        filters.add(dateFrom);
        filters.add(UITheme.accentLabel("To:"));
        filters.add(dateTo);
        filters.add(cboStatus);
        filters.add(btnSearch);
        filters.add(btnReset);

        TableUtil.hideColumn(requestTable, 0);   // the ID column stays in the model only
        TableUtil.setColumnWidths(requestTable, 140, 95, 95, 175, 165, 50, 60, 105, 120);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblRequestCount, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnView);
        actions.add(btnApprove);
        actions.add(btnRelease);
        actions.add(btnReject);
        actions.add(btnPrint);
        bottom.add(actions, BorderLayout.EAST);

        btnSearch.addActionListener(e -> loadRequests());
        txtSearch.addActionListener(e -> loadRequests());
        cboStatus.addActionListener(e -> loadRequests());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            dateFrom.setDate(null);
            dateTo.setDate(null);
            cboStatus.setSelectedIndex(0);   // also reloads the table
        });
        requestTable.getSelectionModel().addListSelectionListener(e -> updateRequestButtons());
        requestTable.addMouseListener(new MouseAdapter() {   // double-click opens the details
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && requestTable.getSelectedRow() >= 0) {
                    viewRequest();
                }
            }
        });
        btnView.addActionListener(e -> viewRequest());
        btnApprove.addActionListener(e -> approveRequest());
        btnRelease.addActionListener(e -> releaseRequest());
        btnReject.addActionListener(e -> rejectRequest());
        btnPrint.addActionListener(e -> TableUtil.print(this, requestTable, "Customer Stock Requests"));
        updateRequestButtons();
        return tabPanel(filters, requestTable, bottom);
    }

    private JComponent buildAccountsTab() {
        txtAccountSearch.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.SEARCH, 18, UITheme.TEXT_MUTED));
        UITheme.setPlaceholder(txtAccountSearch, "Username, customer, e-mail...");
        JPanel filters = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        filters.setOpaque(false);
        filters.add(UITheme.accentLabel("Search:"));
        filters.add(txtAccountSearch);
        filters.add(cboAccountStatus);
        filters.add(btnAccountSearch);
        filters.add(btnAccountShowAll);

        TableUtil.hideColumn(accountTable, 0);
        TableUtil.setColumnWidths(accountTable, 110, 190, 115, 210, 135, 135, 95);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblAccountCount, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnActivate);
        actions.add(btnDeactivate);
        bottom.add(actions, BorderLayout.EAST);

        btnAccountSearch.addActionListener(e -> loadAccounts());
        txtAccountSearch.addActionListener(e -> loadAccounts());
        cboAccountStatus.addActionListener(e -> loadAccounts());
        btnAccountShowAll.addActionListener(e -> {
            txtAccountSearch.setText("");
            cboAccountStatus.setSelectedIndex(0);   // also reloads the table
        });
        accountTable.getSelectionModel().addListSelectionListener(e -> updateAccountButtons());
        btnActivate.addActionListener(e -> activateAccount());
        btnDeactivate.addActionListener(e -> deactivateAccount());
        updateAccountButtons();
        return tabPanel(filters, accountTable, bottom);
    }

    private static JComponent tabPanel(JComponent filters, JTable table, JComponent bottom) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.add(filters, BorderLayout.NORTH);
        card.add(UITheme.scroll(table), BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(UITheme.BACKGROUND);
        tab.setBorder(new EmptyBorder(12, 2, 2, 2));
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    // ------------------------------------------------------------------ data loading

    @Override
    public void refreshData() {
        if (tabs.getSelectedIndex() == 1) {
            loadAccounts();
        } else {
            loadRequests();
        }
    }

    /** Shows the number of items waiting for review in the tab titles. */
    private void updateTabTitles() {
        try {
            int pendingRequests = requestDAO.countByStatus(0).get(Status.PENDING);
            int pendingAccounts = accountDAO.countPending();
            tabs.setTitleAt(0, "Stock Requests" + (pendingRequests > 0 ? " (" + pendingRequests + " pending)" : ""));
            tabs.setTitleAt(1, "Customer Accounts" + (pendingAccounts > 0 ? " (" + pendingAccounts + " pending)" : ""));
        } catch (SQLException e) {
            ErrorHandler.log(e);   // the titles are only a hint; the tables show the real data
        }
    }

    private void loadRequests() {
        LocalDate from = null;
        LocalDate to = null;
        if (!dateFrom.isEmpty()) {
            from = dateFrom.getDate();
            if (from == null) {
                Dialogs.invalid(this, "The From date must be in the yyyy-MM-dd format.", dateFrom);
                return;
            }
        }
        if (!dateTo.isEmpty()) {
            to = dateTo.getDate();
            if (to == null) {
                Dialogs.invalid(this, "The To date must be in the yyyy-MM-dd format.", dateTo);
                return;
            }
        }
        if (from != null && to != null && from.isAfter(to)) {
            Dialogs.invalid(this, "The From date must be before the To date.", dateFrom);
            return;
        }
        String status = cboStatus.getSelectedIndex() == 0 ? null : (String) cboStatus.getSelectedItem();
        try {
            List<Object[]> rows = new ArrayList<>();
            for (StockRequest r : requestDAO.searchRequests(txtSearch.getText().trim(), from, to, status, 0)) {
                rows.add(new Object[]{r.getRequestId(), r.getReferenceNo(), Formats.date(r.getRequestDate()),
                    r.getNeededBy() == null ? "" : Formats.date(r.getNeededBy()), r.getCustomerName(),
                    r.getWarehouseName(), r.getItemCount(), r.getTotalQuantity(), r.getStatus(),
                    r.getReleaseReference()});
            }
            TableUtil.setRows(requestModel, rows);
            lblRequestCount.setText(rows.size() + " request(s) found");
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the customer requests");
        }
        updateTabTitles();
        updateRequestButtons();
    }

    private void loadAccounts() {
        String status = cboAccountStatus.getSelectedIndex() == 0 ? null : (String) cboAccountStatus.getSelectedItem();
        try {
            List<Object[]> rows = new ArrayList<>();
            for (CustomerAccount a : accountDAO.searchAccounts(txtAccountSearch.getText().trim(), status)) {
                rows.add(new Object[]{a.getAccountId(), a.getUsername(), a.getCustomerName(), a.getContactNo(), a.getEmail(), Formats.dateTime(a.getCreatedAt()),
                    a.getLastLogin() == null ? "Never" : Formats.dateTime(a.getLastLogin()), a.getStatus()});
            }
            TableUtil.setRows(accountModel, rows);
            lblAccountCount.setText(rows.size() + " account(s) found");
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the customer accounts");
        }
        updateTabTitles();
        updateAccountButtons();
    }

    private void updateRequestButtons() {
        int row = TableUtil.getSelectedModelRow(requestTable);
        String status = row < 0 ? null : (String) requestModel.getValueAt(row, REQUEST_STATUS_COLUMN);
        boolean open = Status.PENDING.equals(status) || Status.APPROVED.equals(status);
        btnView.setEnabled(row >= 0);
        btnApprove.setEnabled(Status.PENDING.equals(status));
        btnRelease.setEnabled(open);
        btnReject.setEnabled(open);
    }

    private void updateAccountButtons() {
        int row = TableUtil.getSelectedModelRow(accountTable);
        String status = row < 0 ? null : (String) accountModel.getValueAt(row, ACCOUNT_STATUS_COLUMN);
        btnActivate.setText(Status.INACTIVE.equals(status) ? "Reactivate" : "Approve Account");
        btnActivate.setEnabled(row >= 0 && !Status.ACTIVE.equals(status));
        btnDeactivate.setEnabled(row >= 0 && !Status.INACTIVE.equals(status));
    }

    // ------------------------------------------------------------------ request actions

    private int selectedRequestId() {
        int row = TableUtil.getSelectedModelRow(requestTable);
        return row < 0 ? -1 : (Integer) requestModel.getValueAt(row, 0);
    }

    private String selectedReference() {
        int row = TableUtil.getSelectedModelRow(requestTable);
        return row < 0 ? "" : (String) requestModel.getValueAt(row, 1);
    }

    /** Reads the request again so the dialogs show the current stock. Null when it no longer exists. */
    private StockRequest loadSelectedRequest() throws SQLException {
        int id = selectedRequestId();
        if (id < 0) {
            return null;
        }
        StockRequest request = requestDAO.getRequestById(id, 0);
        if (request == null) {
            Dialogs.warning(this, "This request no longer exists. The list will be refreshed.");
            loadRequests();
        }
        return request;
    }

    private void viewRequest() {
        try {
            StockRequest request = loadSelectedRequest();
            if (request != null) {
                StockRequestDetails.show(this, request, true);
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the request details");
        }
    }

    private void approveRequest() {
        int id = selectedRequestId();
        if (id < 0) {
            return;
        }
        String reference = selectedReference();
        String note = JOptionPane.showInputDialog(this, "Approve request " + reference + "?\n\n"
                + "The customer will see it as APPROVED. The stock is deducted only when\n"
                + "you click Release Stock.\n\nOptional note for the customer:", "Approve Request",
                JOptionPane.QUESTION_MESSAGE);
        if (note == null) {
            return;   // Cancel
        }
        if (note.trim().length() > NOTE_LENGTH) {
            Dialogs.invalid(this, "The note can have at most " + NOTE_LENGTH + " characters.", null);
            return;
        }
        try {
            requestDAO.approveRequest(id, Session.getUserId(), note);
            Dialogs.success(this, "Request " + reference + " was approved.");
        } catch (BusinessRuleException e) {
            ErrorHandler.showRuleViolation(this, e);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "approve the request");
        }
        loadRequests();
    }

    private void rejectRequest() {
        int id = selectedRequestId();
        if (id < 0) {
            return;
        }
        String reference = selectedReference();
        String reason;
        while (true) {
            reason = JOptionPane.showInputDialog(this, "Reject request " + reference + "?\n\n"
                    + "Reason (shown to the customer):", "Reject Request", JOptionPane.WARNING_MESSAGE);
            if (reason == null) {
                return;   // Cancel
            }
            if (reason.trim().isEmpty()) {
                Dialogs.invalid(this, "Please enter the reason for rejecting the request.", null);
            } else if (reason.trim().length() > NOTE_LENGTH) {
                Dialogs.invalid(this, "The reason can have at most " + NOTE_LENGTH + " characters.", null);
            } else {
                break;
            }
        }
        try {
            requestDAO.rejectRequest(id, Session.getUserId(), reason);
            Dialogs.success(this, "Request " + reference + " was rejected.");
        } catch (BusinessRuleException e) {
            ErrorHandler.showRuleViolation(this, e);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "reject the request");
        }
        loadRequests();
    }

    /** Creates a release document for the request (after checking and confirming the stock). */
    private void releaseRequest() {
        try {
            StockRequest request = loadSelectedRequest();
            if (request == null) {
                return;
            }
            StringBuilder shortage = new StringBuilder();
            for (StockRequestItem item : request.getItems()) {
                if (item.getAvailable() < item.getQuantity()) {
                    shortage.append("\n  ").append(item.getProductName()).append(": requested ")
                            .append(Formats.number(item.getQuantity())).append(", in stock ")
                            .append(Formats.number(item.getAvailable()));
                }
            }
            if (shortage.length() > 0) {
                Dialogs.warning(this, "Request " + request.getReferenceNo() + " cannot be released yet.\n"
                        + request.getWarehouseName() + " does not have enough stock of:" + shortage
                        + "\n\nReceive or transfer more stock first, or reject the request.");
                return;
            }
            if (!Dialogs.confirm(this, "Release request " + request.getReferenceNo() + " now?\n\n"
                    + "A release document will be created for " + request.getCustomerName() + "\n"
                    + "from " + request.getWarehouseName() + ": " + request.getItems().size() + " product(s), "
                    + Formats.number(request.getItemsQuantity()) + " units.\n"
                    + "The quantities are deducted from the warehouse stock.", "Release Stock")) {
                return;
            }
            Release release = requestDAO.releaseRequest(request.getRequestId(), Session.getUserId(), LocalDate.now());
            Dialogs.success(this, "Release " + release.getReferenceNo() + " was created.\n\n"
                    + "Request " + request.getReferenceNo() + " is now FULFILLED and the warehouse stock was updated.");
        } catch (BusinessRuleException e) {
            ErrorHandler.showRuleViolation(this, e);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "release the request");
        }
        loadRequests();
    }

    // ------------------------------------------------------------------ account actions

    private void activateAccount() {
        int row = TableUtil.getSelectedModelRow(accountTable);
        if (row < 0) {
            return;
        }
        int id = (Integer) accountModel.getValueAt(row, 0);
        String username = (String) accountModel.getValueAt(row, 1);
        String customer = (String) accountModel.getValueAt(row, 2);
        boolean pending = Status.PENDING.equals(accountModel.getValueAt(row, ACCOUNT_STATUS_COLUMN));
        if (!Dialogs.confirm(this, (pending ? "Approve" : "Reactivate") + " the account of " + customer
                + " (" + username + ")?\n\nThe customer will be able to log in and send stock requests.\n"
                + "The customer record also becomes ACTIVE.", pending ? "Approve Account" : "Reactivate Account")) {
            return;
        }
        try {
            if (accountDAO.activateAccount(id)) {
                Dialogs.success(this, "The account \"" + username + "\" is now ACTIVE.");
            } else {
                Dialogs.warning(this, "This account no longer exists. The list will be refreshed.");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "activate the account");
        }
        loadAccounts();
    }

    private void deactivateAccount() {
        int row = TableUtil.getSelectedModelRow(accountTable);
        if (row < 0) {
            return;
        }
        int id = (Integer) accountModel.getValueAt(row, 0);
        String username = (String) accountModel.getValueAt(row, 1);
        String customer = (String) accountModel.getValueAt(row, 2);
        if (!Dialogs.confirm(this, "Deactivate the account of " + customer + " (" + username + ")?\n\n"
                + "The customer can no longer log in. Existing requests are kept.", "Deactivate Account")) {
            return;
        }
        try {
            if (accountDAO.deactivateAccount(id)) {
                Dialogs.success(this, "The account \"" + username + "\" was deactivated.");
            } else {
                Dialogs.warning(this, "This account no longer exists. The list will be refreshed.");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "deactivate the account");
        }
        loadAccounts();
    }

    /** Pending requests and registrations (used by the dashboard banner). */
    static int[] pendingCounts() throws SQLException {
        Map<String, Integer> counts = new StockRequestDAO().countByStatus(0);
        return new int[]{counts.get(Status.PENDING), new CustomerAccountDAO().countPending()};
    }
}
