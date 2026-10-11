package model;

import java.util.ArrayList;
import java.util.List;

/**
 * A stock transfer document (table STOCK_TRANSFERS): stock moved from one
 * warehouse to another. Saving it deducts from the source warehouse and adds
 * to the destination warehouse.
 */
public class StockTransfer extends StockDocument {

    private int transferId;
    private int fromWarehouseId;
    private String fromWarehouseName;
    private int toWarehouseId;
    private String toWarehouseName;
    private final List<TransferItem> items = new ArrayList<>();

    public StockTransfer() {
    }

    public StockTransfer(int fromWarehouseId, int toWarehouseId, int userId) {
        this.fromWarehouseId = fromWarehouseId;
        this.toWarehouseId = toWarehouseId;
        setUserId(userId);
    }

    @Override
    public int getId() {
        return transferId;
    }

    @Override
    public String getDocumentType() {
        return "Stock Transfer";
    }

    @Override
    public List<TransferItem> getItems() {
        return items;
    }

    public void addItem(TransferItem item) {
        items.add(item);
    }

    public int getTransferId() {
        return transferId;
    }

    public void setTransferId(int transferId) {
        this.transferId = transferId;
    }

    public int getFromWarehouseId() {
        return fromWarehouseId;
    }

    public void setFromWarehouseId(int fromWarehouseId) {
        this.fromWarehouseId = fromWarehouseId;
    }

    public String getFromWarehouseName() {
        return fromWarehouseName;
    }

    public void setFromWarehouseName(String fromWarehouseName) {
        this.fromWarehouseName = fromWarehouseName;
    }

    public int getToWarehouseId() {
        return toWarehouseId;
    }

    public void setToWarehouseId(int toWarehouseId) {
        this.toWarehouseId = toWarehouseId;
    }

    public String getToWarehouseName() {
        return toWarehouseName;
    }

    public void setToWarehouseName(String toWarehouseName) {
        this.toWarehouseName = toWarehouseName;
    }
}
