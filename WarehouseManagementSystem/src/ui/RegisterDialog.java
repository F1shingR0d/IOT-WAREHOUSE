package ui;

import dao.CustomerAccountDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import model.Customer;
import ui.components.FlatButton;
import ui.components.FormGrid;
import ui.components.ModuleIcon;
import util.BusinessRuleException;
import util.ErrorHandler;
import util.LengthFilter;
import util.Validator;

/**
 * Customer Portal registration: company details plus a username and
 * password. The new account is PENDING until the warehouse approves it.
 */
public class RegisterDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final CustomerAccountDAO accountDAO = new CustomerAccountDAO();
    private final JTextField txtName = new JTextField();
    private final JTextField txtContactPerson = new JTextField();
    private final JTextField txtContactNo = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextArea txtAddress = new JTextArea(2, 20);
    private final JTextField txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JPasswordField txtConfirm = new JPasswordField();
    private final JCheckBox chkShowPasswords = new JCheckBox("Show passwords");
    private final char echoChar;
    private String registeredUsername;

    public RegisterDialog(Window owner) {
        super(owner, "Customer Registration", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        echoChar = txtPassword.getEchoChar();

        LengthFilter.apply(txtName, 100);
        LengthFilter.apply(txtContactPerson, 100);
        LengthFilter.phone(txtContactNo);
        LengthFilter.apply(txtEmail, 100);
        LengthFilter.apply(txtAddress, 255);
        LengthFilter.username(txtUsername);
        LengthFilter.apply(txtPassword, Validator.MAX_PASSWORD_LENGTH);
        LengthFilter.apply(txtConfirm, Validator.MAX_PASSWORD_LENGTH);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        UITheme.setPlaceholder(txtName, "Company or store name");
        UITheme.setPlaceholder(txtContactNo, "e.g. 0917-555-0101");
        UITheme.setPlaceholder(txtEmail, "name@company.com");
        UITheme.setPlaceholder(txtUsername, "4-30 letters, digits, . or _");
        chkShowPasswords.setOpaque(false);
        chkShowPasswords.setFont(UITheme.FONT_SMALL);

        FormGrid form = new FormGrid();
        form.addWide(sectionLabel("Company Details"));
        form.addField("Customer Name", txtName, true);
        form.addField("Contact Person", txtContactPerson, true);
        form.addField("Contact No.", txtContactNo, true);
        form.addField("E-mail", txtEmail, true);
        form.addField("Address", UITheme.scroll(txtAddress));
        form.addWide(sectionLabel("Login Details"));
        form.addField("Username", txtUsername, true);
        form.addField("Password", txtPassword, true);
        form.addField("Confirm Password", txtConfirm, true);
        form.addWide(chkShowPasswords);
        form.addWide(UITheme.mutedLabel("<html><div style='width:430px'>The warehouse reviews every registration. "
                + "You can log in as soon as "
                + "your account is approved. Passwords need at least " + Validator.MIN_PASSWORD_LENGTH
                + " characters.</div></html>"));

        FlatButton btnRegister = new FlatButton("Register", UITheme.PRIMARY, ModuleIcon.Type.SAVE);
        FlatButton btnClear = FlatButton.outline("Clear", ModuleIcon.Type.CLEAR);
        FlatButton btnCancel = FlatButton.outline("Cancel", ModuleIcon.Type.CLOSE);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnRegister);
        buttons.add(btnClear);
        buttons.add(btnCancel);

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Create a Customer Account");
        title.setFont(UITheme.FONT_TITLE.deriveFont(28f));
        title.setForeground(UITheme.BRAND);
        header.add(title, BorderLayout.NORTH);
        header.add(UITheme.mutedLabel("Register once, then request stock from our warehouses."),
                BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UITheme.CARD);
        root.setBorder(new EmptyBorder(20, 24, 18, 24));
        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        btnRegister.addActionListener(e -> register());
        btnClear.addActionListener(e -> clearForm());
        btnCancel.addActionListener(e -> dispose());
        chkShowPasswords.addItemListener(e -> {
            char echo = chkShowPasswords.isSelected() ? (char) 0 : echoChar;
            txtPassword.setEchoChar(echo);
            txtConfirm.setEchoChar(echo);
        });
        getRootPane().setDefaultButton(btnRegister);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setSize(Math.max(getWidth(), 600), getHeight());
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** The username of the new account, or null when the dialog was cancelled. */
    public String getRegisteredUsername() {
        return registeredUsername;
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = UITheme.accentLabel(text);
        label.setBorder(new EmptyBorder(4, 0, 0, 0));
        return label;
    }

    private void clearForm() {
        for (JTextField field : new JTextField[]{txtName, txtContactPerson, txtContactNo, txtEmail, txtUsername,
            txtPassword, txtConfirm}) {
            field.setText("");
        }
        txtAddress.setText("");
        chkShowPasswords.setSelected(false);
        txtName.requestFocusInWindow();
    }

    private void register() {
        char[] password = txtPassword.getPassword();
        char[] confirm = txtConfirm.getPassword();
        try {
            if (Validator.isEmpty(txtName.getText())) {
                Dialogs.invalid(this, "Customer name is required.", txtName);
                return;
            }
            if (Validator.isEmpty(txtContactPerson.getText())) {
                Dialogs.invalid(this, "Contact person is required.", txtContactPerson);
                return;
            }
            if (!Validator.isValidPhone(txtContactNo.getText())) {
                Dialogs.invalid(this, "Contact number must have 7 to 15 digits (example: 0917-555-0101).", txtContactNo);
                return;
            }
            if (!Validator.isValidEmail(txtEmail.getText())) {
                Dialogs.invalid(this, "Please enter a valid e-mail address (example: name@company.com).", txtEmail);
                return;
            }
            if (!Validator.isValidUsername(txtUsername.getText())) {
                Dialogs.invalid(this, "Username must have 4 to 30 letters, digits, dots or underscores (no spaces).",
                        txtUsername);
                return;
            }
            if (!Validator.isValidPassword(password)) {
                Dialogs.invalid(this, "Password must be " + Validator.MIN_PASSWORD_LENGTH + " to "
                        + Validator.MAX_PASSWORD_LENGTH + " characters long.", txtPassword);
                return;
            }
            if (!Arrays.equals(password, confirm)) {
                Dialogs.invalid(this, "The passwords do not match.", txtConfirm);
                return;
            }
            Customer customer = new Customer(0, txtName.getText().trim(), Validator.clean(txtContactPerson.getText()),
                    txtContactNo.getText().trim());
            customer.setEmail(Validator.clean(txtEmail.getText()));
            customer.setAddress(Validator.clean(txtAddress.getText()));
            String username = txtUsername.getText().trim();
            accountDAO.register(customer, username, password);
            Dialogs.success(this, "Thank you! Your registration was sent.\n\n"
                    + "The warehouse will review your account. You can log in as \"" + username
                    + "\"\nas soon as it is approved.");
            registeredUsername = username;
            dispose();
        } catch (BusinessRuleException e) {
            ErrorHandler.showRuleViolation(this, e);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "register the account");
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }
}
