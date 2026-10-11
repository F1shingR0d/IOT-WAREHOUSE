package ui;

import dao.TransferDAO;
import dao.WarehouseDAO;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import model.StockTransfer;
import model.TransferItem;
import model.Warehouse;
import util.BusinessRuleException;
import util.Formats;
import util.Session;

/**
 * Stock transfer: moves stock from one warehouse to another. The source and
 * destination must be different and the source must have enough stock.
 */
public class TransferPanel extends TransactionPanel {

    private static final long serialVersionUID = 1L;

    private final TransferDAO transferDAO = new TransferDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final JComboBox<Warehouse> cboFrom = new JComboBox<>();
    private final JComboBox<Warehouse> cboTo = new JComboBox<>();

    public TransferPanel() {
        super("Stock Transfers", "Move stock between warehouses. The source loses and the destination gains the units.",
                "Transfer", false, true,
                new String[]{"ID", "Reference No.", "Date", "From Warehouse", "To Warehouse", "Items", "Units",
                    "Recorded By", "Status"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, Integer.class,
                    Integer.class, String.class, String.class});
        cboFrom.setRenderer(UITheme.placeholderRenderer("Select the source warehouse"));
        cboTo.setRenderer(UITheme.placeholderRenderer("Select the destination warehouse"));
        cboFrom.addActionListener(e -> sourceWarehouseChanged());
        buildUI();
        setHistoryColumnWidths(134, 90, 190, 190, 55, 70, 130, 75);
    }

    @Override
    protected void addHeaderFields(HeaderGrid grid) {
        grid.add("From Warehouse", cboFrom, true);
        grid.add("To Warehouse", cboTo, true);
    }

    @Override
    protected void reloadHeaderLookups() throws SQLException {
        Warehouse from = (Warehouse) cboFrom.getSelectedItem();
        Warehouse to = (Warehouse) cboTo.getSelectedItem();
        Warehouse[] warehouses = warehouseDAO.getActiveWarehouses().toArray(new Warehouse[0]);
        cboFrom.setModel(new DefaultComboBoxModel<>(warehouses));
        cboTo.setModel(new DefaultComboBoxModel<>(warehouses));
        reselect(cboFrom, from);
        reselect(cboTo, to);
    }

    @Override
    protected Warehouse getSourceWarehouse() {
        return (Warehouse) cboFrom.getSelectedItem();
    }

    @Override
    protected JComponent getSourceWarehouseField() {
        return cboFrom;
    }

    @Override
    protected boolean validateHeader() {
        Warehouse from = (Warehouse) cboFrom.getSelectedItem();
        Warehouse to = (Warehouse) cboTo.getSelectedItem();
        if (from == null) {
            Dialogs.invalid(this, "Please select the source warehouse (From).", cboFrom);
            return false;
        }
        if (to == null) {
            Dialogs.invalid(this, "Please select the destination warehouse (To).", cboTo);
            return false;
        }
        if (from.getWarehouseId() == to.getWarehouseId()) {
            Dialogs.invalid(this, "The source and destination warehouses must be different.", cboTo);
            return false;
        }
        return true;
    }

    @Override
    protected String saveDocument(LocalDate date, String remarks) throws SQLException, BusinessRuleException {
        Warehouse from = (Warehouse) cboFrom.getSelectedItem();
        Warehouse to = (Warehouse) cboTo.getSelectedItem();
        StockTransfer transfer = new StockTransfer(from.getWarehouseId(), to.getWarehouseId(), Session.getUserId());
        transfer.setDocumentDate(date);
        transfer.setRemarks(remarks);
        for (Line line : lines) {
            transfer.addItem(new TransferItem(line.product.getProductId(), line.product.getSku(),
                    line.product.getProductName(), line.product.getUnit(), line.quantity));
        }
        transferDAO.saveTransfer(transfer);
        return transfer.getReferenceNo();
    }

    @Override
    protected void clearHeaderFields() {
        cboFrom.setSelectedItem(null);
        cboTo.setSelectedItem(null);
    }

    @Override
    protected List<Object[]> fetchHistory(String keyword, LocalDate from, LocalDate to, String status)
            throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        for (StockTransfer t : transferDAO.searchTransfers(keyword, from, to, status)) {
            rows.add(new Object[]{t.getTransferId(), t.getReferenceNo(), Formats.date(t.getDocumentDate()),
                t.getFromWarehouseName(), t.getToWarehouseName(), t.getItemCount(), t.getTotalQuantity(),
                t.getRecordedBy(), t.getStatus()});
        }
        return rows;
    }

    @Override
    protected void showDetails(int documentId) throws SQLException {
        StockTransfer t = transferDAO.getTransferById(documentId);
        if (t == null) {
            Dialogs.warning(this, "This document no longer exists.");
            return;
        }
        List<Object[]> items = new ArrayList<>();
        int number = 1;
        for (TransferItem item : t.getItems()) {
            items.add(new Object[]{number++, item.getSku(), item.getProductName(), item.getUnit(), item.getQuantity()});
        }
        String[][] header = {
            {"Reference No.", t.getReferenceNo()}, {"Date", Formats.date(t.getDocumentDate())},
            {"From Warehouse", t.getFromWarehouseName()}, {"To Warehouse", t.getToWarehouseName()},
            {"Recorded By", t.getRecordedBy()}, {"Recorded On", Formats.dateTime(t.getCreatedAt())},
            {"Remarks", t.getRemarks()}
        };
        new TransactionDetailsDialog(SwingUtilities.getWindowAncestor(this), "Transfer " + t.getReferenceNo(),
                t.getStatus(), header,
                new String[]{"#", "SKU", "Product", "Unit", "Quantity"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, Integer.class},
                items, t.getItems().size() + " item(s)     Total units: " + Formats.number(t.getItemsQuantity()))
                .setVisible(true);
    }

    @Override
    protected void voidDocument(int documentId) throws SQLException, BusinessRuleException {
        transferDAO.voidTransfer(documentId);
    }

    @Override
    protected String describeVoid() {
        return "Its units will be MOVED BACK from the destination to the source warehouse.";
    }
}
