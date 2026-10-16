package dao;

import database.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import model.Customer;
import model.Product;
import model.Receiving;
import model.Release;
import model.ReportData;
import model.ReportType;
import model.Status;
import model.StockRequest;
import model.StockTransfer;
import model.Supplier;
import model.User;
import model.Warehouse;
import model.WarehouseStock;
import util.Formats;

/**
 * Builds the reports shown in the Reports module.
 */
public class ReportDAO {

    private static final Class<?> TEXT = String.class;
    private static final Class<?> NUMBER = Integer.class;
    private static final Class<?> MONEY = BigDecimal.class;

    /**
     * Generates a report.
     *
     * @param from        start date (only for reports that use a date range)
     * @param to          end date
     * @param warehouseId 0 = all warehouses (only for reports with a warehouse filter)
     */
    public ReportData generate(ReportType type, LocalDate from, LocalDate to, int warehouseId) throws SQLException {
        switch (type) {
            case INVENTORY_STATUS:
                return inventoryStatus(warehouseId);
            case LOW_STOCK:
                return lowStock();
            case STOCK_MOVEMENT:
                return stockMovement(from, to, warehouseId);
            case RECEIVING:
                return receivingReport(from, to, warehouseId);
            case RELEASING:
                return releasingReport(from, to, warehouseId);
            case TRANSFERS:
                return transferReport(from, to, warehouseId);
            case CUSTOMER_REQUESTS:
                return customerRequestReport(from, to, warehouseId);
            case WAREHOUSE_SUMMARY:
                return warehouseSummary();
            case MONTHLY_SUMMARY:
                return monthlySummary(from, to);
            case PRODUCT_LIST:
                return productList();
            case SUPPLIER_LIST:
                return supplierList();
            case CUSTOMER_LIST:
                return customerList();
            case USER_LIST:
                return userList();
            default:
                throw new IllegalArgumentException("Unknown report: " + type);
        }
    }

    // ------------------------------------------------------------------ inventory

    private ReportData inventoryStatus(int warehouseId) throws SQLException {
        ReportData report = new ReportData(ReportType.INVENTORY_STATUS.getTitle(),
                new String[]{"Warehouse", "SKU", "Product", "Category", "Unit", "On Hand", "Reorder Level",
                    "Unit Cost", "Stock Value", "Status"},
                new Class<?>[]{TEXT, TEXT, TEXT, TEXT, TEXT, NUMBER, NUMBER, MONEY, MONEY, TEXT});
        long units = 0;
        BigDecimal value = BigDecimal.ZERO;
        int alerts = 0;
        for (WarehouseStock s : new StockDAO().getStockOnHand(warehouseId, 0, "", false)) {
            report.addRow(s.getWarehouseName(), s.getSku(), s.getProductName(), s.getCategoryName(), s.getUnit(),
                    s.getQuantity(), s.getReorderLevel(), s.getUnitCost(), s.getStockValue(), s.getStockStatus());
            units += s.getQuantity();
            value = value.add(s.getStockValue());
            if (!WarehouseStock.STATUS_OK.equals(s.getStockStatus())) {
                alerts++;
            }
        }
        report.setSummary(report.getRowCount() + " stock lines   |   Units on hand: " + Formats.number(units)
                + "   |   Stock value: " + Formats.money(value) + "   |   LOW/OUT lines: " + alerts);
        return report;
    }

