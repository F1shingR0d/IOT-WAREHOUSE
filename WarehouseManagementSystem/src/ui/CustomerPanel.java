package ui;

import dao.CustomerDAO;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.Customer;
import model.Status;
import ui.components.FormGrid;
import util.ErrorHandler;
import util.LengthFilter;
import util.TableUtil;
import util.Validator;

/**
 * Customer management (the consignees that receive released stock).
 */
public class CustomerPanel extends CrudPanel<Customer> {

    private static final long serialVersionUID = 1L;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final JTextField txtName = new JTextField();
    private final JTextField txtContactPerson = new JTextField();
    private final JTextField txtContactNo = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextArea txtAddress = new JTextArea(3, 20);
    private final JRadioButton rbActive = new JRadioButton("Active");
    private final JRadioButton rbInactive = new JRadioButton("Inactive");
    private final JComboBox<String> cboFilterStatus =
            new JComboBox<>(new String[]{"All statuses", Status.ACTIVE, Status.INACTIVE});

    public CustomerPanel() {
        super("Customers", "Customers and branches that receive stock released from the warehouses.", "Customer",
                new String[]{"ID", "Customer Name", "Contact Person", "Contact No.", "E-mail", "Status"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class, String.class});
        LengthFilter.apply(txtName, 100);
        LengthFilter.apply(txtContactPerson, 100);
        LengthFilter.phone(txtContactNo);
        LengthFilter.apply(txtEmail, 100);
        LengthFilter.apply(txtAddress, 255);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        buildUI();
        TableUtil.setColumnWidths(table, 45, 220, 150, 120, 230, 75);
    }

    @Override
    protected void buildForm(FormGrid form) {
        form.addField("Customer Name", txtName, true);
        form.addField("Contact Person", txtContactPerson);
        form.addField("Contact No.", txtContactNo, true);
        form.addField("E-mail", txtEmail);
        form.addField("Address", UITheme.scroll(txtAddress));
        form.addField("Status", statusPanel(rbActive, rbInactive), true);
    }

    @Override
    protected void addFilters(JPanel filterBar) {
        filterBar.add(cboFilterStatus);
        cboFilterStatus.addActionListener(e -> loadTable());
    }

    @Override
    protected void resetFilters() {
        cboFilterStatus.setSelectedIndex(0);
    }

    @Override
    protected List<Customer> fetchRecords(String keyword) throws SQLException {
        String status = cboFilterStatus.getSelectedIndex() == 0 ? null : (String) cboFilterStatus.getSelectedItem();
        return customerDAO.searchCustomers(keyword, status);
    }

    @Override
    protected Object[] toRow(Customer c) {
        return new Object[]{c.getCustomerId(), c.getCustomerName(), c.getContactPerson(), c.getContactNo(),
            c.getEmail(), c.getStatus()};
    }

    @Override
    protected int getId(Customer c) {
        return c.getCustomerId();
    }

    @Override
    protected void fillForm(Customer c) {
        txtName.setText(c.getCustomerName());
        txtContactPerson.setText(c.getContactPerson() == null ? "" : c.getContactPerson());
        txtContactNo.setText(c.getContactNo());
        txtEmail.setText(c.getEmail() == null ? "" : c.getEmail());
        txtAddress.setText(c.getAddress() == null ? "" : c.getAddress());
        rbActive.setSelected(Status.ACTIVE.equals(c.getStatus()));
        rbInactive.setSelected(!Status.ACTIVE.equals(c.getStatus()));
    }

    @Override
    protected void clearFields() {
        txtName.setText("");
        txtContactPerson.setText("");
        txtContactNo.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        rbActive.setSelected(true);
    }

    @Override
    protected void setFieldsEditable(boolean editable) {
        txtName.setEditable(editable);
        txtContactPerson.setEditable(editable);
        txtContactNo.setEditable(editable);
        txtEmail.setEditable(editable);
        txtAddress.setEditable(editable);
        rbActive.setEnabled(editable);
        rbInactive.setEnabled(editable);
    }

    @Override
    protected void focusFirstField() {
        txtName.requestFocusInWindow();
    }

    private Customer readForm(int customerId) throws SQLException {
        if (Validator.isEmpty(txtName.getText())) {
            Dialogs.invalid(this, "Customer name is required.", txtName);
            return null;
        }
        if (customerDAO.isNameTaken(text(txtName), customerId)) {
            Dialogs.invalid(this, "The customer \"" + text(txtName) + "\" already exists.", txtName);
            return null;
        }
        if (Validator.isEmpty(txtContactNo.getText())) {
            Dialogs.invalid(this, "Contact number is required.", txtContactNo);
            return null;
        }
        if (!Validator.isValidPhone(txtContactNo.getText())) {
            Dialogs.invalid(this, "Contact number must have 7 to 15 digits (example: 0917-555-0101).", txtContactNo);
            return null;
        }
        if (!Validator.isEmpty(txtEmail.getText()) && !Validator.isValidEmail(txtEmail.getText())) {
            Dialogs.invalid(this, "Please enter a valid e-mail address (example: name@company.com).", txtEmail);
            return null;
        }
        Customer customer = new Customer(customerId, text(txtName), Validator.clean(txtContactPerson.getText()),
                text(txtContactNo));
        customer.setEmail(Validator.clean(txtEmail.getText()));
        customer.setAddress(Validator.clean(txtAddress.getText()));
        customer.setStatus(statusOf(rbActive));
        return customer;
    }

    @Override
    protected void saveRecord() {
        try {
            Customer customer = readForm(0);
            if (customer != null) {
                int id = customerDAO.addCustomer(customer);
                afterChange("Customer \"" + customer.getCustomerName() + "\" was saved (ID " + id + ").");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "save the customer");
        }
    }

    @Override
    protected void updateRecord() {
        try {
            Customer customer = readForm(selected.getCustomerId());
            if (customer == null) {
                return;
            }
            if (customerDAO.updateCustomer(customer)) {
                afterChange("Customer \"" + customer.getCustomerName() + "\" was updated.");
            } else {
                Dialogs.warning(this, "This customer no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "update the customer");
        }
    }

    @Override
    protected void deleteRecord() {
        if (!Dialogs.confirmDelete(this, "Customer: " + selected.getCustomerName())) {
            return;
        }
        try {
            if (customerDAO.deleteCustomer(selected.getCustomerId())) {
                afterChange("The customer was deleted.");
            } else {
                Dialogs.warning(this, "This customer no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            // A customer with release records is protected by the foreign key (SQLState 23503).
            ErrorHandler.showDatabaseError(this, e, "delete the customer");
        }
    }
}
