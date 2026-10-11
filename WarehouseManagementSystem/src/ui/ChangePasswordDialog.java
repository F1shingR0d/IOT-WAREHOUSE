package ui;

import dao.UserDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import model.User;
import ui.components.FlatButton;
import ui.components.ModuleIcon;
import ui.components.FormGrid;
import util.ErrorHandler;
import util.LengthFilter;
import util.Session;
import util.Validator;

/**
 * Lets the logged-in user change his or her own password.
 */
public class ChangePasswordDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();
    private final JPasswordField txtCurrent = new JPasswordField(20);
    private final JPasswordField txtNew = new JPasswordField(20);
    private final JPasswordField txtConfirm = new JPasswordField(20);

    public ChangePasswordDialog(Window owner) {
        super(owner, "Change Password", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        LengthFilter.apply(txtCurrent, Validator.MAX_PASSWORD_LENGTH);
        LengthFilter.apply(txtNew, Validator.MAX_PASSWORD_LENGTH);
        LengthFilter.apply(txtConfirm, Validator.MAX_PASSWORD_LENGTH);

        FormGrid form = new FormGrid();
        form.addField("Current Password", txtCurrent, true);
        form.addField("New Password", txtNew, true);
        form.addField("Confirm New Password", txtConfirm, true);
        form.addWide(UITheme.mutedLabel("The new password must have at least " + Validator.MIN_PASSWORD_LENGTH
                + " characters."));

        FlatButton btnChange = new FlatButton("Change Password", UITheme.PRIMARY, ModuleIcon.Type.KEY);
        FlatButton btnCancel = FlatButton.outline("Cancel", ModuleIcon.Type.CLOSE);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnChange);
        buttons.add(btnCancel);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.CARD);
        root.setBorder(new EmptyBorder(20, 24, 18, 24));
        root.add(UITheme.headingLabel("Change password for " + Session.getCurrentUser().getUsername()),
                BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        btnChange.addActionListener(e -> changePassword());
        btnCancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(btnChange);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void changePassword() {
        char[] current = txtCurrent.getPassword();
        char[] newPassword = txtNew.getPassword();
        char[] confirm = txtConfirm.getPassword();
        try {
            if (current.length == 0) {
                Dialogs.invalid(this, "Please enter your current password.", txtCurrent);
                return;
            }
            if (!Validator.isValidPassword(newPassword)) {
                Dialogs.invalid(this, "The new password must be " + Validator.MIN_PASSWORD_LENGTH + " to "
                        + Validator.MAX_PASSWORD_LENGTH + " characters long.", txtNew);
                return;
            }
            if (!Arrays.equals(newPassword, confirm)) {
                Dialogs.invalid(this, "The new passwords do not match.", txtConfirm);
                return;
            }
            if (Arrays.equals(current, newPassword)) {
                Dialogs.invalid(this, "The new password must be different from the current one.", txtNew);
                return;
            }
            User user = Session.getCurrentUser();
            if (userDAO.authenticate(user.getUsername(), current) == null) {
                Dialogs.invalid(this, "The current password is not correct.", txtCurrent);
                return;
            }
            userDAO.changePassword(user.getUserId(), newPassword);
            Dialogs.success(this, "Your password was changed.");
            dispose();
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "change the password");
        } finally {
            Arrays.fill(current, '\0');
            Arrays.fill(newPassword, '\0');
            Arrays.fill(confirm, '\0');
        }
    }
}
