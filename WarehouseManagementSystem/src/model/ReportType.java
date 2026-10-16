package model;

/**
 * The reports available in the Reports module.
 */
public enum ReportType {

    INVENTORY_STATUS("Inventory Status",
            "Stock on hand of every product in each warehouse, with stock value and status.", false, true, false),
    LOW_STOCK("Low Stock / Reorder List",
            "Active products whose total stock (all warehouses) is at or below the reorder level.", false, false, false),
    STOCK_MOVEMENT("Stock Movement History",
            "Every stock movement in the period: receiving (IN), releasing (OUT) and transfers (IN/OUT).", true, true, false),
    RECEIVING("Receiving Report",
            "Receiving documents in the period, with supplier, units and total cost.", true, true, false),
    RELEASING("Releasing Report",
            "Release documents in the period, with customer and units released.", true, true, false),
    TRANSFERS("Stock Transfer Report",
            "Stock transfers in the period. The warehouse filter matches the source or the destination.", true, true, false),
    CUSTOMER_REQUESTS("Customer Requests Report",
            "Stock requests sent through the Customer Portal in the period, with status and the release that delivered them.",
            true, true, false),
    WAREHOUSE_SUMMARY("Warehouse Summary",
            "Products stored, units on hand, capacity used and stock value of every warehouse.", false, false, false),
    MONTHLY_SUMMARY("Monthly In/Out Summary",
            "Documents and units received, released and transferred per month (posted documents only).", true, false, false),
    PRODUCT_LIST("Product List", "All products with category, cost, reorder level and total stock.", false, false, false),
    SUPPLIER_LIST("Supplier List", "All suppliers and their contact details.", false, false, false),
    CUSTOMER_LIST("Customer List", "All customers and their contact details.", false, false, false),
    USER_LIST("User List", "All user accounts and their roles (administrators only).", false, false, true);

    private final String title;
    private final String description;
    private final boolean usesDateRange;
    private final boolean usesWarehouse;
    private final boolean adminOnly;

    ReportType(String title, String description, boolean usesDateRange, boolean usesWarehouse, boolean adminOnly) {
        this.title = title;
        this.description = description;
        this.usesDateRange = usesDateRange;
        this.usesWarehouse = usesWarehouse;
        this.adminOnly = adminOnly;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean usesDateRange() {
        return usesDateRange;
    }

    public boolean usesWarehouse() {
        return usesWarehouse;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    @Override
    public String toString() {
        return title;
    }
}
