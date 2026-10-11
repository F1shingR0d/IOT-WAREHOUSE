package util;

/**
 * Thrown when a warehouse does not have enough stock of a product for a
 * release, a transfer or a void.
 */
public class InsufficientStockException extends BusinessRuleException {

    private static final long serialVersionUID = 1L;

    private final String productName;
    private final String warehouseName;
    private final int available;
    private final int requested;

    public InsufficientStockException(String productName, String warehouseName, int available, int requested) {
        super("Not enough stock of " + productName + " in " + warehouseName
                + ".\nAvailable: " + Formats.number(available) + "   Requested: " + Formats.number(requested));
        this.productName = productName;
        this.warehouseName = warehouseName;
        this.available = available;
        this.requested = requested;
    }

    public String getProductName() {
        return productName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
