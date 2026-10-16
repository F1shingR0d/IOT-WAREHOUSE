package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Status;
import model.WarehouseStock;
import util.BusinessRuleException;
import util.Formats;
import util.InsufficientStockException;

/**
 * Database operations for WAREHOUSE_STOCK, the stock balance of every product
 * in every warehouse.
 *
 * The package-level methods receive the caller's Connection so that they run
 * inside the same database transaction as the receiving / release / transfer
 * that is being saved (all-or-nothing).
 */
public class StockDAO {

    // ------------------------------------------------------------------
    // Used inside transactions (same connection as the document being saved)
    // ------------------------------------------------------------------

    /** Adds stock; creates the WAREHOUSE_STOCK row the first time a product enters a warehouse. */
    void addStock(Connection conn, int warehouseId, int productId, int quantity) throws SQLException {
        String update = "UPDATE WAREHOUSE_STOCK SET quantity = quantity + ?, last_updated = CURRENT_TIMESTAMP "
                + "WHERE warehouse_id = ? AND product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            ps.setInt(1, quantity);
            ps.setInt(2, warehouseId);
            ps.setInt(3, productId);
            if (ps.executeUpdate() > 0) {
                return;
            }
        }
        String insert = "INSERT INTO WAREHOUSE_STOCK (warehouse_id, product_id, quantity) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setInt(1, warehouseId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            ps.executeUpdate();
        }
    }

    /**
     * Deducts stock. The condition "quantity >= ?" makes the check and the
     * update one atomic step, so the stock can never become negative.
     */
    void deductStock(Connection conn, int warehouseId, int productId, int quantity)
            throws SQLException, InsufficientStockException {
        String sql = "UPDATE WAREHOUSE_STOCK SET quantity = quantity - ?, last_updated = CURRENT_TIMESTAMP "
                + "WHERE warehouse_id = ? AND product_id = ? AND quantity >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, warehouseId);
            ps.setInt(3, productId);
            ps.setInt(4, quantity);
            if (ps.executeUpdate() == 0) {
                throw new InsufficientStockException(productName(conn, productId),
                        warehouseName(conn, warehouseId), getQuantity(conn, warehouseId, productId), quantity);
            }
        }
    }

    /** Current quantity of a product in a warehouse (0 when it was never stocked there). */
    int getQuantity(Connection conn, int warehouseId, int productId) throws SQLException {
        return DaoUtil.queryInt(conn,
                "SELECT quantity FROM WAREHOUSE_STOCK WHERE warehouse_id = ? AND product_id = ?",
                warehouseId, productId);
    }

    /** Fails when the warehouse does not exist or is INACTIVE. */
    void requireActiveWarehouse(Connection conn, int warehouseId) throws SQLException, BusinessRuleException {
        String sql = "SELECT warehouse_name, status FROM WAREHOUSES WHERE warehouse_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessRuleException("The selected warehouse no longer exists. Please refresh the screen.");
                }
                if (!Status.ACTIVE.equals(rs.getString("status"))) {
                    throw new BusinessRuleException(rs.getString("warehouse_name")
                            + " is INACTIVE and cannot be used for new transactions.");
                }
            }
        }
    }

    /** Fails when the incoming units do not fit in the warehouse's remaining capacity. */
    void checkCapacity(Connection conn, int warehouseId, int incomingUnits) throws SQLException, BusinessRuleException {
        String sql = "SELECT w.warehouse_name, w.capacity, "
                + "(SELECT COALESCE(SUM(s.quantity), 0) FROM WAREHOUSE_STOCK s WHERE s.warehouse_id = w.warehouse_id) AS used "
                + "FROM WAREHOUSES w WHERE w.warehouse_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessRuleException("The selected warehouse no longer exists. Please refresh the screen.");
                }
                int capacity = rs.getInt("capacity");
                int used = rs.getInt("used");
                if ((long) used + incomingUnits > capacity) {
                    throw new BusinessRuleException(rs.getString("warehouse_name") + " does not have enough space for "
                            + Formats.number(incomingUnits) + " more units.\n"
                            + "Capacity: " + Formats.number(capacity)
                            + "   Currently stored: " + Formats.number(used)
                            + "   Free space: " + Formats.number(Math.max(0, capacity - used)));
                }
            }
        }
    }

    private String productName(Connection conn, int productId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sku, product_name FROM PRODUCTS WHERE product_id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("product_name") + " (" + rs.getString("sku") + ")" : "product #" + productId;
            }
        }
    }

    private String warehouseName(Connection conn, int warehouseId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT warehouse_name FROM WAREHOUSES WHERE warehouse_id = ?")) {
            ps.setInt(1, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "warehouse #" + warehouseId;
            }
        }
    }

    // ------------------------------------------------------------------
    // Read-only methods used by the screens
    // ------------------------------------------------------------------

    /** Units of a product available in a warehouse. */
    public int getAvailable(int warehouseId, int productId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return getQuantity(conn, warehouseId, productId);
        }
    }

    /**
     * Stock on hand per warehouse and product (joins 4 tables).
     *
     * @param warehouseId 0 = all warehouses
     * @param categoryId  0 = all categories
     * @param keyword     SKU or product name, empty = all
     * @param lowOnly     true = only rows at or below the reorder level
     */
    public List<WarehouseStock> getStockOnHand(int warehouseId, int categoryId, String keyword, boolean lowOnly)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String pattern = DaoUtil.likePattern(keyword);
        StringBuilder sql = new StringBuilder(
                "SELECT s.warehouse_id, w.warehouse_code, w.warehouse_name, s.product_id, p.sku, p.product_name, "
                + "c.category_name, p.unit, s.quantity, p.reorder_level, p.unit_cost, s.last_updated "
                + "FROM WAREHOUSE_STOCK s "
                + "INNER JOIN WAREHOUSES w ON s.warehouse_id = w.warehouse_id "
                + "INNER JOIN PRODUCTS p ON s.product_id = p.product_id "
                + "INNER JOIN CATEGORIES c ON p.category_id = c.category_id ");
        sql.append("WHERE (UPPER(p.sku)").append(DaoUtil.LIKE)
           .append(" OR UPPER(p.product_name)").append(DaoUtil.LIKE).append(")");
        params.add(pattern);
        params.add(pattern);
        if (warehouseId > 0) {
            sql.append(" AND s.warehouse_id = ?");
            params.add(warehouseId);
        }
        if (categoryId > 0) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }
        if (lowOnly) {
            sql.append(" AND s.quantity <= p.reorder_level");
        }
        sql.append(" ORDER BY w.warehouse_code, p.product_name");

        List<WarehouseStock> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WarehouseStock stock = new WarehouseStock(rs.getInt("warehouse_id"),
                            rs.getInt("product_id"), rs.getInt("quantity"));
                    stock.setWarehouseCode(rs.getString("warehouse_code"));
                    stock.setWarehouseName(rs.getString("warehouse_name"));
                    stock.setSku(rs.getString("sku"));
                    stock.setProductName(rs.getString("product_name"));
                    stock.setCategoryName(rs.getString("category_name"));
                    stock.setUnit(rs.getString("unit"));
                    stock.setReorderLevel(rs.getInt("reorder_level"));
                    stock.setUnitCost(rs.getBigDecimal("unit_cost"));
                    stock.setLastUpdated(rs.getTimestamp("last_updated"));
                    list.add(stock);
                }
            }
        }
        return list;
    }

    /**
     * The Customer Portal catalog: every ACTIVE product with its stock in one
     * warehouse. The LEFT JOIN also lists products that were never stocked
     * there (quantity 0 = OUT OF STOCK).
     *
     * @param categoryId 0 = all categories
     * @param keyword    SKU, product name or description, empty = all
     */
    public List<WarehouseStock> getCatalog(int warehouseId, int categoryId, String keyword) throws SQLException {
        List<Object> params = new ArrayList<>();
        String pattern = DaoUtil.likePattern(keyword);
        StringBuilder sql = new StringBuilder(
                "SELECT p.product_id, p.sku, p.product_name, c.category_name, p.unit, p.reorder_level, p.description, "
                + "COALESCE(s.quantity, 0) AS quantity "
                + "FROM PRODUCTS p "
                + "INNER JOIN CATEGORIES c ON p.category_id = c.category_id "
                + "LEFT JOIN WAREHOUSE_STOCK s ON s.product_id = p.product_id AND s.warehouse_id = ? ");
        params.add(warehouseId);
        sql.append("WHERE p.status = ? AND (UPPER(p.sku)").append(DaoUtil.LIKE)
           .append(" OR UPPER(p.product_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(p.description, ''))").append(DaoUtil.LIKE).append(")");
        params.add(Status.ACTIVE);
        params.add(pattern);
        params.add(pattern);
        params.add(pattern);
        if (categoryId > 0) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }
        sql.append(" ORDER BY c.category_name, p.product_name");

        List<WarehouseStock> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WarehouseStock stock = new WarehouseStock(warehouseId, rs.getInt("product_id"),
                            rs.getInt("quantity"));
                    stock.setSku(rs.getString("sku"));
                    stock.setProductName(rs.getString("product_name"));
                    stock.setCategoryName(rs.getString("category_name"));
                    stock.setUnit(rs.getString("unit"));
                    stock.setReorderLevel(rs.getInt("reorder_level"));
                    stock.setDescription(rs.getString("description"));
                    list.add(stock);
                }
            }
        }
        return list;
    }
}
