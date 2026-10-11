package ui;

import dao.UserDAO;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import model.Role;
import model.Status;
import model.User;
import ui.components.FormGrid;
import util.ErrorHandler;
import util.Formats;
import util.LengthFilter;
import util.Session;
import util.TableUtil;
import util.Validator;

/**
 * User account management (administrators only). Passwords are typed in
 * JPasswordFields and stored as hashes; they are never shown in the table.
 */
public class UserPanel extends CrudPanel<User> {

    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();
    private final JTextField txtUsername = new JTextField();
    private final JTextField txtFullName = new JTextField();
    private final JComboBox<Role> cboRole = new JComboBox<>(Role.values());
    private final JRadioButton rbActive = new JRadioButton("Active");
    private final JRadioButton rbInactive = new JRadioButton("Inactive");
    private final JPasswordField txtPassword = new JPasswordField();
    private final JPasswordField txtConfirm = new JPasswordField();
    private final JLabel lblPasswordHint = UITheme.mutedLabel(" ");
    private final JComboBox<Object> cboFilterRole = new JComboBox<>();

    public UserPanel() {
        super("User Management", "Create accounts and assign roles. Only administrators can open this screen.",
                "User", new String[]{"ID", "Username", "Full Name", "Role", "Status", "Created"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, String.class});
        LengthFilter.username(txtUsername);
        LengthFilter.apply(txtFullName, 100);
        LengthFilter.apply(txtPassword, Validator.MAX_PASSWORD_LENGTH);
        LengthFilter.apply(txtConfirm, Validator.MAX_PASSWORD_LENGTH);
        cboFilterRole.addItem("All roles");
        for (Role role : Role.values()) {
            cboFilterRole.addItem(role);
        }
        buildUI();
        TableUtil.setColumnWidths(table, 45, 120, 200, 150, 80, 150);
    }

    @Override
    protected boolean canAdd() {
        return Session.canManageUsers();
    }

    @Override
    protected boolean canEdit() {
        return Session.canManageUsers();
    }

    @Override
    protected boolean canDelete() {
        return Session.canManageUsers();
    }

    @Override
    protected void buildForm(FormGrid form) {
        form.addField("Username", txtUsername, true);
        form.addField("Full Name", txtFullName, true);
        form.addField("Role", cboRole, true);
        form.addField("Status", statusPanel(rbActive, rbInactive), true);
        form.addField("Password", txtPassword);
        form.addField("Confirm Password", txtConfirm);
        form.addWide(lblPasswordHint);
    }

    @Override
    protected void addFilters(JPanel filterBar) {
        filterBar.add(cboFilterRole);
        cboFilterRole.addActionListener(e -> loadTable());
    }

    @Override
    protected void resetFilters() {
        cboFilterRole.setSelectedIndex(0);
    }

    @Override
    protected List<User> fetchRecords(String keyword) throws SQLException {
        Object role = cboFilterRole.getSelectedItem();
        return userDAO.searchUsers(keyword, role instanceof Role ? (Role) role : null);
    }

    @Override
    protected Object[] toRow(User u) {
        return new Object[]{u.getUserId(), u.getUsername(), u.getFullName(), u.getRole().getDisplayName(),
            u.getStatus(), Formats.dateTime(u.getCreatedAt())};
    }

    @Override
    protected int getId(User u) {
        return u.getUserId();
    }

    @Override
    protected void fillForm(User u) {
        txtUsername.setText(u.getUsername());
        txtFullName.setText(u.getFullName());
        cboRole.setSelectedItem(u.getRole());
        rbActive.setSelected(u.isActive());
        rbInactive.setSelected(!u.isActive());
        txtPassword.setText("");
        txtConfirm.setText("");
        lblPasswordHint.setText(wrap("Leave the password fields empty to keep the current password."));
    }

    @Override
    protected void clearFields() {
        txtUsername.setText("");
        txtFullName.setText("");
        cboRole.setSelectedItem(Role.STAFF);
        rbActive.setSelected(true);
        txtPassword.setText("");
        txtConfirm.setText("");
        lblPasswordHint.setText(wrap("Password: at least " + Validator.MIN_PASSWORD_LENGTH + " characters."));
    }

    @Override
    protected void setFieldsEditable(boolean editable) {
        txtUsername.setEditable(editable);
        txtFullName.setEditable(editable);
        cboRole.setEnabled(editable);
        rbActive.setEnabled(editable);
        rbInactive.setEnabled(editable);
        txtPassword.setEditable(editable);
        txtConfirm.setEditable(editable);
    }

    @Override
    protected void focusFirstField() {
        txtUsername.requestFocusInWindow();
    }

