package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Role;
import model.Status;
import model.User;
import util.PasswordUtil;

/**
 * Database operations for the USERS table, including login.
 */
public class UserDAO {

    private static final String SELECT_USERS =
            "SELECT user_id, username, password_hash, full_name, role, status, created_at FROM USERS ";

    /**
     * Checks the username and password against the USERS table.
     *
     * @return the user when the password is correct, otherwise null.
     *         The caller still has to check whether the account is ACTIVE.
     */
    public User authenticate(String username, char[] password) throws SQLException {
        User user = getUserByUsername(username);
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            return null;
        }
        return user;
    }

    public User getUserByUsername(String username) throws SQLException {
        String sql = SELECT_USERS + "WHERE UPPER(username) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public User getUserById(int userId) throws SQLException {
        String sql = SELECT_USERS + "WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Adds a user; the password is hashed before it is stored. */
    public int addUser(User user, char[] password) throws SQLException {
        String sql = "INSERT INTO USERS (username, password_hash, full_name, role, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = DaoUtil.prepareInsert(conn, sql)) {
            ps.setString(1, user.getUsername().trim());
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, user.getFullName().trim());
            ps.setString(4, user.getRole().name());
            ps.setString(5, user.getStatus());
            int id = DaoUtil.insertAndGetId(ps);
            user.setUserId(id);
            return id;
        }
    }

    /**
     * Updates a user. When newPassword is null or empty the current password is kept.
     */
    public boolean updateUser(User user, char[] newPassword) throws SQLException {
        boolean changePassword = newPassword != null && newPassword.length > 0;
        String sql = changePassword
                ? "UPDATE USERS SET username = ?, full_name = ?, role = ?, status = ?, password_hash = ? WHERE user_id = ?"
                : "UPDATE USERS SET username = ?, full_name = ?, role = ?, status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername().trim());
            ps.setString(2, user.getFullName().trim());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getStatus());
            if (changePassword) {
                ps.setString(5, PasswordUtil.hash(newPassword));
                ps.setInt(6, user.getUserId());
            } else {
                ps.setInt(5, user.getUserId());
            }
            return ps.executeUpdate() > 0;
        }
    }

    public boolean changePassword(int userId, char[] newPassword) throws SQLException {
        String sql = "UPDATE USERS SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM USERS WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<User> getUsers() throws SQLException {
        return searchUsers("", null);
    }

    /** Searches by ID, username or full name, optionally only one role. */
    public List<User> searchUsers(String keyword, Role role) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_USERS);
        String pattern = DaoUtil.likePattern(keyword);
        sql.append("WHERE (UPPER(username)").append(DaoUtil.LIKE)
           .append(" OR UPPER(full_name)").append(DaoUtil.LIKE);
        params.add(pattern);
        params.add(pattern);
        Integer id = DaoUtil.parseId(keyword);
        if (id != null) {
            sql.append(" OR user_id = ?");
            params.add(id);
        }
        sql.append(")");
        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role.name());
        }
        sql.append(" ORDER BY full_name");

        List<User> list = new ArrayList<>();
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

    /**
     * Active managers who are not yet in charge of another warehouse
     * (the one-to-one rule of WAREHOUSES.manager_id).
     *
     * @param warehouseId the warehouse being edited (0 for a new warehouse)
     */
    public List<User> getAssignableManagers(int warehouseId) throws SQLException {
        String sql = SELECT_USERS
                + "WHERE role = ? AND status = ? AND user_id NOT IN "
                + "(SELECT w.manager_id FROM WAREHOUSES w WHERE w.manager_id IS NOT NULL AND w.warehouse_id <> ?) "
                + "ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Role.MANAGER.name());
            ps.setString(2, Status.ACTIVE);
            ps.setInt(3, warehouseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public boolean isUsernameTaken(String username, int excludeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM USERS WHERE UPPER(username) = ? AND user_id <> ?",
                    username.trim().toUpperCase(), excludeId) > 0;
        }
    }

    /** Number of ACTIVE administrators, used to protect the last admin account. */
    public int countActiveAdmins() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn, "SELECT COUNT(*) FROM USERS WHERE role = ? AND status = ?",
                    Role.ADMIN.name(), Status.ACTIVE);
        }
    }

    /** Number of documents recorded by the user (used before deleting). */
    public int countRecordedTransactions(int userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT (SELECT COUNT(*) FROM RECEIVINGS WHERE user_id = ?) "
                    + "+ (SELECT COUNT(*) FROM RELEASES WHERE user_id = ?) "
                    + "+ (SELECT COUNT(*) FROM STOCK_TRANSFERS WHERE user_id = ?) FROM SYSIBM.SYSDUMMY1",
                    userId, userId, userId);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("full_name"),
                Role.fromCode(rs.getString("role")), rs.getString("status"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
