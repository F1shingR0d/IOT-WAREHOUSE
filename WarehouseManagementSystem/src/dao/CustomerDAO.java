package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Customer;
import model.Status;

/**
 * Database operations for the CUSTOMERS table.
 */
public class CustomerDAO {

    private static final String SELECT_CUSTOMERS =
            "SELECT customer_id, customer_name, contact_person, contact_no, email, address, status "
            + "FROM CUSTOMERS ";

    public int addCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO CUSTOMERS (customer_name, contact_person, contact_no, email, address, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = DaoUtil.prepareInsert(conn, sql)) {
            fillStatement(ps, customer);
            int id = DaoUtil.insertAndGetId(ps);
            customer.setCustomerId(id);
            return id;
        }
    }

    public boolean updateCustomer(Customer customer) throws SQLException {
        String sql = "UPDATE CUSTOMERS SET customer_name = ?, contact_person = ?, contact_no = ?, "
                + "email = ?, address = ?, status = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            fillStatement(ps, customer);
            ps.setInt(7, customer.getCustomerId());
            return ps.executeUpdate() > 0;
        }
    }

    private void fillStatement(PreparedStatement ps, Customer customer) throws SQLException {
        ps.setString(1, customer.getCustomerName().trim());
        DaoUtil.setText(ps, 2, customer.getContactPerson());
        ps.setString(3, customer.getContactNo().trim());
        DaoUtil.setText(ps, 4, customer.getEmail());
        DaoUtil.setText(ps, 5, customer.getAddress());
        ps.setString(6, customer.getStatus());
    }

    public boolean deleteCustomer(int customerId) throws SQLException {
        String sql = "DELETE FROM CUSTOMERS WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Customer> getCustomers() throws SQLException {
        return searchCustomers("", null);
    }

    public List<Customer> getActiveCustomers() throws SQLException {
        return searchCustomers("", Status.ACTIVE);
    }

    /** Searches by ID, name, contact person, phone number or e-mail. */
    public List<Customer> searchCustomers(String keyword, String status) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_CUSTOMERS);
        String pattern = DaoUtil.likePattern(keyword);
        sql.append("WHERE (UPPER(customer_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(contact_person, ''))").append(DaoUtil.LIKE)
           .append(" OR UPPER(contact_no)").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(email, ''))").append(DaoUtil.LIKE);
        for (int i = 0; i < 4; i++) {
            params.add(pattern);
        }
        Integer id = DaoUtil.parseId(keyword);
        if (id != null) {
            sql.append(" OR customer_id = ?");
            params.add(id);
        }
        sql.append(")");
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY customer_name");

        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Customer customer = new Customer(rs.getInt("customer_id"), rs.getString("customer_name"),
                            rs.getString("contact_person"), rs.getString("contact_no"));
                    customer.setEmail(rs.getString("email"));
                    customer.setAddress(rs.getString("address"));
                    customer.setStatus(rs.getString("status"));
                    list.add(customer);
                }
            }
        }
        return list;
    }

    public boolean isNameTaken(String name, int excludeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM CUSTOMERS WHERE UPPER(customer_name) = ? AND customer_id <> ?",
                    name.trim().toUpperCase(), excludeId) > 0;
        }
    }
}
