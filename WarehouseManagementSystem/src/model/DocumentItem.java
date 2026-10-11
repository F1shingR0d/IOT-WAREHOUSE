package model;

/**
 * One product line of a stock document. ReceivingItem, ReleaseItem and
 * TransferItem inherit these common fields.
 */
public abstract class DocumentItem {

    private int itemId;
    private int productId;
    private String sku;            // from the JOIN with PRODUCTS
    private String productName;    // from the JOIN with PRODUCTS
    private String unit;           // from the JOIN with PRODUCTS
    private int quantity;

    protected DocumentItem() {
    }

    protected DocumentItem(int productId, String sku, String productName, String unit, int quantity) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unit = unit;
        this.quantity = quantity;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
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
}
