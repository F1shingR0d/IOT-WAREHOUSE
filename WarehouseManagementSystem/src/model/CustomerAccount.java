package model;

import java.sql.Timestamp;

/**
 * A customer login account of the Customer Portal (table CUSTOMER_ACCOUNTS).
 * Each account belongs to exactly one customer (one-to-one with CUSTOMERS).
 */
public class CustomerAccount {

    private int accountId;
    private int customerId;
    private String username;
    private String passwordHash;
    private String status;
    private Timestamp createdAt;
    private Timestamp lastLogin;

    // from the JOIN with CUSTOMERS
    private String customerName;
    private String customerStatus;
    private String contactPerson;
    private String contactNo;
    private String email;

    public CustomerAccount() {
        this.status = Status.PENDING;
    }

    public CustomerAccount(int accountId, int customerId, String username, String status) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.username = username;
        this.status = status;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerStatus() {
        return customerStatus;
    }

    public void setCustomerStatus(String customerStatus) {
        this.customerStatus = customerStatus;
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

    public boolean isActive() {
        return Status.ACTIVE.equals(status);
    }

    public boolean isPending() {
        return Status.PENDING.equals(status);
    }

    @Override
    public String toString() {
        return customerName == null ? username : customerName;
    }
}
