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
import model.Receiving;
import model.ReceivingItem;
import model.Status;
import util.BusinessRuleException;
import util.InsufficientStockException;

/**
 * Database operations for RECEIVINGS and RECEIVING_ITEMS (stock IN).
 */
public class ReceivingDAO {

    /** Header with warehouse, supplier and user names plus totals of the item lines. */
    private static final String SELECT_RECEIVINGS =
            "SELECT r.receiving_id, r.reference_no, r.receiving_date, r.warehouse_id, w.warehouse_name, "
            + "r.supplier_id, s.supplier_name, r.user_id, u.full_name, r.remarks, r.status, r.created_at, "
            + "COUNT(i.item_id) AS item_count, COALESCE(SUM(i.quantity), 0) AS total_qty, "
            + "COALESCE(SUM(i.quantity * i.unit_cost), 0) AS total_amount "
            + "FROM RECEIVINGS r "
            + "INNER JOIN WAREHOUSES w ON r.warehouse_id = w.warehouse_id "
            + "INNER JOIN SUPPLIERS s ON r.supplier_id = s.supplier_id "
            + "INNER JOIN USERS u ON r.user_id = u.user_id "
            + "LEFT JOIN RECEIVING_ITEMS i ON i.receiving_id = r.receiving_id ";

    private static final String GROUP_BY =
            " GROUP BY r.receiving_id, r.reference_no, r.receiving_date, r.warehouse_id, w.warehouse_name, "
            + "r.supplier_id, s.supplier_name, r.user_id, u.full_name, r.remarks, r.status, r.created_at";

    private static final String INSERT_HEADER =
            "INSERT INTO RECEIVINGS (reference_no, receiving_date, warehouse_id, supplier_id, user_id, remarks) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ITEM =
            "INSERT INTO RECEIVING_ITEMS (receiving_id, product_id, quantity, unit_cost) VALUES (?, ?, ?, ?)";

    private final StockDAO stockDAO = new StockDAO();

