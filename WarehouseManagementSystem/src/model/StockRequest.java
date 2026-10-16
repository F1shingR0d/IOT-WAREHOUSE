package model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A stock request made by a customer in the Customer Portal (table
 * STOCK_REQUESTS). A request never changes the stock by itself: when the
 * warehouse releases it, a normal Release document is created and linked
 * through releaseId.
 *
 * Status flow: PENDING -> APPROVED -> FULFILLED, or REJECTED / CANCELLED.
 */
public class StockRequest {

    private int requestId;
    private String referenceNo;
    private LocalDate requestDate;
    private LocalDate neededBy;
    private int customerId;
    private String customerName;     // from the JOIN with CUSTOMERS
    private int warehouseId;
    private String warehouseName;    // from the JOIN with WAREHOUSES
    private String remarks;
    private String status;
    private int reviewedBy;          // 0 = not reviewed yet
    private String reviewedByName;   // from the JOIN with USERS
    private Timestamp reviewedAt;
    private String reviewNote;
    private int releaseId;           // 0 = not released yet
    private String releaseReference; // from the JOIN with RELEASES
    private Timestamp createdAt;
    private int itemCount;           // number of lines, computed by list queries
    private int totalQuantity;       // total units, computed by list queries
    private final List<StockRequestItem> items = new ArrayList<>();

    public StockRequest() {
        this.status = Status.PENDING;
    }

    public StockRequest(int customerId, int warehouseId) {
        this();
        this.customerId = customerId;
        this.warehouseId = warehouseId;
    }

    public List<StockRequestItem> getItems() {
        return items;
    }

    public void addItem(StockRequestItem item) {
        items.add(item);
    }

    /** Total units of the item lines currently loaded in this object. */
    public int getItemsQuantity() {
        int total = 0;
        for (StockRequestItem item : items) {
            total += item.getQuantity();
        }
        return total;
    }

    /** true while the warehouse can still approve, reject or release the request. */
    public boolean isOpen() {
        return Status.PENDING.equals(status) || Status.APPROVED.equals(status);
    }

    public boolean isPending() {
        return Status.PENDING.equals(status);
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDate getNeededBy() {
        return neededBy;
    }

    public void setNeededBy(LocalDate neededBy) {
        this.neededBy = neededBy;
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

    public int getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(int reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public void setReviewedByName(String reviewedByName) {
        this.reviewedByName = reviewedByName;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public int getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(int releaseId) {
        this.releaseId = releaseId;
    }

    public String getReleaseReference() {
        return releaseReference;
    }

    public void setReleaseReference(String releaseReference) {
        this.releaseReference = releaseReference;
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
}
