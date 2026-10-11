package model;

import java.math.BigDecimal;

/**
 * A product kept in the warehouses (table PRODUCTS).
 * The stock itself is stored per warehouse in WAREHOUSE_STOCK.
 */
public class Product {

    private int productId;
    private String sku;
    private String productName;
    private int categoryId;
    private String categoryName;   // from the JOIN with CATEGORIES
    private String unit;
    private BigDecimal unitCost;
    private int reorderLevel;
    private String description;
    private String status;
    private int totalStock;        // SUM of all warehouse stock, computed by the query

    public Product() {
        this.unitCost = BigDecimal.ZERO;
        this.status = Status.ACTIVE;
    }

    public Product(int productId, String sku, String productName, int categoryId,
                   String unit, BigDecimal unitCost, int reorderLevel) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.categoryId = categoryId;
        this.unit = unit;
        this.unitCost = unitCost;
        this.reorderLevel = reorderLevel;
        this.status = Status.ACTIVE;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(int totalStock) {
        this.totalStock = totalStock;
    }

    public boolean isLowStock() {
        return totalStock <= reorderLevel;
    }

    @Override
    public String toString() {
        return sku + " - " + productName;
    }
}