    /**
     * Validates the form. For a new user the password is required; when
     * editing, an empty password keeps the old one.
     */
    private User readForm(int userId, boolean passwordRequired) throws SQLException {
        if (Validator.isEmpty(txtUsername.getText())) {
            Dialogs.invalid(this, "Username is required.", txtUsername);
            return null;
        }
        if (!Validator.isValidUsername(txtUsername.getText())) {
            Dialogs.invalid(this, "Username must be 4-30 characters: letters, digits, dots or underscores.", txtUsername);
            return null;
        }
        if (userDAO.isUsernameTaken(text(txtUsername), userId)) {
            Dialogs.invalid(this, "The username \"" + text(txtUsername) + "\" is already taken.", txtUsername);
            return null;
        }
        if (Validator.isEmpty(txtFullName.getText())) {
            Dialogs.invalid(this, "Full name is required.", txtFullName);
            return null;
        }
        char[] password = txtPassword.getPassword();
        char[] confirm = txtConfirm.getPassword();
        try {
            if (passwordRequired || password.length > 0 || confirm.length > 0) {
                if (!Validator.isValidPassword(password)) {
                    Dialogs.invalid(this, "Password must be " + Validator.MIN_PASSWORD_LENGTH + " to "
                            + Validator.MAX_PASSWORD_LENGTH + " characters long.", txtPassword);
                    return null;
                }
                if (!Arrays.equals(password, confirm)) {
                    Dialogs.invalid(this, "The two passwords do not match.", txtConfirm);
                    return null;
                }
            }
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
        User user = new User(userId, text(txtUsername), text(txtFullName), (Role) cboRole.getSelectedItem(),
                statusOf(rbActive));
        return user;
    }

    @Override
    protected void saveRecord() {
        try {
            User user = readForm(0, true);
            if (user != null) {
                char[] password = txtPassword.getPassword();
                int id = userDAO.addUser(user, password);
                Arrays.fill(password, '\0');
                afterChange("User account \"" + user.getUsername() + "\" was created (ID " + id + ").");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "save the user");
        }
    }

    @Override
    protected void updateRecord() {
        try {
            User user = readForm(selected.getUserId(), false);
            if (user == null || !checkAdminRules(user)) {
                return;
            }
            char[] password = txtPassword.getPassword();
            boolean updated = userDAO.updateUser(user, password.length > 0 ? password : null);
            Arrays.fill(password, '\0');
            if (updated) {
                if (user.getUserId() == Session.getUserId()) {
                    Session.getCurrentUser().setFullName(user.getFullName());
                    Session.getCurrentUser().setUsername(user.getUsername());
                }
                afterChange("User account \"" + user.getUsername() + "\" was updated.");
            } else {
                Dialogs.warning(this, "This user no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "update the user");
        }
    }

    /** Protects the logged-in administrator and the last active administrator account. */
    private boolean checkAdminRules(User changed) throws SQLException {
        boolean removesAdminAccess = changed.getRole() != Role.ADMIN || !Status.ACTIVE.equals(changed.getStatus());
        if (changed.getUserId() == Session.getUserId() && removesAdminAccess) {
            Dialogs.warning(this, "You cannot remove your own administrator role or deactivate your own account.");
            return false;
        }
        if (selected.getRole() == Role.ADMIN && selected.isActive() && removesAdminAccess
                && userDAO.countActiveAdmins() <= 1) {
            Dialogs.warning(this, "This is the last active administrator. The system needs at least one.");
            return false;
        }
        return true;
    }

    @Override
    protected void deleteRecord() {
        try {
            if (selected.getUserId() == Session.getUserId()) {
                Dialogs.warning(this, "You cannot delete the account you are logged in with.");
                return;
            }
            if (selected.getRole() == Role.ADMIN && selected.isActive() && userDAO.countActiveAdmins() <= 1) {
                Dialogs.warning(this, "This is the last active administrator and cannot be deleted.");
                return;
            }
            int documents = userDAO.countRecordedTransactions(selected.getUserId());
            if (documents > 0) {
                Dialogs.warning(this, selected.getUsername() + " recorded " + documents + " transaction(s), so the "
                        + "account is kept for the audit trail.\n\nTip: set its status to INACTIVE instead.");
                return;
            }
            if (!Dialogs.confirmDelete(this, "User: " + selected.getUsername() + " (" + selected.getFullName() + ")")) {
                return;
            }
            if (userDAO.deleteUser(selected.getUserId())) {
                afterChange("The user account was deleted.");
            } else {
                Dialogs.warning(this, "This user no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "delete the user");
        }
    }

    /** Lets a long hint wrap onto a second line instead of being cut off. */
    private static String wrap(String text) {
        return "<html><div style='width:240px'>" + text + "</div></html>";
    }
}
