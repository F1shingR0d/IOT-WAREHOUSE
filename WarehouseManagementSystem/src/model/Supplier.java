package model;

/**
 * A supplier that delivers stock to the warehouses (table SUPPLIERS).
 * One supplier can have many receiving records.
 */
public class Supplier {

    private int supplierId;
    private String supplierName;
    private String contactPerson;
    private String contactNo;
    private String email;
    private String address;
    private String status;

    public Supplier() {
        this.status = Status.ACTIVE;
    }

    public Supplier(int supplierId, String supplierName, String contactPerson, String contactNo) {
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.contactPerson = contactPerson;
        this.contactNo = contactNo;
        this.status = Status.ACTIVE;
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

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return supplierName;
    }
}
