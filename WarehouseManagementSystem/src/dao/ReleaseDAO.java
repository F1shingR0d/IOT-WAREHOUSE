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
import model.Release;
import model.ReleaseItem;
import model.Status;
import util.BusinessRuleException;

/**
 * Database operations for RELEASES and RELEASE_ITEMS (stock OUT).
 */
public class ReleaseDAO {

    private static final String SELECT_RELEASES =
            "SELECT r.release_id, r.reference_no, r.release_date, r.warehouse_id, w.warehouse_name, "
            + "r.customer_id, c.customer_name, r.user_id, u.full_name, r.remarks, r.status, r.created_at, "
            + "COUNT(i.item_id) AS item_count, COALESCE(SUM(i.quantity), 0) AS total_qty "
            + "FROM RELEASES r "
            + "INNER JOIN WAREHOUSES w ON r.warehouse_id = w.warehouse_id "
            + "INNER JOIN CUSTOMERS c ON r.customer_id = c.customer_id "
            + "INNER JOIN USERS u ON r.user_id = u.user_id "
            + "LEFT JOIN RELEASE_ITEMS i ON i.release_id = r.release_id ";

    private static final String GROUP_BY =
            " GROUP BY r.release_id, r.reference_no, r.release_date, r.warehouse_id, w.warehouse_name, "
            + "r.customer_id, c.customer_name, r.user_id, u.full_name, r.remarks, r.status, r.created_at";

    private static final String INSERT_HEADER =
            "INSERT INTO RELEASES (reference_no, release_date, warehouse_id, customer_id, user_id, remarks) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ITEM =
            "INSERT INTO RELEASE_ITEMS (release_id, product_id, quantity) VALUES (?, ?, ?)";

    private final StockDAO stockDAO = new StockDAO();

