package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Stock of one product in one warehouse (table WAREHOUSE_STOCK).
 * This associative table resolves the many-to-many relationship
 * between WAREHOUSES and PRODUCTS.
 */
public class WarehouseStock {

    public static final String STATUS_OK = "OK";
    public static final String STATUS_LOW = "LOW";
    public static final String STATUS_OUT = "OUT";

    private int warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private int productId;
    private String sku;
    private String productName;
    private String categoryName;
    private String unit;
    private int quantity;
    private int reorderLevel;
    private BigDecimal unitCost;
    private Timestamp lastUpdated;

    public WarehouseStock() {
        this.unitCost = BigDecimal.ZERO;
    }

    public WarehouseStock(int warehouseId, int productId, int quantity) {
        this();
        this.warehouseId = warehouseId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(int warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /** Quantity x unit cost. */
    public BigDecimal getStockValue() {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    /** OUT when empty, LOW when at or below the reorder level, otherwise OK. */
    public String getStockStatus() {
        if (quantity <= 0) {
            return STATUS_OUT;
        }
        return quantity <= reorderLevel ? STATUS_LOW : STATUS_OK;
    }
}
