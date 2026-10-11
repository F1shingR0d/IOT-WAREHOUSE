package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A receiving document (table RECEIVINGS): stock that arrives at a warehouse
 * from a supplier. Saving it ADDS the item quantities to the warehouse stock.
 */
public class Receiving extends StockDocument {

    private int receivingId;
    private int warehouseId;
    private String warehouseName;
    private int supplierId;
    private String supplierName;
    private BigDecimal totalAmount = BigDecimal.ZERO;   // computed by list queries
    private final List<ReceivingItem> items = new ArrayList<>();

    public Receiving() {
    }

    public Receiving(int warehouseId, int supplierId, int userId) {
        this.warehouseId = warehouseId;
        this.supplierId = supplierId;
        setUserId(userId);
    }

    @Override
    public int getId() {
        return receivingId;
    }

    @Override
    public String getDocumentType() {
        return "Receiving";
    }

    @Override
    public List<ReceivingItem> getItems() {
        return items;
    }

    public void addItem(ReceivingItem item) {
        items.add(item);
    }

    /** Total cost of the item lines currently loaded in this object. */
    public BigDecimal getItemsAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (ReceivingItem item : items) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    public int getReceivingId() {
        return receivingId;
    }

    public void setReceivingId(int receivingId) {
        this.receivingId = receivingId;
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

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
