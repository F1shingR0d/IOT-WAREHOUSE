package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Release;
import model.ReleaseItem;
import model.Status;
import model.StockRequest;
import model.StockRequestItem;
import util.BusinessRuleException;

/**
 * Database operations for STOCK_REQUESTS and STOCK_REQUEST_ITEMS: customers
 * submit and cancel requests, staff approve, reject or release them.
 * Releasing a request creates a normal release document in the same
 * transaction, so the stock is only deducted at that moment.
 */
public class StockRequestDAO {

    private static final String SELECT_REQUESTS =
            "SELECT r.request_id, r.reference_no, r.request_date, r.needed_by, r.customer_id, c.customer_name, "
            + "r.warehouse_id, w.warehouse_name, r.remarks, r.status, r.reviewed_by, u.full_name AS reviewer, "
            + "r.reviewed_at, r.review_note, r.release_id, rl.reference_no AS release_ref, r.created_at, "
            + "COUNT(i.item_id) AS item_count, COALESCE(SUM(i.quantity), 0) AS total_qty "
            + "FROM STOCK_REQUESTS r "
            + "INNER JOIN CUSTOMERS c ON r.customer_id = c.customer_id "
            + "INNER JOIN WAREHOUSES w ON r.warehouse_id = w.warehouse_id "
            + "LEFT JOIN USERS u ON r.reviewed_by = u.user_id "
            + "LEFT JOIN RELEASES rl ON r.release_id = rl.release_id "
            + "LEFT JOIN STOCK_REQUEST_ITEMS i ON i.request_id = r.request_id ";

    private static final String GROUP_BY =
            " GROUP BY r.request_id, r.reference_no, r.request_date, r.needed_by, r.customer_id, c.customer_name, "
            + "r.warehouse_id, w.warehouse_name, r.remarks, r.status, r.reviewed_by, u.full_name, "
            + "r.reviewed_at, r.review_note, r.release_id, rl.reference_no, r.created_at";

    private static final String INSERT_HEADER =
            "INSERT INTO STOCK_REQUESTS (reference_no, request_date, needed_by, customer_id, warehouse_id, remarks) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_ITEM =
            "INSERT INTO STOCK_REQUEST_ITEMS (request_id, product_id, quantity) VALUES (?, ?, ?)";

    private final StockDAO stockDAO = new StockDAO();
    private final ReleaseDAO releaseDAO = new ReleaseDAO();

    // ------------------------------------------------------------------ customer actions

    /**
     * Saves a new request (status PENDING). The stock is NOT changed.
     *
     * @return the new request ID; the reference number is set on the request
     */
    public int submitRequest(StockRequest request) throws SQLException, BusinessRuleException {
        if (request.getItems().isEmpty()) {
            throw new BusinessRuleException("Add at least one product before submitting the request.");
        }
        if (request.getRequestDate() == null) {
            request.setRequestDate(LocalDate.now());
        }
        if (request.getNeededBy() != null && request.getNeededBy().isBefore(request.getRequestDate())) {
            throw new BusinessRuleException("The \"needed by\" date cannot be before the request date.");
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requireActiveCustomer(conn, request.getCustomerId());
                stockDAO.requireActiveWarehouse(conn, request.getWarehouseId());
                for (StockRequestItem item : request.getItems()) {
                    requireActiveProduct(conn, item.getProductId());
                }
                String referenceNo = ReferenceNumber.next(conn, "REQ", "STOCK_REQUESTS", request.getRequestDate());

                int requestId;
                try (PreparedStatement ps = DaoUtil.prepareInsert(conn, INSERT_HEADER)) {
                    ps.setString(1, referenceNo);
                    ps.setDate(2, Date.valueOf(request.getRequestDate()));
                    if (request.getNeededBy() == null) {
                        ps.setNull(3, java.sql.Types.DATE);
                    } else {
                        ps.setDate(3, Date.valueOf(request.getNeededBy()));
                    }
                    ps.setInt(4, request.getCustomerId());
                    ps.setInt(5, request.getWarehouseId());
                    DaoUtil.setText(ps, 6, request.getRemarks());
                    requestId = DaoUtil.insertAndGetId(ps);
                }
                try (PreparedStatement ps = conn.prepareStatement(INSERT_ITEM)) {
                    for (StockRequestItem item : request.getItems()) {
                        ps.setInt(1, requestId);
                        ps.setInt(2, item.getProductId());
                        ps.setInt(3, item.getQuantity());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();

                request.setRequestId(requestId);
                request.setReferenceNo(referenceNo);
                request.setStatus(Status.PENDING);
                return requestId;
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    /** A customer withdraws one of his or her own requests while it is still PENDING. */
    public void cancelRequest(int requestId, int customerId) throws SQLException, BusinessRuleException {
        String sql = "UPDATE STOCK_REQUESTS SET status = ? WHERE request_id = ? AND customer_id = ? AND status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Status.CANCELLED);
            ps.setInt(2, requestId);
            ps.setInt(3, customerId);
            ps.setString(4, Status.PENDING);
            if (ps.executeUpdate() == 0) {
                throw new BusinessRuleException("Only PENDING requests can be cancelled.\n"
                        + "This request was already reviewed by the warehouse. Please refresh the list.");
            }
        }
    }

    // ------------------------------------------------------------------ staff actions

    /** Accepts a PENDING request. The stock is not deducted until the request is released. */
    public void approveRequest(int requestId, int userId, String note) throws SQLException, BusinessRuleException {
        String sql = "UPDATE STOCK_REQUESTS SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, "
                + "review_note = ? WHERE request_id = ? AND status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Status.APPROVED);
            ps.setInt(2, userId);
            DaoUtil.setText(ps, 3, note);
            ps.setInt(4, requestId);
            ps.setString(5, Status.PENDING);
            if (ps.executeUpdate() == 0) {
                throw new BusinessRuleException("Only PENDING requests can be approved. Please refresh the list.");
            }
        }
    }

    /** Turns down a PENDING or APPROVED request; the reason is shown to the customer. */
    public void rejectRequest(int requestId, int userId, String reason) throws SQLException, BusinessRuleException {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessRuleException("Please enter the reason for rejecting the request.");
        }
        String sql = "UPDATE STOCK_REQUESTS SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, "
                + "review_note = ? WHERE request_id = ? AND status IN (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Status.REJECTED);
            ps.setInt(2, userId);
            ps.setString(3, reason.trim());
            ps.setInt(4, requestId);
            ps.setString(5, Status.PENDING);
            ps.setString(6, Status.APPROVED);
            if (ps.executeUpdate() == 0) {
                throw new BusinessRuleException("This request is no longer open (it was already released, rejected "
                        + "or cancelled). Please refresh the list.");
            }
        }
    }

