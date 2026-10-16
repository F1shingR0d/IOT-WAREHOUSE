package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Customer;
import model.CustomerAccount;
import model.Status;
import util.BusinessRuleException;
import util.PasswordUtil;

/**
 * Database operations for CUSTOMER_ACCOUNTS: customer registration, login
 * and the account approval done by the warehouse staff.
 */
public class CustomerAccountDAO {

    private static final String SELECT_ACCOUNTS =
            "SELECT a.account_id, a.customer_id, a.username, a.password_hash, a.status, a.created_at, a.last_login, "
            + "c.customer_name, c.status AS customer_status, c.contact_person, c.contact_no, c.email "
            + "FROM CUSTOMER_ACCOUNTS a INNER JOIN CUSTOMERS c ON a.customer_id = c.customer_id ";

    /**
     * Checks the username and password of a customer account.
     *
     * @return the account when the password is correct, otherwise null.
     *         The caller still has to check the account status (PENDING,
     *         ACTIVE or INACTIVE) and the customer status.
     */
    public CustomerAccount authenticate(String username, char[] password) throws SQLException {
        CustomerAccount account = getAccountByUsername(username);
        if (account == null || !PasswordUtil.verify(password, account.getPasswordHash())) {
            return null;
        }
        return account;
    }

    public CustomerAccount getAccountByUsername(String username) throws SQLException {
        return findOne("WHERE UPPER(a.username) = ?", username.trim().toUpperCase());
    }

    public CustomerAccount getAccountById(int accountId) throws SQLException {
        return findOne("WHERE a.account_id = ?", accountId);
    }

