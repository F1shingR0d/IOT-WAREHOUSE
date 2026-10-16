package util;

import java.time.LocalDateTime;
import model.CustomerAccount;
import model.Role;
import model.User;

/**
 * Remembers who is logged in and answers "is this user allowed to ...?".
 * All role-based access rules of the system are defined here:
 *
 *   Module / action                          Admin  Manager  Staff  Customer
 *   Add / edit master data                     yes    yes      no     no
 *   Delete records                             yes    no       no     no
 *   Create receiving, releasing, transfers     yes    yes      yes    no
 *   Void transactions                          yes    yes      no     no
 *   Reports                                    yes    yes      no     no
 *   User management                            yes    no       no     no
 *   Customer requests and accounts             yes    yes      no     no
 *   Customer Portal (own requests only)        no     no       no     yes
 *
 * A session belongs either to a staff user (USERS) or to a customer
 * account (CUSTOMER_ACCOUNTS), never both.
 */
public final class Session {

    private static User currentUser;
    private static CustomerAccount currentCustomer;
    private static LocalDateTime loginTime;

    private Session() {
    }

    /** Starts a staff session. */
    public static void start(User user) {
        currentUser = user;
        currentCustomer = null;
        loginTime = LocalDateTime.now();
    }

    /** Starts a Customer Portal session. */
    public static void startCustomer(CustomerAccount account) {
        currentCustomer = account;
        currentUser = null;
        loginTime = LocalDateTime.now();
    }

    /** Ends the session (logout). */
    public static void end() {
        currentUser = null;
        currentCustomer = null;
        loginTime = null;
    }

    public static CustomerAccount getCurrentCustomer() {
        return currentCustomer;
    }

    public static boolean isCustomer() {
        return currentCustomer != null;
    }

    /** The CUSTOMERS id of the logged-in customer (0 for staff). */
    public static int getCustomerId() {
        return currentCustomer == null ? 0 : currentCustomer.getCustomerId();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static int getUserId() {
        return currentUser == null ? 0 : currentUser.getUserId();
    }

    public static LocalDateTime getLoginTime() {
        return loginTime;
    }

    public static boolean isLoggedIn() {
        return currentUser != null || currentCustomer != null;
    }

    public static boolean hasRole(Role... roles) {
        if (currentUser == null) {
            return false;
        }
        for (Role role : roles) {
            if (currentUser.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    public static boolean canEditMasterData() {
        return hasRole(Role.ADMIN, Role.MANAGER);
    }

    public static boolean canDelete() {
        return hasRole(Role.ADMIN);
    }

    public static boolean canCreateTransactions() {
        return hasRole(Role.ADMIN, Role.MANAGER, Role.STAFF);
    }

    public static boolean canVoid() {
        return hasRole(Role.ADMIN, Role.MANAGER);
    }

    public static boolean canViewReports() {
        return hasRole(Role.ADMIN, Role.MANAGER);
    }

    public static boolean canManageUsers() {
        return hasRole(Role.ADMIN);
    }

    /** Review customer stock requests and approve customer accounts. */
    public static boolean canProcessRequests() {
        return hasRole(Role.ADMIN, Role.MANAGER);
    }
}
