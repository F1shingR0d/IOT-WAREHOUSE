package dao;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Product;
import model.Status;

/**
 * Database operations for the PRODUCTS table.
 */
public class ProductDAO {

    /**
     * INNER JOIN shows the category name instead of only the category ID.
     * The sub-query adds up the product's stock in all warehouses.
     */
    private static final String SELECT_PRODUCTS =
            "SELECT p.product_id, p.sku, p.product_name, p.category_id, c.category_name, p.unit, "
            + "p.unit_cost, p.reorder_level, p.description, p.status, "
            + "(SELECT COALESCE(SUM(s.quantity), 0) FROM WAREHOUSE_STOCK s "
            + " WHERE s.product_id = p.product_id) AS total_stock "
            + "FROM PRODUCTS p INNER JOIN CATEGORIES c ON p.category_id = c.category_id ";

    public int addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO PRODUCTS (sku, product_name, category_id, unit, unit_cost, "
                + "reorder_level, description, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = DaoUtil.prepareInsert(conn, sql)) {
            fillStatement(ps, product);
            int id = DaoUtil.insertAndGetId(ps);
            product.setProductId(id);
            return id;
        }
    }

    public boolean updateProduct(Product product) throws SQLException {
        String sql = "UPDATE PRODUCTS SET sku = ?, product_name = ?, category_id = ?, unit = ?, "
                + "unit_cost = ?, reorder_level = ?, description = ?, status = ? WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            fillStatement(ps, product);
            ps.setInt(9, product.getProductId());
            return ps.executeUpdate() > 0;
        }
    }

    private void fillStatement(PreparedStatement ps, Product product) throws SQLException {
        ps.setString(1, product.getSku().trim().toUpperCase());
        ps.setString(2, product.getProductName().trim());
        ps.setInt(3, product.getCategoryId());
        ps.setString(4, product.getUnit());
        ps.setBigDecimal(5, product.getUnitCost());
        ps.setInt(6, product.getReorderLevel());
        DaoUtil.setText(ps, 7, product.getDescription());
        ps.setString(8, product.getStatus());
    }

    public boolean deleteProduct(int productId) throws SQLException {
        String sql = "DELETE FROM PRODUCTS WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Product> getProducts() throws SQLException {
        return searchProducts("", 0, null);
    }

    /** Active products only, for the product pickers of the transaction screens. */
    public List<Product> getActiveProducts() throws SQLException {
        return searchProducts("", 0, Status.ACTIVE);
    }

    /**
     * Searches by ID, SKU, product name or category name.
     *
     * @param keyword    text to look for (empty = everything)
     * @param categoryId 0 = all categories
     * @param status     null = all statuses
     */
    public List<Product> searchProducts(String keyword, int categoryId, String status) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_PRODUCTS);
        String pattern = DaoUtil.likePattern(keyword);
        sql.append("WHERE (UPPER(p.sku)").append(DaoUtil.LIKE)
           .append(" OR UPPER(p.product_name)").append(DaoUtil.LIKE)
           .append(" OR UPPER(c.category_name)").append(DaoUtil.LIKE);
        params.add(pattern);
        params.add(pattern);
        params.add(pattern);
        Integer id = DaoUtil.parseId(keyword);
        if (id != null) {
            sql.append(" OR p.product_id = ?");
            params.add(id);
        }
        sql.append(")");
        if (categoryId > 0) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }
        if (status != null) {
            sql.append(" AND p.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY p.product_name");

        List<Product> list = new ArrayList<>();
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

    public Product getProductById(int productId) throws SQLException {
        String sql = SELECT_PRODUCTS + "WHERE p.product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** true if another product already uses this SKU (not case-sensitive). */
    public boolean isSkuTaken(String sku, int excludeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT COUNT(*) FROM PRODUCTS WHERE UPPER(sku) = ? AND product_id <> ?",
                    sku.trim().toUpperCase(), excludeId) > 0;
        }
    }

    /** Number of transaction lines that use the product (used before deleting). */
    public int countTransactionLines(int productId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return DaoUtil.queryInt(conn,
                    "SELECT (SELECT COUNT(*) FROM RECEIVING_ITEMS WHERE product_id = ?) "
                    + "+ (SELECT COUNT(*) FROM RELEASE_ITEMS WHERE product_id = ?) "
                    + "+ (SELECT COUNT(*) FROM TRANSFER_ITEMS WHERE product_id = ?) FROM SYSIBM.SYSDUMMY1",
                    productId, productId, productId);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product(rs.getInt("product_id"), rs.getString("sku"),
                rs.getString("product_name"), rs.getInt("category_id"), rs.getString("unit"),
                rs.getBigDecimal("unit_cost"), rs.getInt("reorder_level"));
        product.setCategoryName(rs.getString("category_name"));
        product.setDescription(rs.getString("description"));
        product.setStatus(rs.getString("status"));
        product.setTotalStock(rs.getInt("total_stock"));
        return product;
    }
}
