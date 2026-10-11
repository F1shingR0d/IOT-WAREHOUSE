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
import model.DashboardStats;
import model.Status;

/**
 * Queries for the dashboard: summary numbers, recent transactions and
 * low-stock alerts.
 */
public class DashboardDAO {

    /** Products whose total stock is at or below the reorder level (GROUP BY + HAVING). */
    static final String LOW_STOCK_QUERY =
            "SELECT p.sku, p.product_name, p.unit, COALESCE(SUM(s.quantity), 0) AS on_hand, p.reorder_level "
            + "FROM PRODUCTS p LEFT JOIN WAREHOUSE_STOCK s ON s.product_id = p.product_id "
            + "WHERE p.status = ? "
            + "GROUP BY p.product_id, p.sku, p.product_name, p.unit, p.reorder_level "
            + "HAVING COALESCE(SUM(s.quantity), 0) <= p.reorder_level ";

    public DashboardStats getStats() throws SQLException {
        DashboardStats stats = new DashboardStats();
        LocalDate today = LocalDate.now();
        try (Connection conn = DatabaseConnection.getConnection()) {
            stats.setActiveWarehouses(DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM WAREHOUSES WHERE status = ?", Status.ACTIVE));
            stats.setActiveProducts(DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM PRODUCTS WHERE status = ?", Status.ACTIVE));

            String totals = "SELECT COALESCE(SUM(s.quantity), 0), COALESCE(SUM(s.quantity * p.unit_cost), 0) "
                    + "FROM WAREHOUSE_STOCK s INNER JOIN PRODUCTS p ON s.product_id = p.product_id";
            try (PreparedStatement ps = conn.prepareStatement(totals);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.setUnitsOnHand(rs.getLong(1));
                    stats.setStockValue(rs.getBigDecimal(2));
                }
            }
            stats.setLowStockProducts(DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM (" + LOW_STOCK_QUERY + ") AS low_items", Status.ACTIVE));

            int[] receiving = todayTotals(conn, "RECEIVINGS", "RECEIVING_ITEMS", "receiving_id", "receiving_date", today);
            stats.setTodayReceivings(receiving[0]);
            stats.setTodayReceivedUnits(receiving[1]);
            int[] releasing = todayTotals(conn, "RELEASES", "RELEASE_ITEMS", "release_id", "release_date", today);
            stats.setTodayReleases(releasing[0]);
            stats.setTodayReleasedUnits(releasing[1]);
            int[] transfers = todayTotals(conn, "STOCK_TRANSFERS", "TRANSFER_ITEMS", "transfer_id", "transfer_date", today);
            stats.setTodayTransfers(transfers[0]);
            stats.setTodayTransferredUnits(transfers[1]);
        }
        return stats;
    }

    /** Number of posted documents and units for one day. Table names are constants of this class. */
    private int[] todayTotals(Connection conn, String headerTable, String itemTable, String idColumn,
                              String dateColumn, LocalDate day) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT h." + idColumn + "), COALESCE(SUM(i.quantity), 0) "
                + "FROM " + headerTable + " h LEFT JOIN " + itemTable + " i ON i." + idColumn + " = h." + idColumn
                + " WHERE h." + dateColumn + " = ? AND h.status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(day));
            ps.setString(2, Status.POSTED);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new int[]{rs.getInt(1), rs.getInt(2)} : new int[]{0, 0};
            }
        }
    }

    /**
     * The latest documents of all three types, combined with UNION ALL.
     * Each row: date, reference no., type, details, units, status.
     */
    public List<Object[]> getRecentTransactions(int limit) throws SQLException {
        String sql =
                "SELECT r.receiving_date AS doc_date, r.reference_no, CAST('Receiving' AS VARCHAR(20)) AS doc_type, "
                + "s.supplier_name || ' → ' || w.warehouse_name AS details, "
                + "(SELECT COALESCE(SUM(i.quantity), 0) FROM RECEIVING_ITEMS i WHERE i.receiving_id = r.receiving_id) AS units, "
                + "r.status, r.created_at "
                + "FROM RECEIVINGS r INNER JOIN WAREHOUSES w ON r.warehouse_id = w.warehouse_id "
                + "INNER JOIN SUPPLIERS s ON r.supplier_id = s.supplier_id "
                + "UNION ALL "
                + "SELECT l.release_date, l.reference_no, CAST('Release' AS VARCHAR(20)), "
                + "w.warehouse_name || ' → ' || c.customer_name, "
                + "(SELECT COALESCE(SUM(i.quantity), 0) FROM RELEASE_ITEMS i WHERE i.release_id = l.release_id), "
                + "l.status, l.created_at "
                + "FROM RELEASES l INNER JOIN WAREHOUSES w ON l.warehouse_id = w.warehouse_id "
                + "INNER JOIN CUSTOMERS c ON l.customer_id = c.customer_id "
                + "UNION ALL "
                + "SELECT t.transfer_date, t.reference_no, CAST('Transfer' AS VARCHAR(20)), "
                + "fw.warehouse_name || ' → ' || tw.warehouse_name, "
                + "(SELECT COALESCE(SUM(i.quantity), 0) FROM TRANSFER_ITEMS i WHERE i.transfer_id = t.transfer_id), "
                + "t.status, t.created_at "
                + "FROM STOCK_TRANSFERS t INNER JOIN WAREHOUSES fw ON t.from_warehouse_id = fw.warehouse_id "
                + "INNER JOIN WAREHOUSES tw ON t.to_warehouse_id = tw.warehouse_id "
                + "ORDER BY 1 DESC, 7 DESC "
                + "FETCH FIRST " + Math.max(1, limit) + " ROWS ONLY";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getDate(1).toLocalDate().toString(), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getInt(5), rs.getString(6)
                });
            }
        }
        return rows;
    }

    /** Low-stock products, lowest stock first. Each row: SKU, product, on hand, reorder level. */
    public List<Object[]> getLowStockProducts() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(LOW_STOCK_QUERY + "ORDER BY 4, 2")) {
            ps.setString(1, Status.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString("sku"), rs.getString("product_name"),
                        rs.getInt("on_hand"), rs.getInt("reorder_level")
                    });
                }
            }
        }
        return rows;
    }
}
