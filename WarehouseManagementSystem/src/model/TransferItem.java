package model;

/**
 * A product line of a stock transfer document (table TRANSFER_ITEMS).
 */
public class TransferItem extends DocumentItem {

    private int transferId;

    public TransferItem() {
    }

    public TransferItem(int productId, String sku, String productName, String unit, int quantity) {
        super(productId, sku, productName, unit, quantity);
    }

    public int getTransferId() {
        return transferId;
    }

    public void setTransferId(int transferId) {
        this.transferId = transferId;
    }
}
