package ui;

import dao.ReceivingDAO;
import dao.SupplierDAO;
import dao.WarehouseDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import model.Receiving;
import model.ReceivingItem;
import model.Supplier;
import model.Warehouse;
import util.BusinessRuleException;
import util.Formats;
import util.Session;

/**
 * Receiving (stock IN): goods delivered by a supplier to a warehouse.
 * Saving the document ADDS the quantities to the warehouse stock.
 */
public class ReceivingPanel extends TransactionPanel {

    private static final long serialVersionUID = 1L;

    private final ReceivingDAO receivingDAO = new ReceivingDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final JComboBox<Warehouse> cboWarehouse = new JComboBox<>();
    private final JComboBox<Supplier> cboSupplier = new JComboBox<>();

    public ReceivingPanel() {
        super("Receiving", "Record stock delivered by suppliers. Saving adds the quantities to the warehouse stock.",
                "Receiving", true, false,
                new String[]{"ID", "Reference No.", "Date", "Warehouse", "Supplier", "Items", "Units", "Total Cost",
                    "Recorded By", "Status"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, Integer.class,
                    Integer.class, BigDecimal.class, String.class, String.class});
        cboWarehouse.setRenderer(UITheme.placeholderRenderer("Select the receiving warehouse"));
        cboSupplier.setRenderer(UITheme.placeholderRenderer("Select the supplier"));
        cboWarehouse.addActionListener(e -> sourceWarehouseChanged());
        buildUI();
        setHistoryColumnWidths(134, 90, 170, 210, 55, 70, 110, 130, 75);
    }

    @Override
    protected void addHeaderFields(HeaderGrid grid) {
        grid.add("Warehouse", cboWarehouse, true);
        grid.add("Supplier", cboSupplier, true);
    }

    @Override
    protected void reloadHeaderLookups() throws SQLException {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        Supplier supplier = (Supplier) cboSupplier.getSelectedItem();
        cboWarehouse.setModel(new DefaultComboBoxModel<>(warehouseDAO.getActiveWarehouses().toArray(new Warehouse[0])));
        cboSupplier.setModel(new DefaultComboBoxModel<>(supplierDAO.getActiveSuppliers().toArray(new Supplier[0])));
        reselect(cboWarehouse, warehouse);
        reselect(cboSupplier, supplier);
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
            Dialogs.invalid(this, "Please select the warehouse that receives the stock.", cboWarehouse);
            return false;
        }
        if (cboSupplier.getSelectedItem() == null) {
            Dialogs.invalid(this, "Please select the supplier.", cboSupplier);
            return false;
        }
        return true;
    }

    @Override
    protected String saveDocument(LocalDate date, String remarks) throws SQLException, BusinessRuleException {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        Supplier supplier = (Supplier) cboSupplier.getSelectedItem();
        Receiving receiving = new Receiving(warehouse.getWarehouseId(), supplier.getSupplierId(), Session.getUserId());
        receiving.setDocumentDate(date);
        receiving.setRemarks(remarks);
        for (Line line : lines) {
            receiving.addItem(new ReceivingItem(line.product.getProductId(), line.product.getSku(),
                    line.product.getProductName(), line.product.getUnit(), line.quantity, line.unitCost));
        }
        receivingDAO.saveReceiving(receiving);
        return receiving.getReferenceNo();
    }

    @Override
    protected void clearHeaderFields() {
        cboWarehouse.setSelectedItem(null);
        cboSupplier.setSelectedItem(null);
    }

    @Override
    protected List<Object[]> fetchHistory(String keyword, LocalDate from, LocalDate to, String status)
            throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        for (Receiving r : receivingDAO.searchReceivings(keyword, from, to, status)) {
            rows.add(new Object[]{r.getReceivingId(), r.getReferenceNo(), Formats.date(r.getDocumentDate()),
                r.getWarehouseName(), r.getSupplierName(), r.getItemCount(), r.getTotalQuantity(),
                r.getTotalAmount(), r.getRecordedBy(), r.getStatus()});
        }
        return rows;
    }

    @Override
    protected void showDetails(int documentId) throws SQLException {
        Receiving r = receivingDAO.getReceivingById(documentId);
        if (r == null) {
            Dialogs.warning(this, "This document no longer exists.");
            return;
        }
        List<Object[]> items = new ArrayList<>();
        int number = 1;
        for (ReceivingItem item : r.getItems()) {
            items.add(new Object[]{number++, item.getSku(), item.getProductName(), item.getUnit(), item.getQuantity(),
                item.getUnitCost(), item.getSubtotal()});
        }
        String[][] header = {
            {"Reference No.", r.getReferenceNo()}, {"Date", Formats.date(r.getDocumentDate())},
            {"Warehouse", r.getWarehouseName()}, {"Supplier", r.getSupplierName()},
            {"Recorded By", r.getRecordedBy()}, {"Recorded On", Formats.dateTime(r.getCreatedAt())},
            {"Remarks", r.getRemarks()}
        };
        new TransactionDetailsDialog(SwingUtilities.getWindowAncestor(this), "Receiving " + r.getReferenceNo(),
                r.getStatus(), header,
                new String[]{"#", "SKU", "Product", "Unit", "Quantity", "Unit Cost", "Subtotal"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, Integer.class,
                    BigDecimal.class, BigDecimal.class},
                items, r.getItems().size() + " item(s)     Total units: " + Formats.number(r.getItemsQuantity())
                + "     Total cost: " + Formats.money(r.getItemsAmount())).setVisible(true);
    }

    @Override
    protected void voidDocument(int documentId) throws SQLException, BusinessRuleException {
        receivingDAO.voidReceiving(documentId);
    }

    @Override
    protected String describeVoid() {
        return "Its units will be REMOVED from the warehouse stock again.";
    }
}
