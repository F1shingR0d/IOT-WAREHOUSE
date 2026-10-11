package model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/**
 * Header data shared by the three stock documents (Receiving, Release and
 * StockTransfer). Every document has a reference number, a date, the user
 * who recorded it, a status and one or more item lines.
 */
public abstract class StockDocument {

    private String referenceNo;
    private LocalDate documentDate;
    private int userId;
    private String recordedBy;      // full name of the user, from the JOIN with USERS
    private String remarks;
    private String status;
    private Timestamp createdAt;
    private int itemCount;          // number of lines, computed by list queries
    private int totalQuantity;      // total units, computed by list queries

    protected StockDocument() {
        this.status = Status.POSTED;
    }

    /** Primary key of the document. */
    public abstract int getId();

    /** "Receiving", "Release" or "Stock Transfer". */
    public abstract String getDocumentType();

    /** The item lines of this document. */
    public abstract List<? extends DocumentItem> getItems();

    /** Total units of the item lines currently loaded in this object. */
    public int getItemsQuantity() {
        int total = 0;
        for (DocumentItem item : getItems()) {
            total += item.getQuantity();
        }
        return total;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public LocalDate getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(LocalDate documentDate) {
        this.documentDate = documentDate;
    }

    public int getUserId() {
        return userId;
    }

    public final void setUserId(int userId) {
        this.userId = userId;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public boolean isVoided() {
        return Status.VOIDED.equals(status);
    }
}
