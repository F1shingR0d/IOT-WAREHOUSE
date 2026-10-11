package util;

import java.time.LocalDateTime;
import model.Role;
import model.User;

/**
 * Remembers who is logged in and answers "is this user allowed to ...?".
 * All role-based access rules of the system are defined here:
 *
 *   Module / action                          Admin  Manager  Staff
 *   Add / edit master data                     yes    yes      no (view only)
 *   Delete records                             yes    no       no
 *   Create receiving, releasing, transfers     yes    yes      yes
 *   Void transactions                          yes    yes      no
 *   Reports                                    yes    yes      no
 *   User management                            yes    no       no
 */
public final class Session {

    private static User currentUser;
    private static LocalDateTime loginTime;

    private Session() {
    }

    public static void start(User user) {
        currentUser = user;
        loginTime = LocalDateTime.now();
    }

    /** Ends the session (logout). */
    public static void end() {
        currentUser = null;
        loginTime = null;
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
        return currentUser != null;
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
}