    /**
     * Saves a receiving and ADDS its quantities to the warehouse stock.
     * Header, items and stock are saved in ONE database transaction:
     * if anything fails, everything is rolled back.
     *
     * @return the new receiving_id (the reference number is set on the object)
     */
    public int saveReceiving(Receiving receiving) throws SQLException, BusinessRuleException {
        if (receiving.getItems().isEmpty()) {
            throw new BusinessRuleException("Add at least one product before saving the receiving.");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);  // start of the transaction
            try {
                stockDAO.requireActiveWarehouse(conn, receiving.getWarehouseId());
                stockDAO.checkCapacity(conn, receiving.getWarehouseId(), receiving.getItemsQuantity());
                String referenceNo = ReferenceNumber.next(conn, "RCV", "RECEIVINGS", receiving.getDocumentDate());

                int receivingId;
                try (PreparedStatement ps = DaoUtil.prepareInsert(conn, INSERT_HEADER)) {
                    ps.setString(1, referenceNo);
                    ps.setDate(2, Date.valueOf(receiving.getDocumentDate()));
                    ps.setInt(3, receiving.getWarehouseId());
                    ps.setInt(4, receiving.getSupplierId());
                    ps.setInt(5, receiving.getUserId());
                    DaoUtil.setText(ps, 6, receiving.getRemarks());
                    receivingId = DaoUtil.insertAndGetId(ps);
                }
                try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM)) {
                    for (ReceivingItem item : receiving.getItems()) {
                        ps.setInt(1, receivingId);
                        ps.setInt(2, item.getProductId());
                        ps.setInt(3, item.getQuantity());
                        ps.setBigDecimal(4, item.getUnitCost());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                for (ReceivingItem item : receiving.getItems()) {
                    stockDAO.addStock(conn, receiving.getWarehouseId(), item.getProductId(), item.getQuantity());
                }
                conn.commit();  // everything succeeded

                receiving.setReceivingId(receivingId);
                receiving.setReferenceNo(referenceNo);
                receiving.setStatus(Status.POSTED);
                return receivingId;
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);  // nothing is saved
                throw e;
            }
        }
    }

    /**
     * Voids a receiving: its units are removed from the warehouse again and
     * the status becomes VOIDED. Not allowed when the units were already
     * released or transferred out.
     */
    public void voidReceiving(int receivingId) throws SQLException, BusinessRuleException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Receiving receiving = findById(conn, receivingId);
                if (receiving == null) {
                    throw new BusinessRuleException("The receiving record was not found. It may have been removed.");
                }
                if (receiving.isVoided()) {
                    throw new BusinessRuleException(receiving.getReferenceNo() + " is already voided.");
                }
                for (ReceivingItem item : getItems(conn, receivingId)) {
                    try {
                        stockDAO.deductStock(conn, receiving.getWarehouseId(), item.getProductId(), item.getQuantity());
                    } catch (InsufficientStockException e) {
                        throw new BusinessRuleException("Cannot void " + receiving.getReferenceNo() + ".\n"
                                + "Only " + e.getAvailable() + " of the " + item.getQuantity() + " units of "
                                + e.getProductName() + " are still in " + e.getWarehouseName()
                                + ".\nThe rest were already released or transferred.");
                    }
                }
                setVoided(conn, receivingId);
                conn.commit();
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    private void setVoided(Connection conn, int receivingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE RECEIVINGS SET status = ? WHERE receiving_id = ? AND status = ?")) {
            ps.setString(1, Status.VOIDED);
            ps.setInt(2, receivingId);
            ps.setString(3, Status.POSTED);
            ps.executeUpdate();
        }
    }

    /**
     * Searches receivings by reference no., warehouse, supplier, user or a
     * product they contain, within an optional date range and status.
     */
    public List<Receiving> searchReceivings(String keyword, LocalDate from, LocalDate to, String status)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String pattern = DaoUtil.likePattern(keyword);
        StringBuilder sql = new StringBuilder(SELECT_RECEIVINGS);
        sql.append("WHERE (UPPER(r.reference_no)").append(DaoUtil.LIKE)
           .append(" OR UPPER(w.warehouse_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(s.supplier_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(u.full_name)").append(DaoUtil.LIKE)
           .append(" OR EXISTS (SELECT 1 FROM RECEIVING_ITEMS x INNER JOIN PRODUCTS p ON x.product_id = p.product_id")
           .append(" WHERE x.receiving_id = r.receiving_id AND (UPPER(p.product_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(p.sku)").append(DaoUtil.LIKE).append(")))");
        for (int i = 0; i < 6; i++) {
            params.add(pattern);
        }
        if (from != null) {
            sql.append(" AND r.receiving_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND r.receiving_date <= ?");
            params.add(to);
        }
        if (status != null) {
            sql.append(" AND r.status = ?");
            params.add(status);
        }
        sql.append(GROUP_BY).append(" ORDER BY r.receiving_date DESC, r.receiving_id DESC");

        List<Receiving> list = new ArrayList<>();
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

    /** Loads one receiving with all its item lines. */
    public Receiving getReceivingById(int receivingId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Receiving receiving = findById(conn, receivingId);
            if (receiving != null) {
                receiving.getItems().addAll(getItems(conn, receivingId));
            }
            return receiving;
        }
    }

    private Receiving findById(Connection conn, int receivingId) throws SQLException {
        String sql = SELECT_RECEIVINGS + "WHERE r.receiving_id = ?" + GROUP_BY;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, receivingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapHeader(rs) : null;
            }
        }
    }

    private List<ReceivingItem> getItems(Connection conn, int receivingId) throws SQLException {
        String sql = "SELECT i.item_id, i.receiving_id, i.product_id, p.sku, p.product_name, p.unit, "
                + "i.quantity, i.unit_cost FROM RECEIVING_ITEMS i "
                + "INNER JOIN PRODUCTS p ON i.product_id = p.product_id "
                + "WHERE i.receiving_id = ? ORDER BY i.item_id";
        List<ReceivingItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, receivingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReceivingItem item = new ReceivingItem(rs.getInt("product_id"), rs.getString("sku"),
                            rs.getString("product_name"), rs.getString("unit"), rs.getInt("quantity"),
                            rs.getBigDecimal("unit_cost"));
                    item.setItemId(rs.getInt("item_id"));
                    item.setReceivingId(rs.getInt("receiving_id"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private Receiving mapHeader(ResultSet rs) throws SQLException {
        Receiving receiving = new Receiving(rs.getInt("warehouse_id"), rs.getInt("supplier_id"), rs.getInt("user_id"));
        receiving.setReceivingId(rs.getInt("receiving_id"));
        receiving.setReferenceNo(rs.getString("reference_no"));
        receiving.setDocumentDate(rs.getDate("receiving_date").toLocalDate());
        receiving.setWarehouseName(rs.getString("warehouse_name"));
        receiving.setSupplierName(rs.getString("supplier_name"));
        receiving.setRecordedBy(rs.getString("full_name"));
        receiving.setRemarks(rs.getString("remarks"));
        receiving.setStatus(rs.getString("status"));
        receiving.setCreatedAt(rs.getTimestamp("created_at"));
        receiving.setItemCount(rs.getInt("item_count"));
        receiving.setTotalQuantity(rs.getInt("total_qty"));
        receiving.setTotalAmount(rs.getBigDecimal("total_amount"));
        return receiving;
    }
}
