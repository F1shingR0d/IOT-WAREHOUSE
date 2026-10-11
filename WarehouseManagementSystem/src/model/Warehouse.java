package model;

/**
 * A warehouse / storage location (table WAREHOUSES).
 * A warehouse may have one user in charge (optional one-to-one with USERS).
 */
public class Warehouse {

    private int warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String location;
    private int capacity;           // maximum number of units the warehouse can hold
    private String contactNo;
    private Integer managerId;      // null when nobody is assigned
    private String managerName;     // from the JOIN with USERS
    private String status;
    private int stockOnHand;        // SUM of WAREHOUSE_STOCK.quantity, computed by the query

    public Warehouse() {
        this.status = Status.ACTIVE;
    }

    public Warehouse(int warehouseId, String warehouseCode, String warehouseName, String location, int capacity) {
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.location = location;
        this.capacity = capacity;
        this.status = Status.ACTIVE;
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(int warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getStockOnHand() {
        return stockOnHand;
    }

    public void setStockOnHand(int stockOnHand) {
        this.stockOnHand = stockOnHand;
    }

    /** Percentage of the capacity that is currently used (0 - 100+). */
    public double getUtilization() {
        return capacity <= 0 ? 0 : (stockOnHand * 100.0) / capacity;
    }

    public int getFreeCapacity() {
        return Math.max(0, capacity - stockOnHand);
    }

    @Override
    public String toString() {
        return warehouseCode + " - " + warehouseName;
    }
}
