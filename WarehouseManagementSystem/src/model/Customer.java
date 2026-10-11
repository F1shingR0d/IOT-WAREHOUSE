package model;

/**
 * A customer / consignee that receives released stock (table CUSTOMERS).
 * One customer can have many release records.
 */
public class Customer {

    private int customerId;
    private String customerName;
    private String contactPerson;
    private String contactNo;
    private String email;
    private String address;
    private String status;

    public Customer() {
        this.status = Status.ACTIVE;
    }

    public Customer(int customerId, String customerName, String contactPerson, String contactNo) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.contactPerson = contactPerson;
        this.contactNo = contactNo;
        this.status = Status.ACTIVE;
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
        return customerName;
    }
}