    private CustomerAccount findOne(String where, Object value) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACCOUNTS + where)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Registers a new customer: creates the CUSTOMERS record (INACTIVE) and
     * its login account (PENDING) in one transaction. Both become ACTIVE when
     * the warehouse approves the account.
     *
     * @return the new account ID
     */
    public int register(Customer customer, String username, char[] password)
            throws SQLException, BusinessRuleException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (DaoUtil.queryInt(conn, "SELECT COUNT(*) FROM CUSTOMERS WHERE UPPER(customer_name) = ?",
                        customer.getCustomerName().trim().toUpperCase()) > 0) {
                    throw new BusinessRuleException("\"" + customer.getCustomerName().trim()
                            + "\" is already registered with the warehouse.\n"
                            + "Please contact the warehouse to get a login account for it.");
                }
                if (isUsernameTaken(conn, username)) {
                    throw new BusinessRuleException("The username \"" + username.trim()
                            + "\" is already taken. Please choose another one.");
                }
                int customerId;
                String insertCustomer = "INSERT INTO CUSTOMERS (customer_name, contact_person, contact_no, email, "
                        + "address, status) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = DaoUtil.prepareInsert(conn, insertCustomer)) {
                    ps.setString(1, customer.getCustomerName().trim());
                    DaoUtil.setText(ps, 2, customer.getContactPerson());
                    ps.setString(3, customer.getContactNo().trim());
                    DaoUtil.setText(ps, 4, customer.getEmail());
                    DaoUtil.setText(ps, 5, customer.getAddress());
                    ps.setString(6, Status.INACTIVE);   // not offered in Releasing until approved
                    customerId = DaoUtil.insertAndGetId(ps);
                }
                int accountId;
                String insertAccount = "INSERT INTO CUSTOMER_ACCOUNTS (customer_id, username, password_hash, status) "
                        + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = DaoUtil.prepareInsert(conn, insertAccount)) {
                    ps.setInt(1, customerId);
                    ps.setString(2, username.trim());
                    ps.setString(3, PasswordUtil.hash(password));
                    ps.setString(4, Status.PENDING);
                    accountId = DaoUtil.insertAndGetId(ps);
                }
                conn.commit();
                customer.setCustomerId(customerId);
                customer.setStatus(Status.INACTIVE);
                return accountId;
            } catch (SQLException | BusinessRuleException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    /** Creates a login account for an existing customer (used for the demo accounts). */
    public int addAccount(int customerId, String username, char[] password, String status) throws SQLException {
        String sql = "INSERT INTO CUSTOMER_ACCOUNTS (customer_id, username, password_hash, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = DaoUtil.prepareInsert(conn, sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, username.trim());
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, status);
            return DaoUtil.insertAndGetId(ps);
        }
    }

    /**
     * Approves or reactivates an account (status ACTIVE). The customer record
     * becomes ACTIVE as well, so it can receive releases.
     */
    public boolean activateAccount(int accountId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean changed = updateStatus(conn, accountId, Status.ACTIVE);
                if (changed) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE CUSTOMERS SET status = ? WHERE customer_id = "
                            + "(SELECT a.customer_id FROM CUSTOMER_ACCOUNTS a WHERE a.account_id = ?)")) {
                        ps.setString(1, Status.ACTIVE);
                        ps.setInt(2, accountId);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return changed;
            } catch (SQLException | RuntimeException e) {
                DaoUtil.rollback(conn, e);
                throw e;
            }
        }
    }

    /** Blocks the account from logging in. The customer record itself is not changed. */
    public boolean deactivateAccount(int accountId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return updateStatus(conn, accountId, Status.INACTIVE);
        }
    }

    private boolean updateStatus(Connection conn, int accountId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE CUSTOMER_ACCOUNTS SET status = ? WHERE account_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Remembers the time of the last successful login. */
    public void recordLogin(int accountId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE CUSTOMER_ACCOUNTS SET last_login = CURRENT_TIMESTAMP WHERE account_id = ?")) {
            ps.setInt(1, accountId);
            ps.executeUpdate();
        }
    }

    public boolean changePassword(int accountId, char[] newPassword) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE CUSTOMER_ACCOUNTS SET password_hash = ? WHERE account_id = ?")) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Searches by username, customer name, contact person or e-mail; status null = all. */
    public List<CustomerAccount> searchAccounts(String keyword, String status) throws SQLException {
        List<Object> params = new ArrayList<>();
        String pattern = DaoUtil.likePattern(keyword);
        StringBuilder sql = new StringBuilder(SELECT_ACCOUNTS);
        sql.append("WHERE (UPPER(a.username)").append(DaoUtil.LIKE)
           .append(" OR UPPER(c.customer_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(c.contact_person, ''))").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(c.email, ''))").append(DaoUtil.LIKE).append(")");
        for (int i = 0; i < 4; i++) {
            params.add(pattern);
        }
        if (status != null) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }
        // pending registrations first, then the newest accounts
        sql.append(" ORDER BY CASE WHEN a.status = 'PENDING' THEN 0 ELSE 1 END, a.created_at DESC");

        List<CustomerAccount> list = new ArrayList<>();
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

    public boolean isUsernameTaken(String username) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return isUsernameTaken(conn, username);
        }
    }

    private boolean isUsernameTaken(Connection conn, String username) throws SQLException {
        return DaoUtil.queryInt(conn, "SELECT COUNT(*) FROM CUSTOMER_ACCOUNTS WHERE UPPER(username) = ?",
                username.trim().toUpperCase()) > 0;
    }

    /** Registrations waiting for approval (shown on the staff dashboard). */
    public int countPending() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn, "SELECT COUNT(*) FROM CUSTOMER_ACCOUNTS WHERE status = ?", Status.PENDING);
        }
    }

    private CustomerAccount mapRow(ResultSet rs) throws SQLException {
        CustomerAccount account = new CustomerAccount(rs.getInt("account_id"), rs.getInt("customer_id"),
                rs.getString("username"), rs.getString("status"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setCreatedAt(rs.getTimestamp("created_at"));
        account.setLastLogin(rs.getTimestamp("last_login"));
        account.setCustomerName(rs.getString("customer_name"));
        account.setCustomerStatus(rs.getString("customer_status"));
        account.setContactPerson(rs.getString("contact_person"));
        account.setContactNo(rs.getString("contact_no"));
        account.setEmail(rs.getString("email"));
        return account;
    }
}