    /**
     * Saves a release and DEDUCTS its quantities from the warehouse stock in
     * one transaction. If any product does not have enough stock, nothing
     * is saved.
     */
    public int saveRelease(Release release) throws SQLException, BusinessRuleException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int releaseId = saveRelease(conn, release);
                conn.commit();
                return releaseId;
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    /**
     * Inserts the release and deducts the stock using the caller's connection
     * (no commit), so it can be part of a bigger transaction such as
     * releasing a customer stock request.
     */
    int saveRelease(Connection conn, Release release) throws SQLException, BusinessRuleException {
        if (release.getItems().isEmpty()) {
            throw new BusinessRuleException("Add at least one product before saving the release.");
        }
        stockDAO.requireActiveWarehouse(conn, release.getWarehouseId());
        String referenceNo = ReferenceNumber.next(conn, "REL", "RELEASES", release.getDocumentDate());

        int releaseId;
        try (PreparedStatement ps = DaoUtil.prepareInsert(conn, INSERT_HEADER)) {
            ps.setString(1, referenceNo);
            ps.setDate(2, Date.valueOf(release.getDocumentDate()));
            ps.setInt(3, release.getWarehouseId());
            ps.setInt(4, release.getCustomerId());
            ps.setInt(5, release.getUserId());
            DaoUtil.setText(ps, 6, release.getRemarks());
            releaseId = DaoUtil.insertAndGetId(ps);
        }
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM)) {
            for (ReleaseItem item : release.getItems()) {
                ps.setInt(1, releaseId);
                ps.setInt(2, item.getProductId());
                ps.setInt(3, item.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        for (ReleaseItem item : release.getItems()) {
            stockDAO.deductStock(conn, release.getWarehouseId(), item.getProductId(), item.getQuantity());
        }
        release.setReleaseId(releaseId);
        release.setReferenceNo(referenceNo);
        release.setStatus(Status.POSTED);
        return releaseId;
    }

    /**
     * Voids a release: the units are returned to the warehouse and the status
     * becomes VOIDED. A customer request that this release delivered goes back
     * to APPROVED so it can be released again.
     */
    public void voidRelease(int releaseId) throws SQLException, BusinessRuleException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Release release = findById(conn, releaseId);
                if (release == null) {
                    throw new BusinessRuleException("The release record was not found. It may have been removed.");
                }
                if (release.isVoided()) {
                    throw new BusinessRuleException(release.getReferenceNo() + " is already voided.");
                }
                List<ReleaseItem> items = getItems(conn, releaseId);
                int units = 0;
                for (ReleaseItem item : items) {
                    units += item.getQuantity();
                }
                stockDAO.checkCapacity(conn, release.getWarehouseId(), units);
                for (ReleaseItem item : items) {
                    stockDAO.addStock(conn, release.getWarehouseId(), item.getProductId(), item.getQuantity());
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE RELEASES SET status = ? WHERE release_id = ? AND status = ?")) {
                    ps.setString(1, Status.VOIDED);
                    ps.setInt(2, releaseId);
                    ps.setString(3, Status.POSTED);
                    ps.executeUpdate();
                }
                // A customer request delivered by this release is open again (APPROVED).
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE STOCK_REQUESTS SET status = ?, release_id = NULL WHERE release_id = ?")) {
                    ps.setString(1, Status.APPROVED);
                    ps.setInt(2, releaseId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    /** Searches releases by reference no., warehouse, customer, user or product. */
    public List<Release> searchReleases(String keyword, LocalDate from, LocalDate to, String status)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        String pattern = DaoUtil.likePattern(keyword);
        StringBuilder sql = new StringBuilder(SELECT_RELEASES);
        sql.append("WHERE (UPPER(r.reference_no)").append(DaoUtil.LIKE)
           .append(" OR UPPER(w.warehouse_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(c.customer_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(u.full_name)").append(DaoUtil.LIKE)
           .append(" OR EXISTS (SELECT 1 FROM RELEASE_ITEMS x INNER JOIN PRODUCTS p ON x.product_id = p.product_id")
           .append(" WHERE x.release_id = r.release_id AND (UPPER(p.product_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(p.sku)").append(DaoUtil.LIKE).append(")))");
        for (int i = 0; i < 6; i++) {
            params.add(pattern);
        }
        if (from != null) {
            sql.append(" AND r.release_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND r.release_date <= ?");
            params.add(to);
        }
        if (status != null) {
            sql.append(" AND r.status = ?");
            params.add(status);
        }
        sql.append(GROUP_BY).append(" ORDER BY r.release_date DESC, r.release_id DESC");

        List<Release> list = new ArrayList<>();
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

    public Release getReleaseById(int releaseId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Release release = findById(conn, releaseId);
            if (release != null) {
                release.getItems().addAll(getItems(conn, releaseId));
            }
            return release;
        }
    }

    private Release findById(Connection conn, int releaseId) throws SQLException {
        String sql = SELECT_RELEASES + "WHERE r.release_id = ?" + GROUP_BY;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, releaseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapHeader(rs) : null;
            }
        }
    }

    private List<ReleaseItem> getItems(Connection conn, int releaseId) throws SQLException {
        String sql = "SELECT i.item_id, i.release_id, i.product_id, p.sku, p.product_name, p.unit, i.quantity "
                + "FROM RELEASE_ITEMS i INNER JOIN PRODUCTS p ON i.product_id = p.product_id "
                + "WHERE i.release_id = ? ORDER BY i.item_id";
        List<ReleaseItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, releaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReleaseItem item = new ReleaseItem(rs.getInt("product_id"), rs.getString("sku"),
                            rs.getString("product_name"), rs.getString("unit"), rs.getInt("quantity"));
                    item.setItemId(rs.getInt("item_id"));
                    item.setReleaseId(rs.getInt("release_id"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private Release mapHeader(ResultSet rs) throws SQLException {
        Release release = new Release(rs.getInt("warehouse_id"), rs.getInt("customer_id"), rs.getInt("user_id"));
        release.setReleaseId(rs.getInt("release_id"));
        release.setReferenceNo(rs.getString("reference_no"));
        release.setDocumentDate(rs.getDate("release_date").toLocalDate());
        release.setWarehouseName(rs.getString("warehouse_name"));
        release.setCustomerName(rs.getString("customer_name"));
        release.setRecordedBy(rs.getString("full_name"));
        release.setRemarks(rs.getString("remarks"));
        release.setStatus(rs.getString("status"));
        release.setCreatedAt(rs.getTimestamp("created_at"));
        release.setItemCount(rs.getInt("item_count"));
        release.setTotalQuantity(rs.getInt("total_qty"));
        return release;
    }
}
