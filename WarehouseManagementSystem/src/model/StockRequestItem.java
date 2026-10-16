package model;

/**
 * A product line of a customer stock request (table STOCK_REQUEST_ITEMS).
 */
public class StockRequestItem extends DocumentItem {

    private int requestId;
    private int available;   // current stock in the request's warehouse (not stored)
    private int reorderLevel; // from the JOIN with PRODUCTS (for the availability status)

    public StockRequestItem() {
    }

    public StockRequestItem(int productId, String sku, String productName, String unit, int quantity) {
        super(productId, sku, productName, unit, quantity);
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    /** IN STOCK / LOW STOCK / OUT OF STOCK, as customers see it (no exact quantities). */
    public String getAvailability() {
        return WarehouseStock.availabilityOf(available, reorderLevel, getQuantity());
    }
}
