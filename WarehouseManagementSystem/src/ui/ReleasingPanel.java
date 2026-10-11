package ui;

import dao.CustomerDAO;
import dao.ReleaseDAO;
import dao.WarehouseDAO;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import model.Customer;
import model.Release;
import model.ReleaseItem;
import model.Warehouse;
import util.BusinessRuleException;
import util.Formats;
import util.Session;

/**
 * Releasing (stock OUT): goods released from a warehouse to a customer.
 * The available stock is checked while adding items and again when saving.
 */
public class ReleasingPanel extends TransactionPanel {

    private static final long serialVersionUID = 1L;

    private final ReleaseDAO releaseDAO = new ReleaseDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final JComboBox<Warehouse> cboWarehouse = new JComboBox<>();
    private final JComboBox<Customer> cboCustomer = new JComboBox<>();

    public ReleasingPanel() {
        super("Releasing", "Record stock released to customers. Saving deducts the quantities from the warehouse stock.",
                "Release", false, true,
                new String[]{"ID", "Reference No.", "Date", "Warehouse", "Customer", "Items", "Units", "Recorded By",
                    "Status"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, Integer.class,
                    Integer.class, String.class, String.class});
        cboWarehouse.setRenderer(UITheme.placeholderRenderer("Select the warehouse to release from"));
        cboCustomer.setRenderer(UITheme.placeholderRenderer("Select the customer"));
        cboWarehouse.addActionListener(e -> sourceWarehouseChanged());
        buildUI();
        setHistoryColumnWidths(134, 90, 180, 220, 55, 70, 130, 75);
    }

    @Override
    protected void addHeaderFields(HeaderGrid grid) {
        grid.add("Warehouse", cboWarehouse, true);
        grid.add("Customer", cboCustomer, true);
    }

    @Override
    protected void reloadHeaderLookups() throws SQLException {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        Customer customer = (Customer) cboCustomer.getSelectedItem();
        cboWarehouse.setModel(new DefaultComboBoxModel<>(warehouseDAO.getActiveWarehouses().toArray(new Warehouse[0])));
        cboCustomer.setModel(new DefaultComboBoxModel<>(customerDAO.getActiveCustomers().toArray(new Customer[0])));
        reselect(cboWarehouse, warehouse);
        reselect(cboCustomer, customer);
    }

    @Override
    protected Warehouse getSourceWarehouse() {
        return (Warehouse) cboWarehouse.getSelectedItem();
    }

    @Override
    protected JComponent getSourceWarehouseField() {
        return cboWarehouse;
    }

    @Override
    protected boolean validateHeader() {
        if (cboWarehouse.getSelectedItem() == null) {
            Dialogs.invalid(this, "Please select the warehouse the stock is released from.", cboWarehouse);
            return false;
        }
        if (cboCustomer.getSelectedItem() == null) {
            Dialogs.invalid(this, "Please select the customer.", cboCustomer);
            return false;
        }
        return true;
    }

    @Override
    protected String saveDocument(LocalDate date, String remarks) throws SQLException, BusinessRuleException {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        Customer customer = (Customer) cboCustomer.getSelectedItem();
        Release release = new Release(warehouse.getWarehouseId(), customer.getCustomerId(), Session.getUserId());
        release.setDocumentDate(date);
        release.setRemarks(remarks);
        for (Line line : lines) {
            release.addItem(new ReleaseItem(line.product.getProductId(), line.product.getSku(),
                    line.product.getProductName(), line.product.getUnit(), line.quantity));
        }
        releaseDAO.saveRelease(release);
        return release.getReferenceNo();
    }

    @Override
    protected void clearHeaderFields() {
        cboWarehouse.setSelectedItem(null);
        cboCustomer.setSelectedItem(null);
    }

    @Override
    protected List<Object[]> fetchHistory(String keyword, LocalDate from, LocalDate to, String status)
            throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        for (Release r : releaseDAO.searchReleases(keyword, from, to, status)) {
            rows.add(new Object[]{r.getReleaseId(), r.getReferenceNo(), Formats.date(r.getDocumentDate()),
                r.getWarehouseName(), r.getCustomerName(), r.getItemCount(), r.getTotalQuantity(),
                r.getRecordedBy(), r.getStatus()});
        }
        return rows;
    }

    @Override
    protected void showDetails(int documentId) throws SQLException {
        Release r = releaseDAO.getReleaseById(documentId);
        if (r == null) {
            Dialogs.warning(this, "This document no longer exists.");
            return;
        }
        List<Object[]> items = new ArrayList<>();
        int number = 1;
        for (ReleaseItem item : r.getItems()) {
            items.add(new Object[]{number++, item.getSku(), item.getProductName(), item.getUnit(), item.getQuantity()});
        }
        String[][] header = {
            {"Reference No.", r.getReferenceNo()}, {"Date", Formats.date(r.getDocumentDate())},
            {"Warehouse", r.getWarehouseName()}, {"Customer", r.getCustomerName()},
            {"Recorded By", r.getRecordedBy()}, {"Recorded On", Formats.dateTime(r.getCreatedAt())},
            {"Remarks", r.getRemarks()}
        };
        new TransactionDetailsDialog(SwingUtilities.getWindowAncestor(this), "Release " + r.getReferenceNo(),
                r.getStatus(), header,
                new String[]{"#", "SKU", "Product", "Unit", "Quantity"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, Integer.class},
                items, r.getItems().size() + " item(s)     Total units: " + Formats.number(r.getItemsQuantity()))
                .setVisible(true);
    }

    @Override
    protected void voidDocument(int documentId) throws SQLException, BusinessRuleException {
        releaseDAO.voidRelease(documentId);
    }

    @Override
    protected String describeVoid() {
        return "Its units will be RETURNED to the warehouse stock.";
    }
}
