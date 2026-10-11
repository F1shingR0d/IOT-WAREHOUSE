package model;

import java.sql.Timestamp;

/**
 * A system user account (table USERS).
 */
public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private Role role;
    private String status;
    private Timestamp createdAt;

    public User() {
        this.status = Status.ACTIVE;
    }

    public User(int userId, String username, String fullName, Role role, String status) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public boolean isActive() {
        return Status.ACTIVE.equals(status);
    }

    @Override
    public String toString() {
        return fullName;
    }
}