    /**
     * Releases a PENDING or APPROVED request in ONE transaction: creates a
     * release document for the requested items (which deducts the stock),
     * marks the request FULFILLED and links it to the release. If any
     * product does not have enough stock, nothing is saved.
     *
     * @return the release that was created
     */
    public Release releaseRequest(int requestId, int userId, LocalDate releaseDate)
            throws SQLException, BusinessRuleException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // claim the request first, so two users cannot release it twice
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE STOCK_REQUESTS SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP "
                        + "WHERE request_id = ? AND status IN (?, ?)")) {
                    ps.setString(1, Status.FULFILLED);
                    ps.setInt(2, userId);
                    ps.setInt(3, requestId);
                    ps.setString(4, Status.PENDING);
                    ps.setString(5, Status.APPROVED);
                    if (ps.executeUpdate() == 0) {
                        throw new BusinessRuleException("This request is no longer open (it was already released, "
                                + "rejected or cancelled). Please refresh the list.");
                    }
                }
                StockRequest request = findById(conn, requestId);
                Release release = new Release(request.getWarehouseId(), request.getCustomerId(), userId);
                release.setDocumentDate(releaseDate);
                release.setRemarks(("Customer request " + request.getReferenceNo()
                        + (request.getRemarks() == null ? "" : " - " + request.getRemarks())));
                if (release.getRemarks().length() > 255) {
                    release.setRemarks(release.getRemarks().substring(0, 255));
                }
                for (StockRequestItem item : getItems(conn, requestId)) {
                    release.addItem(new ReleaseItem(item.getProductId(), item.getSku(), item.getProductName(),
                            item.getUnit(), item.getQuantity()));
                }
                int releaseId = releaseDAO.saveRelease(conn, release);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE STOCK_REQUESTS SET release_id = ? WHERE request_id = ?")) {
                    ps.setInt(1, releaseId);
                    ps.setInt(2, requestId);
                    ps.executeUpdate();
                }
                conn.commit();
                return release;
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------ queries

    /**
     * Searches requests by reference no., customer, warehouse or product.
     *
     * @param status     null = all statuses
     * @param customerId 0 = all customers (staff); a customer only sees his or her own requests
     */
    public List<StockRequest> searchRequests(String keyword, LocalDate from, LocalDate to, String status,
                                             int customerId) throws SQLException {
        List<Object> params = new ArrayList<>();
        String pattern = DaoUtil.likePattern(keyword);
        StringBuilder sql = new StringBuilder(SELECT_REQUESTS);
        sql.append("WHERE (UPPER(r.reference_no)").append(DaoUtil.LIKE)
           .append(" OR UPPER(c.customer_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(w.warehouse_name)").append(DaoUtil.LIKE)
           .append(" OR EXISTS (SELECT 1 FROM STOCK_REQUEST_ITEMS x INNER JOIN PRODUCTS p ON x.product_id = p.product_id")
           .append(" WHERE x.request_id = r.request_id AND (UPPER(p.product_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(p.sku)").append(DaoUtil.LIKE).append(")))");
        for (int i = 0; i < 5; i++) {
            params.add(pattern);
        }
        if (from != null) {
            sql.append(" AND r.request_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND r.request_date <= ?");
            params.add(to);
        }
        if (status != null) {
            sql.append(" AND r.status = ?");
            params.add(status);
        }
        if (customerId > 0) {
            sql.append(" AND r.customer_id = ?");
            params.add(customerId);
        }
        sql.append(GROUP_BY).append(" ORDER BY r.request_date DESC, r.request_id DESC");

        List<StockRequest> list = new ArrayList<>();
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

    /**
     * One request with its items and the current stock of each item.
     *
     * @param customerId 0 = any customer (staff); otherwise null is returned
     *                   when the request belongs to another customer
     */
    public StockRequest getRequestById(int requestId, int customerId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            StockRequest request = findById(conn, requestId);
            if (request == null || (customerId > 0 && request.getCustomerId() != customerId)) {
                return null;
            }
            request.getItems().addAll(getItems(conn, requestId));
            return request;
        }
    }

    /**
     * Number of requests per status.
     *
     * @param customerId 0 = all customers
     */
    public Map<String, Integer> countByStatus(int customerId) throws SQLException {
        String sql = "SELECT status, COUNT(*) FROM STOCK_REQUESTS"
                + (customerId > 0 ? " WHERE customer_id = ?" : "") + " GROUP BY status";
        Map<String, Integer> counts = new HashMap<>();
        for (String status : new String[]{Status.PENDING, Status.APPROVED, Status.FULFILLED, Status.REJECTED,
            Status.CANCELLED}) {
            counts.put(status, 0);
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (customerId > 0) {
                ps.setInt(1, customerId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return counts;
    }

    private StockRequest findById(Connection conn, int requestId) throws SQLException {
        String sql = SELECT_REQUESTS + "WHERE r.request_id = ?" + GROUP_BY;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapHeader(rs) : null;
            }
        }
    }

    private List<StockRequestItem> getItems(Connection conn, int requestId) throws SQLException {
        String sql = "SELECT i.item_id, i.request_id, i.product_id, p.sku, p.product_name, p.unit, p.reorder_level, "
                + "i.quantity, COALESCE(s.quantity, 0) AS available "
                + "FROM STOCK_REQUEST_ITEMS i "
                + "INNER JOIN STOCK_REQUESTS r ON i.request_id = r.request_id "
                + "INNER JOIN PRODUCTS p ON i.product_id = p.product_id "
                + "LEFT JOIN WAREHOUSE_STOCK s ON s.product_id = i.product_id AND s.warehouse_id = r.warehouse_id "
                + "WHERE i.request_id = ? ORDER BY i.item_id";
        List<StockRequestItem> items = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockRequestItem item = new StockRequestItem(rs.getInt("product_id"), rs.getString("sku"),
                            rs.getString("product_name"), rs.getString("unit"), rs.getInt("quantity"));
                    item.setItemId(rs.getInt("item_id"));
                    item.setRequestId(rs.getInt("request_id"));
                    item.setReorderLevel(rs.getInt("reorder_level"));
                    item.setAvailable(rs.getInt("available"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private void requireActiveCustomer(Connection conn, int customerId) throws SQLException, BusinessRuleException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM CUSTOMERS WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !Status.ACTIVE.equals(rs.getString(1))) {
                    throw new BusinessRuleException("Your customer record is not active.\n"
                            + "Please contact the warehouse before sending new requests.");
                }
            }
        }
    }

    private void requireActiveProduct(Connection conn, int productId) throws SQLException, BusinessRuleException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT product_name, status FROM PRODUCTS WHERE product_id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BusinessRuleException("A product in your list no longer exists. Please refresh the screen.");
                }
                if (!Status.ACTIVE.equals(rs.getString("status"))) {
                    throw new BusinessRuleException(rs.getString("product_name")
                            + " is no longer available for requests. Please remove it from the list.");
                }
            }
        }
    }

    private StockRequest mapHeader(ResultSet rs) throws SQLException {
        StockRequest request = new StockRequest(rs.getInt("customer_id"), rs.getInt("warehouse_id"));
        request.setRequestId(rs.getInt("request_id"));
        request.setReferenceNo(rs.getString("reference_no"));
        request.setRequestDate(rs.getDate("request_date").toLocalDate());
        Date neededBy = rs.getDate("needed_by");
        request.setNeededBy(neededBy == null ? null : neededBy.toLocalDate());
        request.setCustomerName(rs.getString("customer_name"));
        request.setWarehouseName(rs.getString("warehouse_name"));
        request.setRemarks(rs.getString("remarks"));
        request.setStatus(rs.getString("status"));
        request.setReviewedBy(rs.getInt("reviewed_by"));
        request.setReviewedByName(rs.getString("reviewer"));
        request.setReviewedAt(rs.getTimestamp("reviewed_at"));
        request.setReviewNote(rs.getString("review_note"));
        request.setReleaseId(rs.getInt("release_id"));
        request.setReleaseReference(rs.getString("release_ref"));
        request.setCreatedAt(rs.getTimestamp("created_at"));
        request.setItemCount(rs.getInt("item_count"));
        request.setTotalQuantity(rs.getInt("total_qty"));
        return request;
    }
}
