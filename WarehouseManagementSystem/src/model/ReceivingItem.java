package model;

import java.math.BigDecimal;

/**
 * A product line of a receiving document (table RECEIVING_ITEMS).
 */
public class ReceivingItem extends DocumentItem {

    private int receivingId;
    private BigDecimal unitCost;

    public ReceivingItem() {
        this.unitCost = BigDecimal.ZERO;
    }

    public ReceivingItem(int productId, String sku, String productName, String unit, int quantity, BigDecimal unitCost) {
        super(productId, sku, productName, unit, quantity);
        this.unitCost = unitCost;
    }

    public int getReceivingId() {
        return receivingId;
    }

    public void setReceivingId(int receivingId) {
        this.receivingId = receivingId;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    /** Quantity x unit cost (calculated, not stored in the database). */
    public BigDecimal getSubtotal() {
        return unitCost.multiply(BigDecimal.valueOf(getQuantity()));
    }
}
