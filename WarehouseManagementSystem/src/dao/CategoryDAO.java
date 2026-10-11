package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Category;

/**
 * Database operations for the CATEGORIES table.
 */
public class CategoryDAO {

    /** LEFT JOIN + COUNT shows how many products each category has. */
    private static final String SELECT_WITH_COUNT =
            "SELECT c.category_id, c.category_name, c.description, COUNT(p.product_id) AS product_count "
            + "FROM CATEGORIES c LEFT JOIN PRODUCTS p ON p.category_id = c.category_id ";

    private static final String GROUP_AND_ORDER =
            " GROUP BY c.category_id, c.category_name, c.description ORDER BY c.category_name";

    public int addCategory(Category category) throws SQLException {
        String sql = "INSERT INTO CATEGORIES (category_name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = DaoUtil.prepareInsert(conn, sql)) {
            ps.setString(1, category.getCategoryName().trim());
            DaoUtil.setText(ps, 2, category.getDescription());
            int id = DaoUtil.insertAndGetId(ps);
            category.setCategoryId(id);
            return id;
        }
    }

    public boolean updateCategory(Category category) throws SQLException {
        String sql = "UPDATE CATEGORIES SET category_name = ?, description = ? WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getCategoryName().trim());
            DaoUtil.setText(ps, 2, category.getDescription());
            ps.setInt(3, category.getCategoryId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteCategory(int categoryId) throws SQLException {
        String sql = "DELETE FROM CATEGORIES WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Category> getCategories() throws SQLException {
        return searchCategories("");
    }

    /** Searches by ID, name or description. An empty keyword returns all categories. */
    public List<Category> searchCategories(String keyword) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_WITH_COUNT);
        sql.append("WHERE (UPPER(c.category_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(COALESCE(c.description, ''))").append(DaoUtil.LIKE);
        params.add(DaoUtil.likePattern(keyword));
        params.add(DaoUtil.likePattern(keyword));
        Integer id = DaoUtil.parseId(keyword);
        if (id != null) {
            sql.append(" OR c.category_id = ?");
            params.add(id);
        }
        sql.append(")").append(GROUP_AND_ORDER);

        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            DaoUtil.bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Category category = new Category(rs.getInt("category_id"),
                            rs.getString("category_name"), rs.getString("description"));
                    category.setProductCount(rs.getInt("product_count"));
                    list.add(category);
                }
            }
        }
        return list;
    }

    /** true if another category already uses this name (not case-sensitive). */
    public boolean isNameTaken(String name, int excludeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM CATEGORIES WHERE UPPER(category_name) = ? AND category_id <> ?",
                    name.trim().toUpperCase(), excludeId) > 0;
        }
    }
}
