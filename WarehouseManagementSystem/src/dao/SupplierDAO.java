package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Status;
import model.Supplier;

/**
 * Database operations for the SUPPLIERS table.
 */
public class SupplierDAO {

    private static final String SELECT_SUPPLIERS =
            "SELECT supplier_id, supplier_name, contact_person, contact_no, email, address, status "
            + "FROM SUPPLIERS ";

    public int addSupplier(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO SUPPLIERS (supplier_name, contact_person, contact_no, email, address, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = DaoUtil.prepareInsert(conn, sql)) {
            fillStatement(ps, supplier);
            int id = DaoUtil.insertAndGetId(ps);
            supplier.setSupplierId(id);
            return id;
        }
    }

    public boolean updateSupplier(Supplier supplier) throws SQLException {
        String sql = "UPDATE SUPPLIERS SET supplier_name = ?, contact_person = ?, contact_no = ?, "
                + "email = ?, address = ?, status = ? WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            fillStatement(ps, supplier);
            ps.setInt(7, supplier.getSupplierId());
            return ps.executeUpdate() > 0;
        }
    }

    private void fillStatement(PreparedStatement ps, Supplier supplier) throws SQLException {
        ps.setString(1, supplier.getSupplierName().trim());
        DaoUtil.setText(ps, 2, supplier.getContactPerson());
        ps.setString(3, supplier.getContactNo().trim());
        DaoUtil.setText(ps, 4, supplier.getEmail());
        DaoUtil.setText(ps, 5, supplier.getAddress());
        ps.setString(6, supplier.getStatus());
    }

    public boolean deleteSupplier(int supplierId) throws SQLException {
        String sql = "DELETE FROM SUPPLIERS WHERE supplier_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, supplierId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Supplier> getSuppliers() throws SQLException {
        return searchSuppliers("", null);
    }

    public List<Supplier> getActiveSuppliers() throws SQLException {
        return searchSuppliers("", Status.ACTIVE);
    }

    /** Searches by ID, name, contact person, phone number or e-mail. */
    public List<Supplier> searchSuppliers(String keyword, String status) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_SUPPLIERS);
        String pattern = DaoUtil.likePattern(keyword);
        sql.append("WHERE (UPPER(supplier_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(contact_person, ''))").append(DaoUtil.LIKE)
           .append(" OR UPPER(contact_no)").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(email, ''))").append(DaoUtil.LIKE);
        for (int i = 0; i < 4; i++) {
            params.add(pattern);
        }
        Integer id = DaoUtil.parseId(keyword);
        if (id != null) {
            sql.append(" OR supplier_id = ?");
            params.add(id);
        }
        sql.append(")");
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY supplier_name");

        List<Supplier> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Supplier supplier = new Supplier(rs.getInt("supplier_id"), rs.getString("supplier_name"),
                            rs.getString("contact_person"), rs.getString("contact_no"));
                    supplier.setEmail(rs.getString("email"));
                    supplier.setAddress(rs.getString("address"));
                    supplier.setStatus(rs.getString("status"));
                    list.add(supplier);
                }
            }
        }
        return list;
    }

    public boolean isNameTaken(String name, int excludeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM SUPPLIERS WHERE UPPER(supplier_name) = ? AND supplier_id <> ?",
                    name.trim().toUpperCase(), excludeId) > 0;
        }
    }
}