    private ReportData lowStock() throws SQLException {
        ReportData report = new ReportData(ReportType.LOW_STOCK.getTitle(),
                new String[]{"SKU", "Product", "Category", "Unit", "On Hand", "Reorder Level", "Shortage",
                    "Suggested Order"},
                new Class<?>[]{TEXT, TEXT, TEXT, TEXT, NUMBER, NUMBER, NUMBER, NUMBER});
        String sql = "SELECT p.sku, p.product_name, c.category_name, p.unit, "
                + "COALESCE(SUM(s.quantity), 0) AS on_hand, p.reorder_level "
                + "FROM PRODUCTS p INNER JOIN CATEGORIES c ON p.category_id = c.category_id "
                + "LEFT JOIN WAREHOUSE_STOCK s ON s.product_id = p.product_id "
                + "WHERE p.status = ? "
                + "GROUP BY p.product_id, p.sku, p.product_name, c.category_name, p.unit, p.reorder_level "
                + "HAVING COALESCE(SUM(s.quantity), 0) <= p.reorder_level "
                + "ORDER BY 5, 2";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Status.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int onHand = rs.getInt("on_hand");
                    int reorder = rs.getInt("reorder_level");
                    int shortage = Math.max(0, reorder - onHand);
                    int suggested = Math.max(0, reorder * 2 - onHand);   // refill up to twice the reorder level
                    report.addRow(rs.getString("sku"), rs.getString("product_name"), rs.getString("category_name"),
                            rs.getString("unit"), onHand, reorder, shortage, suggested);
                }
            }
        }
        report.setSummary(report.getRowCount() + " product(s) at or below the reorder level. "
                + "Suggested order = 2 x reorder level - on hand.");
        return report;
    }

    // ------------------------------------------------------------------ movements

    /**
     * All stock movements in the period, combined from 4 queries with UNION ALL:
     * receiving (IN), release (OUT), transfer source (OUT) and transfer destination (IN).
     */
    private ReportData stockMovement(LocalDate from, LocalDate to, int warehouseId) throws SQLException {
        ReportData report = new ReportData(ReportType.STOCK_MOVEMENT.getTitle(),
                new String[]{"Date", "Reference", "Movement", "Warehouse", "SKU", "Product", "Qty In", "Qty Out",
                    "From / To", "Recorded By"},
                new Class<?>[]{TEXT, TEXT, TEXT, TEXT, TEXT, TEXT, NUMBER, NUMBER, TEXT, TEXT});
        List<Object> params = new ArrayList<>();
        String warehouseFilter = warehouseId > 0 ? " AND %s = ?" : "";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.receiving_date AS move_date, r.reference_no, CAST('IN - Receiving' AS VARCHAR(20)) AS movement, ")
           .append("w.warehouse_name, p.sku, p.product_name, i.quantity AS qty_in, 0 AS qty_out, ")
           .append("s.supplier_name AS party, u.full_name ")
           .append("FROM RECEIVING_ITEMS i INNER JOIN RECEIVINGS r ON i.receiving_id = r.receiving_id ")
           .append("INNER JOIN WAREHOUSES w ON r.warehouse_id = w.warehouse_id ")
           .append("INNER JOIN PRODUCTS p ON i.product_id = p.product_id ")
           .append("INNER JOIN SUPPLIERS s ON r.supplier_id = s.supplier_id ")
           .append("INNER JOIN USERS u ON r.user_id = u.user_id ")
           .append("WHERE r.status = 'POSTED' AND r.receiving_date BETWEEN ? AND ?")
           .append(String.format(warehouseFilter, "r.warehouse_id"));
        addRangeParams(params, from, to, warehouseId);

        sql.append(" UNION ALL ")
           .append("SELECT l.release_date, l.reference_no, CAST('OUT - Release' AS VARCHAR(20)), ")
           .append("w.warehouse_name, p.sku, p.product_name, 0, i.quantity, c.customer_name, u.full_name ")
           .append("FROM RELEASE_ITEMS i INNER JOIN RELEASES l ON i.release_id = l.release_id ")
           .append("INNER JOIN WAREHOUSES w ON l.warehouse_id = w.warehouse_id ")
           .append("INNER JOIN PRODUCTS p ON i.product_id = p.product_id ")
           .append("INNER JOIN CUSTOMERS c ON l.customer_id = c.customer_id ")
           .append("INNER JOIN USERS u ON l.user_id = u.user_id ")
           .append("WHERE l.status = 'POSTED' AND l.release_date BETWEEN ? AND ?")
           .append(String.format(warehouseFilter, "l.warehouse_id"));
        addRangeParams(params, from, to, warehouseId);

        sql.append(" UNION ALL ")
           .append("SELECT t.transfer_date, t.reference_no, CAST('OUT - Transfer' AS VARCHAR(20)), ")
           .append("fw.warehouse_name, p.sku, p.product_name, 0, i.quantity, 'To ' || tw.warehouse_name, u.full_name ")
           .append("FROM TRANSFER_ITEMS i INNER JOIN STOCK_TRANSFERS t ON i.transfer_id = t.transfer_id ")
           .append("INNER JOIN WAREHOUSES fw ON t.from_warehouse_id = fw.warehouse_id ")
           .append("INNER JOIN WAREHOUSES tw ON t.to_warehouse_id = tw.warehouse_id ")
           .append("INNER JOIN PRODUCTS p ON i.product_id = p.product_id ")
           .append("INNER JOIN USERS u ON t.user_id = u.user_id ")
           .append("WHERE t.status = 'POSTED' AND t.transfer_date BETWEEN ? AND ?")
           .append(String.format(warehouseFilter, "t.from_warehouse_id"));
        addRangeParams(params, from, to, warehouseId);

        sql.append(" UNION ALL ")
           .append("SELECT t.transfer_date, t.reference_no, CAST('IN - Transfer' AS VARCHAR(20)), ")
           .append("tw.warehouse_name, p.sku, p.product_name, i.quantity, 0, 'From ' || fw.warehouse_name, u.full_name ")
           .append("FROM TRANSFER_ITEMS i INNER JOIN STOCK_TRANSFERS t ON i.transfer_id = t.transfer_id ")
           .append("INNER JOIN WAREHOUSES fw ON t.from_warehouse_id = fw.warehouse_id ")
           .append("INNER JOIN WAREHOUSES tw ON t.to_warehouse_id = tw.warehouse_id ")
           .append("INNER JOIN PRODUCTS p ON i.product_id = p.product_id ")
           .append("INNER JOIN USERS u ON t.user_id = u.user_id ")
           .append("WHERE t.status = 'POSTED' AND t.transfer_date BETWEEN ? AND ?")
           .append(String.format(warehouseFilter, "t.to_warehouse_id"));
        addRangeParams(params, from, to, warehouseId);

        sql.append(" ORDER BY 1, 2, 3");

        long totalIn = 0;
        long totalOut = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int in = rs.getInt(7);
                    int out = rs.getInt(8);
                    report.addRow(rs.getDate(1).toLocalDate().toString(), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6), in, out, rs.getString(9),
                            rs.getString(10));
                    totalIn += in;
                    totalOut += out;
                }
            }
        }
        report.setSummary(report.getRowCount() + " movement(s)   |   Units in: " + Formats.number(totalIn)
                + "   |   Units out: " + Formats.number(totalOut)
                + "   |   Net change: " + Formats.number(totalIn - totalOut));
        return report;
    }

    private void addRangeParams(List<Object> params, LocalDate from, LocalDate to, int warehouseId) {
        params.add(from);
        params.add(to);
        if (warehouseId > 0) {
            params.add(warehouseId);
        }
    }

    // ------------------------------------------------------------------ documents

    private ReportData receivingReport(LocalDate from, LocalDate to, int warehouseId) throws SQLException {
        ReportData report = new ReportData(ReportType.RECEIVING.getTitle(),
                new String[]{"Reference", "Date", "Warehouse", "Supplier", "Items", "Units", "Total Cost",
                    "Recorded By", "Status"},
                new Class<?>[]{TEXT, TEXT, TEXT, TEXT, NUMBER, NUMBER, MONEY, TEXT, TEXT});
        int posted = 0;
        long units = 0;
        BigDecimal amount = BigDecimal.ZERO;
        for (Receiving r : new ReceivingDAO().searchReceivings("", from, to, null)) {
            if (warehouseId > 0 && r.getWarehouseId() != warehouseId) {
                continue;
            }
            report.addRow(r.getReferenceNo(), Formats.date(r.getDocumentDate()), r.getWarehouseName(),
                    r.getSupplierName(), r.getItemCount(), r.getTotalQuantity(), r.getTotalAmount(),
                    r.getRecordedBy(), r.getStatus());
            if (!r.isVoided()) {
                posted++;
                units += r.getTotalQuantity();
                amount = amount.add(r.getTotalAmount());
            }
        }
        report.setSummary(report.getRowCount() + " document(s), " + posted + " posted   |   Units received: "
                + Formats.number(units) + "   |   Total cost: " + Formats.money(amount) + "   (voided excluded)");
        return report;
    }

    private ReportData releasingReport(LocalDate from, LocalDate to, int warehouseId) throws SQLException {
        ReportData report = new ReportData(ReportType.RELEASING.getTitle(),
                new String[]{"Reference", "Date", "Warehouse", "Customer", "Items", "Units", "Recorded By", "Status"},
                new Class<?>[]{TEXT, TEXT, TEXT, TEXT, NUMBER, NUMBER, TEXT, TEXT});
        int posted = 0;
        long units = 0;
        for (Release r : new ReleaseDAO().searchReleases("", from, to, null)) {
            if (warehouseId > 0 && r.getWarehouseId() != warehouseId) {
                continue;
            }
            report.addRow(r.getReferenceNo(), Formats.date(r.getDocumentDate()), r.getWarehouseName(),
                    r.getCustomerName(), r.getItemCount(), r.getTotalQuantity(), r.getRecordedBy(), r.getStatus());
            if (!r.isVoided()) {
                posted++;
                units += r.getTotalQuantity();
            }
        }
        report.setSummary(report.getRowCount() + " document(s), " + posted + " posted   |   Units released: "
                + Formats.number(units) + "   (voided excluded)");
        return report;
    }

    private ReportData customerRequestReport(LocalDate from, LocalDate to, int warehouseId) throws SQLException {
        ReportData report = new ReportData(ReportType.CUSTOMER_REQUESTS.getTitle(),
                new String[]{"Reference", "Request Date", "Customer", "Warehouse", "Items", "Units", "Status",
                    "Release No.", "Reviewed By"},
                new Class<?>[]{TEXT, TEXT, TEXT, TEXT, NUMBER, NUMBER, TEXT, TEXT, TEXT});
        Map<String, Integer> perStatus = new TreeMap<>();
        long releasedUnits = 0;
        for (StockRequest r : new StockRequestDAO().searchRequests("", from, to, null, 0)) {
            if (warehouseId > 0 && r.getWarehouseId() != warehouseId) {
                continue;
            }
            report.addRow(r.getReferenceNo(), Formats.date(r.getRequestDate()), r.getCustomerName(),
                    r.getWarehouseName(), r.getItemCount(), r.getTotalQuantity(), r.getStatus(),
                    r.getReleaseReference(), r.getReviewedByName());
            perStatus.merge(r.getStatus(), 1, Integer::sum);
            if (Status.FULFILLED.equals(r.getStatus())) {
                releasedUnits += r.getTotalQuantity();
            }
        }
        StringBuilder summary = new StringBuilder(report.getRowCount() + " request(s)");
        for (Map.Entry<String, Integer> entry : perStatus.entrySet()) {
            summary.append("   |   ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        report.setSummary(summary + "   |   Units released: " + Formats.number(releasedUnits));
        return report;
    }

    private ReportData transferReport(LocalDate from, LocalDate to, int warehouseId) throws SQLException {
        ReportData report = new ReportData(ReportType.TRANSFERS.getTitle(),
                new String[]{"Reference", "Date", "From Warehouse", "To Warehouse", "Items", "Units",
                    "Recorded By", "Status"},
                new Class<?>[]{TEXT, TEXT, TEXT, TEXT, NUMBER, NUMBER, TEXT, TEXT});
        int posted = 0;
        long units = 0;
        for (StockTransfer t : new TransferDAO().searchTransfers("", from, to, null)) {
            if (warehouseId > 0 && t.getFromWarehouseId() != warehouseId && t.getToWarehouseId() != warehouseId) {
                continue;
            }
            report.addRow(t.getReferenceNo(), Formats.date(t.getDocumentDate()), t.getFromWarehouseName(),
                    t.getToWarehouseName(), t.getItemCount(), t.getTotalQuantity(), t.getRecordedBy(), t.getStatus());
            if (!t.isVoided()) {
                posted++;
                units += t.getTotalQuantity();
            }
        }
        report.setSummary(report.getRowCount() + " document(s), " + posted + " posted   |   Units transferred: "
                + Formats.number(units) + "   (voided excluded)");
        return report;
    }

    // ------------------------------------------------------------------ summaries

    private ReportData warehouseSummary() throws SQLException {
        ReportData report = new ReportData(ReportType.WAREHOUSE_SUMMARY.getTitle(),
                new String[]{"Code", "Warehouse", "Location", "In-charge", "Products", "Units on Hand", "Capacity",
                    "Utilization", "Stock Value", "Status"},
                new Class<?>[]{TEXT, TEXT, TEXT, TEXT, NUMBER, NUMBER, NUMBER, Double.class, MONEY, TEXT});
        String sql = "SELECT w.warehouse_code, w.warehouse_name, w.location, u.full_name, "
                + "COUNT(CASE WHEN s.quantity > 0 THEN 1 END) AS products, "
                + "COALESCE(SUM(s.quantity), 0) AS units, w.capacity, "
                + "COALESCE(SUM(s.quantity * p.unit_cost), 0) AS stock_value, w.status "
                + "FROM WAREHOUSES w "
                + "LEFT JOIN USERS u ON w.manager_id = u.user_id "
                + "LEFT JOIN WAREHOUSE_STOCK s ON s.warehouse_id = w.warehouse_id "
                + "LEFT JOIN PRODUCTS p ON s.product_id = p.product_id "
                + "GROUP BY w.warehouse_id, w.warehouse_code, w.warehouse_name, w.location, u.full_name, "
                + "w.capacity, w.status "
                + "ORDER BY w.warehouse_code";
        long units = 0;
        long capacity = 0;
        BigDecimal value = BigDecimal.ZERO;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int whUnits = rs.getInt("units");
                int whCapacity = rs.getInt("capacity");
                BigDecimal whValue = rs.getBigDecimal("stock_value");
                String inCharge = rs.getString("full_name");
                report.addRow(rs.getString("warehouse_code"), rs.getString("warehouse_name"), rs.getString("location"),
                        inCharge == null ? "(none)" : inCharge, rs.getInt("products"), whUnits, whCapacity,
                        whCapacity == 0 ? 0.0 : whUnits * 100.0 / whCapacity, whValue,
                        rs.getString("status"));
                units += whUnits;
                capacity += whCapacity;
                value = value.add(whValue);
            }
        }
        report.setSummary(report.getRowCount() + " warehouse(s)   |   Units on hand: " + Formats.number(units)
                + " of " + Formats.number(capacity) + " capacity ("
                + Formats.percent(capacity == 0 ? 0 : units * 100.0 / capacity) + ")   |   Stock value: "
                + Formats.money(value));
        return report;
    }

    /** Documents and units per month; the three results are merged by month in Java. */
    private ReportData monthlySummary(LocalDate from, LocalDate to) throws SQLException {
        ReportData report = new ReportData(ReportType.MONTHLY_SUMMARY.getTitle(),
                new String[]{"Month", "Receivings", "Units In", "Releases", "Units Out", "Transfers",
                    "Units Transferred", "Net Change"},
                new Class<?>[]{TEXT, NUMBER, NUMBER, NUMBER, NUMBER, NUMBER, NUMBER, NUMBER});
        Map<YearMonth, int[]> months = new TreeMap<>();
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            months.put(m, new int[6]);
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            addMonthly(conn, months, 0, "RECEIVINGS", "RECEIVING_ITEMS", "receiving_id", "receiving_date", from, to);
            addMonthly(conn, months, 2, "RELEASES", "RELEASE_ITEMS", "release_id", "release_date", from, to);
            addMonthly(conn, months, 4, "STOCK_TRANSFERS", "TRANSFER_ITEMS", "transfer_id", "transfer_date", from, to);
        }
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("yyyy-MM (MMM)");
        int[] total = new int[6];
        for (Map.Entry<YearMonth, int[]> entry : months.entrySet()) {
            int[] v = entry.getValue();
            report.addRow(entry.getKey().format(monthFormat), v[0], v[1], v[2], v[3], v[4], v[5], v[1] - v[3]);
            for (int i = 0; i < 6; i++) {
                total[i] += v[i];
            }
        }
        report.setSummary("Totals   |   Receivings: " + total[0] + " (" + Formats.number(total[1]) + " units)"
                + "   |   Releases: " + total[2] + " (" + Formats.number(total[3]) + " units)"
                + "   |   Transfers: " + total[4] + " (" + Formats.number(total[5]) + " units)");
        return report;
    }

    /** Uses a derived table so YEAR() and MONTH() can be grouped. Table names are constants. */
    private void addMonthly(Connection conn, Map<YearMonth, int[]> months, int offset, String headerTable,
                            String itemTable, String idColumn, String dateColumn, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "SELECT yr, mo, COUNT(DISTINCT doc_id), SUM(qty) FROM ("
                + "SELECT YEAR(h." + dateColumn + ") AS yr, MONTH(h." + dateColumn + ") AS mo, "
                + "h." + idColumn + " AS doc_id, i.quantity AS qty "
                + "FROM " + headerTable + " h INNER JOIN " + itemTable + " i ON i." + idColumn + " = h." + idColumn
                + " WHERE h.status = 'POSTED' AND h." + dateColumn + " BETWEEN ? AND ?) AS monthly "
                + "GROUP BY yr, mo";
        List<Object> params = new ArrayList<>();
        params.add(from);
        params.add(to);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    YearMonth month = YearMonth.of(rs.getInt(1), rs.getInt(2));
                    int[] values = months.computeIfAbsent(month, m -> new int[6]);
                    values[offset] += rs.getInt(3);
                    values[offset + 1] += rs.getInt(4);
                }
            }
        }
    }

    // ------------------------------------------------------------------ master lists

    private ReportData productList() throws SQLException {
        ReportData report = new ReportData(ReportType.PRODUCT_LIST.getTitle(),
                new String[]{"ID", "SKU", "Product", "Category", "Unit", "Unit Cost", "Reorder Level", "Total Stock",
                    "Status"},
                new Class<?>[]{NUMBER, TEXT, TEXT, TEXT, TEXT, MONEY, NUMBER, NUMBER, TEXT});
        int active = 0;
        for (Product p : new ProductDAO().getProducts()) {
            report.addRow(p.getProductId(), p.getSku(), p.getProductName(), p.getCategoryName(), p.getUnit(),
                    p.getUnitCost(), p.getReorderLevel(), p.getTotalStock(), p.getStatus());
            if (Status.ACTIVE.equals(p.getStatus())) {
                active++;
            }
        }
        report.setSummary(report.getRowCount() + " product(s), " + active + " active");
        return report;
    }

    private ReportData supplierList() throws SQLException {
        ReportData report = new ReportData(ReportType.SUPPLIER_LIST.getTitle(),
                new String[]{"ID", "Supplier", "Contact Person", "Contact No.", "E-mail", "Address", "Status"},
                new Class<?>[]{NUMBER, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT});
        for (Supplier s : new SupplierDAO().getSuppliers()) {
            report.addRow(s.getSupplierId(), s.getSupplierName(), s.getContactPerson(), s.getContactNo(),
                    s.getEmail(), s.getAddress(), s.getStatus());
        }
        report.setSummary(report.getRowCount() + " supplier(s)");
        return report;
    }

    private ReportData customerList() throws SQLException {
        ReportData report = new ReportData(ReportType.CUSTOMER_LIST.getTitle(),
                new String[]{"ID", "Customer", "Contact Person", "Contact No.", "E-mail", "Address", "Status"},
                new Class<?>[]{NUMBER, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT});
        for (Customer c : new CustomerDAO().getCustomers()) {
            report.addRow(c.getCustomerId(), c.getCustomerName(), c.getContactPerson(), c.getContactNo(),
                    c.getEmail(), c.getAddress(), c.getStatus());
        }
        report.setSummary(report.getRowCount() + " customer(s)");
        return report;
    }

    private ReportData userList() throws SQLException {
        ReportData report = new ReportData(ReportType.USER_LIST.getTitle(),
                new String[]{"ID", "Username", "Full Name", "Role", "Status", "Created"},
                new Class<?>[]{NUMBER, TEXT, TEXT, TEXT, TEXT, TEXT});
        for (User u : new UserDAO().getUsers()) {
            report.addRow(u.getUserId(), u.getUsername(), u.getFullName(), u.getRole().getDisplayName(),
                    u.getStatus(), Formats.dateTime(u.getCreatedAt()));
        }
        report.setSummary(report.getRowCount() + " user account(s)");
        return report;
    }

    /** Active warehouses for the report filter. */
    public List<Warehouse> getWarehouses() throws SQLException {
        return new WarehouseDAO().getWarehouses();
    }
}
