package ui;

import dao.StockRequestDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Status;
import model.StockRequest;
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
 * Customer Portal: the customer's own requests with their status, the
 * warehouse's note and the release that delivered them. PENDING requests
 * can be cancelled.
 */
public class CustomerRequestHistoryPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final StockRequestDAO requestDAO = new StockRequestDAO();
    private final JTextField txtSearch = new JTextField(16);
    private final DateField dateFrom = new DateField(null);
    private final DateField dateTo = new DateField(null);
    private final JComboBox<String> cboStatus = new JComboBox<>(new String[]{"All statuses", Status.PENDING,
        Status.APPROVED, Status.FULFILLED, Status.REJECTED, Status.CANCELLED});
    private final FlatButton btnSearch = new FlatButton("Search", UITheme.PRIMARY, ModuleIcon.Type.SEARCH);
    private final FlatButton btnReset = FlatButton.outline("Reset", ModuleIcon.Type.CLEAR);
    private final FlatButton btnView = FlatButton.outline("View Details", ModuleIcon.Type.VIEW);
    private final FlatButton btnCancel = FlatButton.outline("Cancel Request", ModuleIcon.Type.VOID)
            .withAccent(UITheme.DANGER);
    private final FlatButton btnPrint = FlatButton.outline("Print List", ModuleIcon.Type.PRINT);
    private final DefaultTableModel model = TableUtil.createModel(
            new String[]{"ID", "Reference No.", "Request Date", "Needed By", "Warehouse", "Items", "Units", "Status",
                "Release No.", "Warehouse Note"},
            new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, Integer.class,
                Integer.class, String.class, String.class, String.class});
    private final JTable table = TableUtil.createTable(model);
    private final JLabel lblCount = UITheme.mutedLabel(" ");

    public CustomerRequestHistoryPanel() {
        super(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader("My Requests",
                "Follow the status of your stock requests. Requests that are still PENDING can be cancelled."),
                BorderLayout.NORTH);

        txtSearch.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.SEARCH, 18, UITheme.TEXT_MUTED));
        UITheme.setPlaceholder(txtSearch, "Reference, warehouse, product...");
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

        TableUtil.hideColumn(table, 0);   // the ID column stays in the model only
        TableUtil.setColumnWidths(table, 140, 95, 95, 155, 50, 60, 105, 115, 195);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblCount, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnView);
        actions.add(btnCancel);
        actions.add(btnPrint);
        bottom.add(actions, BorderLayout.EAST);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.add(filters, BorderLayout.NORTH);
        card.add(UITheme.scroll(table), BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        btnSearch.addActionListener(e -> loadTable());
        txtSearch.addActionListener(e -> loadTable());
        cboStatus.addActionListener(e -> loadTable());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            dateFrom.setDate(null);
            dateTo.setDate(null);
            cboStatus.setSelectedIndex(0);   // also reloads the table
        });
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {   // double-click opens the details
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    viewDetails();
                }
            }
        });
        btnView.addActionListener(e -> viewDetails());
        btnCancel.addActionListener(e -> cancelRequest());
        btnPrint.addActionListener(e -> TableUtil.print(this, table, "My Stock Requests - "
                + Session.getCurrentCustomer().getCustomerName()));
        updateButtons();
    }

    @Override
    public void refreshData() {
        loadTable();
    }

    private void loadTable() {
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
            for (StockRequest r : requestDAO.searchRequests(txtSearch.getText().trim(), from, to, status,
                    Session.getCustomerId())) {
                rows.add(new Object[]{r.getRequestId(), r.getReferenceNo(), Formats.date(r.getRequestDate()),
                    r.getNeededBy() == null ? "" : Formats.date(r.getNeededBy()), r.getWarehouseName(),
                    r.getItemCount(), r.getTotalQuantity(), r.getStatus(), r.getReleaseReference(),
                    r.getReviewNote()});
            }
            TableUtil.setRows(model, rows);
            lblCount.setText(rows.size() + " request(s) found");
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load your requests");
        }
        updateButtons();
    }

    private void updateButtons() {
        int row = TableUtil.getSelectedModelRow(table);
        btnView.setEnabled(row >= 0);
        btnCancel.setEnabled(row >= 0 && Status.PENDING.equals(model.getValueAt(row, 7)));
    }

    private void viewDetails() {
        int row = TableUtil.getSelectedModelRow(table);
        if (row < 0) {
            return;
        }
        try {
            StockRequest request = requestDAO.getRequestById((Integer) model.getValueAt(row, 0),
                    Session.getCustomerId());
            if (request == null) {
                Dialogs.warning(this, "This request no longer exists.");
                loadTable();
                return;
            }
            StockRequestDetails.show(this, request, false);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the request details");
        }
    }

    private void cancelRequest() {
        int row = TableUtil.getSelectedModelRow(table);
        if (row < 0) {
            return;
        }
        String reference = (String) model.getValueAt(row, 1);
        if (!Dialogs.confirm(this, "Cancel request " + reference + "?\n\n"
                + "The warehouse will no longer process it. This cannot be undone.", "Cancel Request")) {
            return;
        }
        try {
            requestDAO.cancelRequest((Integer) model.getValueAt(row, 0), Session.getCustomerId());
            Dialogs.success(this, "Request " + reference + " was cancelled.");
        } catch (BusinessRuleException e) {
            ErrorHandler.showRuleViolation(this, e);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "cancel the request");
        }
        loadTable();
    }
}
