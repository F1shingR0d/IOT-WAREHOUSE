package ui;

import dao.SupplierDAO;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.Status;
import model.Supplier;
import ui.components.FormGrid;
import util.ErrorHandler;
import util.LengthFilter;
import util.TableUtil;
import util.Validator;

/**
 * Supplier management (the companies that deliver stock).
 */
public class SupplierPanel extends CrudPanel<Supplier> {

    private static final long serialVersionUID = 1L;

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final JTextField txtName = new JTextField();
    private final JTextField txtContactPerson = new JTextField();
    private final JTextField txtContactNo = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextArea txtAddress = new JTextArea(3, 20);
    private final JRadioButton rbActive = new JRadioButton("Active");
    private final JRadioButton rbInactive = new JRadioButton("Inactive");
    private final JComboBox<String> cboFilterStatus =
            new JComboBox<>(new String[]{"All statuses", Status.ACTIVE, Status.INACTIVE});

    public SupplierPanel() {
        super("Suppliers", "Companies that deliver stock to the warehouses.", "Supplier",
                new String[]{"ID", "Supplier Name", "Contact Person", "Contact No.", "E-mail", "Status"},
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
        form.addField("Supplier Name", txtName, true);
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
    protected List<Supplier> fetchRecords(String keyword) throws SQLException {
        String status = cboFilterStatus.getSelectedIndex() == 0 ? null : (String) cboFilterStatus.getSelectedItem();
        return supplierDAO.searchSuppliers(keyword, status);
    }

    @Override
    protected Object[] toRow(Supplier s) {
        return new Object[]{s.getSupplierId(), s.getSupplierName(), s.getContactPerson(), s.getContactNo(),
            s.getEmail(), s.getStatus()};
    }

    @Override
    protected int getId(Supplier s) {
        return s.getSupplierId();
    }

    @Override
    protected void fillForm(Supplier s) {
        txtName.setText(s.getSupplierName());
        txtContactPerson.setText(s.getContactPerson() == null ? "" : s.getContactPerson());
        txtContactNo.setText(s.getContactNo());
        txtEmail.setText(s.getEmail() == null ? "" : s.getEmail());
        txtAddress.setText(s.getAddress() == null ? "" : s.getAddress());
        rbActive.setSelected(Status.ACTIVE.equals(s.getStatus()));
        rbInactive.setSelected(!Status.ACTIVE.equals(s.getStatus()));
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

    private Supplier readForm(int supplierId) throws SQLException {
        if (Validator.isEmpty(txtName.getText())) {
            Dialogs.invalid(this, "Supplier name is required.", txtName);
            return null;
        }
        if (supplierDAO.isNameTaken(text(txtName), supplierId)) {
            Dialogs.invalid(this, "The supplier \"" + text(txtName) + "\" already exists.", txtName);
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
        Supplier supplier = new Supplier(supplierId, text(txtName), Validator.clean(txtContactPerson.getText()),
                text(txtContactNo));
        supplier.setEmail(Validator.clean(txtEmail.getText()));
        supplier.setAddress(Validator.clean(txtAddress.getText()));
        supplier.setStatus(statusOf(rbActive));
        return supplier;
    }

    @Override
    protected void saveRecord() {
        try {
            Supplier supplier = readForm(0);
            if (supplier != null) {
                int id = supplierDAO.addSupplier(supplier);
                afterChange("Supplier \"" + supplier.getSupplierName() + "\" was saved (ID " + id + ").");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "save the supplier");
        }
    }

    @Override
    protected void updateRecord() {
        try {
            Supplier supplier = readForm(selected.getSupplierId());
            if (supplier == null) {
                return;
            }
            if (supplierDAO.updateSupplier(supplier)) {
                afterChange("Supplier \"" + supplier.getSupplierName() + "\" was updated.");
            } else {
                Dialogs.warning(this, "This supplier no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "update the supplier");
        }
    }

    @Override
    protected void deleteRecord() {
        if (!Dialogs.confirmDelete(this, "Supplier: " + selected.getSupplierName())) {
            return;
        }
        try {
            if (supplierDAO.deleteSupplier(selected.getSupplierId())) {
                afterChange("The supplier was deleted.");
            } else {
                Dialogs.warning(this, "This supplier no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            // A supplier with receiving records is protected by the foreign key (SQLState 23503).
            ErrorHandler.showDatabaseError(this, e, "delete the supplier");
        }
    }
}
