package ui;

import dao.CustomerDAO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import model.Customer;
import model.CustomerAccount;
import ui.components.FlatButton;
import ui.components.FormGrid;
import ui.components.ModuleIcon;
import ui.components.RoundedPanel;
import util.ErrorHandler;
import util.LengthFilter;
import util.Session;
import util.Validator;

/**
 * Customer Portal: the customer's contact details and password. The name
 * and username stay under the warehouse's control (read-only here).
 */
public class CustomerProfilePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final JTextField txtName = new JTextField();
    private final JTextField txtUsername = new JTextField();
    private final JTextField txtContactPerson = new JTextField();
    private final JTextField txtContactNo = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextArea txtAddress = new JTextArea(3, 20);

    public CustomerProfilePanel() {
        super(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader("My Profile",
                "Keep your contact details up to date so the warehouse can reach you."), BorderLayout.NORTH);

        txtName.setEditable(false);
        txtUsername.setEditable(false);
        LengthFilter.apply(txtContactPerson, 100);
        LengthFilter.phone(txtContactNo);
        LengthFilter.apply(txtEmail, 100);
        LengthFilter.apply(txtAddress, 255);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);

        FormGrid form = new FormGrid();
        form.addField("Customer Name", txtName);
        form.addField("Username", txtUsername);
        form.addField("Contact Person", txtContactPerson, true);
        form.addField("Contact No.", txtContactNo, true);
        form.addField("E-mail", txtEmail, true);
        form.addField("Address", UITheme.scroll(txtAddress));
        form.addWide(UITheme.mutedLabel("<html>To change your customer name or username, please contact "
                + "the warehouse.</html>"));
        form.addFiller();

        FlatButton btnSave = new FlatButton("Save Changes", UITheme.PRIMARY, ModuleIcon.Type.SAVE);
        FlatButton btnUndo = FlatButton.outline("Undo Changes", ModuleIcon.Type.CLEAR);
        FlatButton btnPassword = FlatButton.outline("Change Password...", ModuleIcon.Type.KEY);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(btnSave);
        buttons.add(btnUndo);
        buttons.add(btnPassword);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12));
        card.add(UITheme.headingLabel("Contact Details"), BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(700, 10));
        add(card, BorderLayout.WEST);

        btnSave.addActionListener(e -> save());
        btnUndo.addActionListener(e -> refreshData());
        btnPassword.addActionListener(e ->
                new ChangePasswordDialog(javax.swing.SwingUtilities.getWindowAncestor(this)).setVisible(true));
    }

    @Override
    public void refreshData() {
        CustomerAccount account = Session.getCurrentCustomer();
        try {
            Customer c = customerDAO.getCustomerById(account.getCustomerId());
            if (c == null) {
                Dialogs.warning(this, "Your customer record no longer exists. Please contact the warehouse.");
                return;
            }
            txtName.setText(c.getCustomerName());
            txtUsername.setText(account.getUsername());
            txtContactPerson.setText(c.getContactPerson() == null ? "" : c.getContactPerson());
            txtContactNo.setText(c.getContactNo());
            txtEmail.setText(c.getEmail() == null ? "" : c.getEmail());
            txtAddress.setText(c.getAddress() == null ? "" : c.getAddress());
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load your profile");
        }
    }

    private void save() {
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
        Customer c = new Customer(Session.getCustomerId(), txtName.getText(),
                Validator.clean(txtContactPerson.getText()), txtContactNo.getText().trim());
        c.setEmail(Validator.clean(txtEmail.getText()));
        c.setAddress(Validator.clean(txtAddress.getText()));
        try {
            if (customerDAO.updateContactDetails(c)) {
                Dialogs.success(this, "Your contact details were saved.");
            } else {
                Dialogs.warning(this, "Your customer record no longer exists. Please contact the warehouse.");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "save your profile");
        }
    }
}
