package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Status;
import model.Warehouse;

/**
 * Database operations for the WAREHOUSES table.
 */
public class WarehouseDAO {

    /**
     * LEFT JOIN because the person in charge is optional.
     * The sub-query adds up the units currently stored in the warehouse.
     */
    private static final String SELECT_WAREHOUSES =
            "SELECT w.warehouse_id, w.warehouse_code, w.warehouse_name, w.location, w.capacity, "
            + "w.contact_no, w.manager_id, w.status, u.full_name AS manager_name, "
            + "(SELECT COALESCE(SUM(s.quantity), 0) FROM WAREHOUSE_STOCK s "
            + " WHERE s.warehouse_id = w.warehouse_id) AS stock_on_hand "
            + "FROM WAREHOUSES w LEFT JOIN USERS u ON w.manager_id = u.user_id ";

    public int addWarehouse(Warehouse warehouse) throws SQLException {
        String sql = "INSERT INTO WAREHOUSES (warehouse_code, warehouse_name, location, capacity, "
                + "contact_no, manager_id, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = DaoUtil.prepareInsert(conn, sql)) {
            fillStatement(ps, warehouse);
            int id = DaoUtil.insertAndGetId(ps);
            warehouse.setWarehouseId(id);
            return id;
        }
    }

    public boolean updateWarehouse(Warehouse warehouse) throws SQLException {
        String sql = "UPDATE WAREHOUSES SET warehouse_code = ?, warehouse_name = ?, location = ?, "
                + "capacity = ?, contact_no = ?, manager_id = ?, status = ? WHERE warehouse_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            fillStatement(ps, warehouse);
            ps.setInt(8, warehouse.getWarehouseId());
            return ps.executeUpdate() > 0;
        }
    }

    private void fillStatement(PreparedStatement ps, Warehouse warehouse) throws SQLException {
        ps.setString(1, warehouse.getWarehouseCode().trim().toUpperCase());
        ps.setString(2, warehouse.getWarehouseName().trim());
        ps.setString(3, warehouse.getLocation().trim());
        ps.setInt(4, warehouse.getCapacity());
        DaoUtil.setText(ps, 5, warehouse.getContactNo());
        DaoUtil.setInteger(ps, 6, warehouse.getManagerId());
        ps.setString(7, warehouse.getStatus());
    }

    public boolean deleteWarehouse(int warehouseId) throws SQLException {
        String sql = "DELETE FROM WAREHOUSES WHERE warehouse_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Warehouse> getWarehouses() throws SQLException {
        return searchWarehouses("", null);
    }

    /** Active warehouses only, for the transaction screens. */
    public List<Warehouse> getActiveWarehouses() throws SQLException {
        return searchWarehouses("", Status.ACTIVE);
    }

    /** Searches by ID, code, name, location or person in charge. */
    public List<Warehouse> searchWarehouses(String keyword, String status) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_WAREHOUSES);
        String pattern = DaoUtil.likePattern(keyword);
        sql.append("WHERE (UPPER(w.warehouse_code)").append(DaoUtil.LIKE)
           .append(" OR UPPER(w.warehouse_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(w.location)").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(u.full_name, ''))").append(DaoUtil.LIKE);
        for (int i = 0; i < 4; i++) {
            params.add(pattern);
        }
        Integer id = DaoUtil.parseId(keyword);
        if (id != null) {
            sql.append(" OR w.warehouse_id = ?");
            params.add(id);
        }
        sql.append(")");
        if (status != null) {
            sql.append(" AND w.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY w.warehouse_code");

        List<Warehouse> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Warehouse getWarehouseById(int warehouseId) throws SQLException {
        String sql = SELECT_WAREHOUSES + "WHERE w.warehouse_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean isCodeTaken(String code, int excludeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM WAREHOUSES WHERE UPPER(warehouse_code) = ? AND warehouse_id <> ?",
                    code.trim().toUpperCase(), excludeId) > 0;
        }
    }

    public boolean isNameTaken(String name, int excludeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM WAREHOUSES WHERE UPPER(warehouse_name) = ? AND warehouse_id <> ?",
                    name.trim().toUpperCase(), excludeId) > 0;
        }
    }

    private Warehouse mapRow(ResultSet rs) throws SQLException {
        Warehouse warehouse = new Warehouse(rs.getInt("warehouse_id"), rs.getString("warehouse_code"),
                rs.getString("warehouse_name"), rs.getString("location"), rs.getInt("capacity"));
        warehouse.setContactNo(rs.getString("contact_no"));
        int managerId = rs.getInt("manager_id");
        warehouse.setManagerId(rs.wasNull() ? null : managerId);
        warehouse.setManagerName(rs.getString("manager_name"));
        warehouse.setStatus(rs.getString("status"));
        warehouse.setStockOnHand(rs.getInt("stock_on_hand"));
        return warehouse;
    }
}
