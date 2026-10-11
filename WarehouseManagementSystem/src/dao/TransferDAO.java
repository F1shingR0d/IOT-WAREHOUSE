package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Status;
import model.StockTransfer;
import model.TransferItem;
import util.BusinessRuleException;
import util.InsufficientStockException;

/**
 * Database operations for STOCK_TRANSFERS and TRANSFER_ITEMS
 * (stock moved between warehouses).
 */
public class TransferDAO {

    /** WAREHOUSES is joined twice: once as the source (fw) and once as the destination (tw). */
    private static final String SELECT_TRANSFERS =
            "SELECT t.transfer_id, t.reference_no, t.transfer_date, t.from_warehouse_id, fw.warehouse_name AS from_name, "
            + "t.to_warehouse_id, tw.warehouse_name AS to_name, t.user_id, u.full_name, t.remarks, t.status, "
            + "t.created_at, COUNT(i.item_id) AS item_count, COALESCE(SUM(i.quantity), 0) AS total_qty "
            + "FROM STOCK_TRANSFERS t "
            + "INNER JOIN WAREHOUSES fw ON t.from_warehouse_id = fw.warehouse_id "
            + "INNER JOIN WAREHOUSES tw ON t.to_warehouse_id = tw.warehouse_id "
            + "INNER JOIN USERS u ON t.user_id = u.user_id "
            + "LEFT JOIN TRANSFER_ITEMS i ON i.transfer_id = t.transfer_id ";

    private static final String GROUP_BY =
            " GROUP BY t.transfer_id, t.reference_no, t.transfer_date, t.from_warehouse_id, fw.warehouse_name, "
            + "t.to_warehouse_id, tw.warehouse_name, t.user_id, u.full_name, t.remarks, t.status, t.created_at";

    private static final String INSERT_HEADER =
            "INSERT INTO STOCK_TRANSFERS (reference_no, transfer_date, from_warehouse_id, to_warehouse_id, "
            + "user_id, remarks) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ITEM =
            "INSERT INTO TRANSFER_ITEMS (transfer_id, product_id, quantity) VALUES (?, ?, ?)";

    private final StockDAO stockDAO = new StockDAO();

