package model;

import java.util.ArrayList;
import java.util.List;

/**
 * A release document (table RELEASES): stock that leaves a warehouse for a
 * customer. Saving it DEDUCTS the item quantities from the warehouse stock.
 */
public class Release extends StockDocument {

    private int releaseId;
    private int warehouseId;
    private String warehouseName;
    private int customerId;
    private String customerName;
    private final List<ReleaseItem> items = new ArrayList<>();

    public Release() {
    }

    public Release(int warehouseId, int customerId, int userId) {
        this.warehouseId = warehouseId;
        this.customerId = customerId;
        setUserId(userId);
    }

    @Override
    public int getId() {
        return releaseId;
    }

    @Override
    public String getDocumentType() {
        return "Release";
    }

    @Override
    public List<ReleaseItem> getItems() {
        return items;
    }

    public void addItem(ReleaseItem item) {
        items.add(item);
    }

    public int getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(int releaseId) {
        this.releaseId = releaseId;
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(int warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
