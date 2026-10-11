package model;

/**
 * The three user roles of the system. The role decides which modules and
 * buttons a user can access (see util.Session for the permission rules).
 */
public enum Role {

    ADMIN("Administrator"),
    MANAGER("Warehouse Manager"),
    STAFF("Warehouse Staff");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Converts the value stored in the database (e.g. "ADMIN") back to a Role. */
    public static Role fromCode(String code) {
        return Role.valueOf(code.trim().toUpperCase());
    }

    @Override
    public String toString() {
        return displayName;
    }
}