    /**
     * Saves a transfer in one transaction: each item is deducted from the
     * source warehouse and added to the destination warehouse.
     */
    public int saveTransfer(StockTransfer transfer) throws SQLException, BusinessRuleException {
        if (transfer.getItems().isEmpty()) {
            throw new BusinessRuleException("Add at least one product before saving the transfer.");
        }
        if (transfer.getFromWarehouseId() == transfer.getToWarehouseId()) {
            throw new BusinessRuleException("The source and destination warehouses must be different.");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                stockDAO.requireActiveWarehouse(conn, transfer.getFromWarehouseId());
                stockDAO.requireActiveWarehouse(conn, transfer.getToWarehouseId());
                stockDAO.checkCapacity(conn, transfer.getToWarehouseId(), transfer.getItemsQuantity());
                String referenceNo = ReferenceNumber.next(conn, "TRF", "STOCK_TRANSFERS", transfer.getDocumentDate());

                int transferId;
                try (PreparedStatement ps = DaoUtil.prepareInsert(conn, INSERT_HEADER)) {
                    ps.setString(1, referenceNo);
                    ps.setDate(2, Date.valueOf(transfer.getDocumentDate()));
                    ps.setInt(3, transfer.getFromWarehouseId());
                    ps.setInt(4, transfer.getToWarehouseId());
                    ps.setInt(5, transfer.getUserId());
                    DaoUtil.setText(ps, 6, transfer.getRemarks());
                    transferId = DaoUtil.insertAndGetId(ps);
                }
                try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM)) {
                    for (TransferItem item : transfer.getItems()) {
                        ps.setInt(1, transferId);
                        ps.setInt(2, item.getProductId());
                        ps.setInt(3, item.getQuantity());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                for (TransferItem item : transfer.getItems()) {
                    stockDAO.deductStock(conn, transfer.getFromWarehouseId(), item.getProductId(), item.getQuantity());
                    stockDAO.addStock(conn, transfer.getToWarehouseId(), item.getProductId(), item.getQuantity());
                }
                conn.commit();

                transfer.setTransferId(transferId);
                transfer.setReferenceNo(referenceNo);
                transfer.setStatus(Status.POSTED);
                return transferId;
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    /**
     * Voids a transfer: the units go back from the destination to the source
     * warehouse. Not allowed when the destination already used the units.
     */
    public void voidTransfer(int transferId) throws SQLException, BusinessRuleException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StockTransfer transfer = findById(conn, transferId);
                if (transfer == null) {
                    throw new BusinessRuleException("The transfer record was not found. It may have been removed.");
                }
                if (transfer.isVoided()) {
                    throw new BusinessRuleException(transfer.getReferenceNo() + " is already voided.");
                }
                List<TransferItem> items = getItems(conn, transferId);
                int units = 0;
                for (TransferItem item : items) {
                    try {
                        stockDAO.deductStock(conn, transfer.getToWarehouseId(), item.getProductId(), item.getQuantity());
                    } catch (InsufficientStockException e) {
                        throw new BusinessRuleException("Cannot void " + transfer.getReferenceNo() + ".\n"
                                + "Only " + e.getAvailable() + " of the " + item.getQuantity() + " units of "
                                + e.getProductName() + " are still in " + e.getWarehouseName()
                                + ".\nThe rest were already released or transferred.");
                    }
                    units += item.getQuantity();
                }
                stockDAO.checkCapacity(conn, transfer.getFromWarehouseId(), units);
                for (TransferItem item : items) {
                    stockDAO.addStock(conn, transfer.getFromWarehouseId(), item.getProductId(), item.getQuantity());
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE STOCK_TRANSFERS SET status = ? WHERE transfer_id = ? AND status = ?")) {
                    ps.setString(1, Status.VOIDED);
                    ps.setInt(2, transferId);
                    ps.setString(3, Status.POSTED);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    /** Searches transfers by reference no., either warehouse, user or product. */
    public List<StockTransfer> searchTransfers(String keyword, LocalDate from, LocalDate to, String status)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String pattern = DaoUtil.likePattern(keyword);
        StringBuilder sql = new StringBuilder(SELECT_TRANSFERS);
        sql.append("WHERE (UPPER(t.reference_no)").append(DaoUtil.LIKE)
           .append(" OR UPPER(fw.warehouse_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(tw.warehouse_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(u.full_name)").append(DaoUtil.LIKE)
           .append(" OR EXISTS (SELECT 1 FROM TRANSFER_ITEMS x INNER JOIN PRODUCTS p ON x.product_id = p.product_id")
           .append(" WHERE x.transfer_id = t.transfer_id AND (UPPER(p.product_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(p.sku)").append(DaoUtil.LIKE).append(")))");
        for (int i = 0; i < 6; i++) {
            params.add(pattern);
        }
        if (from != null) {
            sql.append(" AND t.transfer_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND t.transfer_date <= ?");
            params.add(to);
        }
        if (status != null) {
            sql.append(" AND t.status = ?");
            params.add(status);
        }
        sql.append(GROUP_BY).append(" ORDER BY t.transfer_date DESC, t.transfer_id DESC");

        List<StockTransfer> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapHeader(rs));
                }
            }
        }
        return list;
    }

    public StockTransfer getTransferById(int transferId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            StockTransfer transfer = findById(conn, transferId);
            if (transfer != null) {
                transfer.getItems().addAll(getItems(conn, transferId));
            }
            return transfer;
        }
    }

    private StockTransfer findById(Connection conn, int transferId) throws SQLException {
        String sql = SELECT_TRANSFERS + "WHERE t.transfer_id = ?" + GROUP_BY;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transferId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapHeader(rs) : null;
            }
        }
    }

    private List<TransferItem> getItems(Connection conn, int transferId) throws SQLException {
        String sql = "SELECT i.item_id, i.transfer_id, i.product_id, p.sku, p.product_name, p.unit, i.quantity "
                + "FROM TRANSFER_ITEMS i INNER JOIN PRODUCTS p ON i.product_id = p.product_id "
                + "WHERE i.transfer_id = ? ORDER BY i.item_id";
        List<TransferItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transferId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransferItem item = new TransferItem(rs.getInt("product_id"), rs.getString("sku"),
                            rs.getString("product_name"), rs.getString("unit"), rs.getInt("quantity"));
                    item.setItemId(rs.getInt("item_id"));
                    item.setTransferId(rs.getInt("transfer_id"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private StockTransfer mapHeader(ResultSet rs) throws SQLException {
        StockTransfer transfer = new StockTransfer(rs.getInt("from_warehouse_id"), rs.getInt("to_warehouse_id"),
                rs.getInt("user_id"));
        transfer.setTransferId(rs.getInt("transfer_id"));
        transfer.setReferenceNo(rs.getString("reference_no"));
        transfer.setDocumentDate(rs.getDate("transfer_date").toLocalDate());
        transfer.setFromWarehouseName(rs.getString("from_name"));
        transfer.setToWarehouseName(rs.getString("to_name"));
        transfer.setRecordedBy(rs.getString("full_name"));
        transfer.setRemarks(rs.getString("remarks"));
        transfer.setStatus(rs.getString("status"));
        transfer.setCreatedAt(rs.getTimestamp("created_at"));
        transfer.setItemCount(rs.getInt("item_count"));
        transfer.setTotalQuantity(rs.getInt("total_qty"));
        return transfer;
    }
}
