package ui;

import dao.UserDAO;
import dao.WarehouseDAO;
import java.sql.SQLException;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import model.Role;
import model.Status;
import model.User;
import model.Warehouse;
import ui.components.FormGrid;
import util.ErrorHandler;
import util.Formats;
import util.LengthFilter;
import util.NumericFilter;
import util.TableUtil;
import util.Validator;

/**
 * Warehouse management. Each warehouse can have one person in charge
 * (a Warehouse Manager); a manager can be in charge of only one warehouse.
 */
public class WarehousePanel extends CrudPanel<Warehouse> {

    private static final long serialVersionUID = 1L;

    /** The "(None)" choice of the in-charge combo box. */
    private static final User NO_MANAGER = new User(0, "", "(None)", Role.MANAGER, Status.ACTIVE);

    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final UserDAO userDAO = new UserDAO();

    private final JTextField txtCode = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JTextField txtLocation = new JTextField();
    private final JTextField txtCapacity = new JTextField();
    private final JTextField txtContact = new JTextField();
    private final JComboBox<User> cboManager = new JComboBox<>();
    private final JRadioButton rbActive = new JRadioButton("Active");
    private final JRadioButton rbInactive = new JRadioButton("Inactive");
    private final JLabel lblUsage = UITheme.mutedLabel(" ");
    private final JComboBox<String> cboFilterStatus =
            new JComboBox<>(new String[]{"All statuses", Status.ACTIVE, Status.INACTIVE});

    public WarehousePanel() {
        super("Warehouses", "Storage locations, their capacity and the person in charge.",
                "Warehouse", new String[]{"ID", "Code", "Warehouse Name", "Location", "In-charge", "Stock",
                    "Capacity", "Used", "Status"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class,
                    Integer.class, Integer.class, Double.class, String.class});
        LengthFilter.code(txtCode, 20);
        LengthFilter.apply(txtName, 100);
        LengthFilter.apply(txtLocation, 255);
        NumericFilter.integer(txtCapacity, 8);
        LengthFilter.phone(txtContact);
        txtCode.setToolTipText("Unique code, e.g. WH-004");
        txtCapacity.setToolTipText("Maximum number of units the warehouse can hold");
        buildUI();
        TableUtil.setColumnWidths(table, 40, 68, 170, 210, 125, 64, 72, 62, 74);
    }

    @Override
    protected void buildForm(FormGrid form) {
        form.addField("Warehouse Code", txtCode, true);
        form.addField("Warehouse Name", txtName, true);
        form.addField("Location", txtLocation, true);
        form.addField("Capacity (units)", txtCapacity, true);
        form.addField("Contact No.", txtContact);
        form.addField("In-charge", cboManager);
        form.addField("Status", statusPanel(rbActive, rbInactive), true);
        form.addWide(lblUsage);
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
    protected List<Warehouse> fetchRecords(String keyword) throws SQLException {
        String status = cboFilterStatus.getSelectedIndex() == 0 ? null : (String) cboFilterStatus.getSelectedItem();
        return warehouseDAO.searchWarehouses(keyword, status);
    }

    @Override
    protected Object[] toRow(Warehouse w) {
        return new Object[]{w.getWarehouseId(), w.getWarehouseCode(), w.getWarehouseName(), w.getLocation(),
            w.getManagerName() == null ? "(None)" : w.getManagerName(), w.getStockOnHand(), w.getCapacity(),
            w.getUtilization(), w.getStatus()};
    }

    @Override
    protected int getId(Warehouse w) {
        return w.getWarehouseId();
    }

    /** Managers who may be put in charge (the one-to-one rule is checked by the DAO query). */
    private void loadManagers(int warehouseId, Integer currentManagerId) {
        DefaultComboBoxModel<User> model = new DefaultComboBoxModel<>();
        model.addElement(NO_MANAGER);
        try {
            for (User user : userDAO.getAssignableManagers(warehouseId)) {
                model.addElement(user);
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the list of managers");
        }
        cboManager.setModel(model);
        cboManager.setSelectedItem(NO_MANAGER);
        if (currentManagerId != null) {
            for (int i = 0; i < model.getSize(); i++) {
                if (model.getElementAt(i).getUserId() == currentManagerId) {
                    cboManager.setSelectedIndex(i);
                }
            }
        }
    }

    @Override
    protected void fillForm(Warehouse w) {
        txtCode.setText(w.getWarehouseCode());
        txtName.setText(w.getWarehouseName());
        txtLocation.setText(w.getLocation());
        txtCapacity.setText(String.valueOf(w.getCapacity()));
        txtContact.setText(w.getContactNo() == null ? "" : w.getContactNo());
        loadManagers(w.getWarehouseId(), w.getManagerId());
        rbActive.setSelected(Status.ACTIVE.equals(w.getStatus()));
        rbInactive.setSelected(!Status.ACTIVE.equals(w.getStatus()));
        lblUsage.setText("Currently stored: " + Formats.number(w.getStockOnHand()) + " of "
                + Formats.number(w.getCapacity()) + " units (" + Formats.percent(w.getUtilization()) + " used)");
    }

    @Override
    protected void clearFields() {
        txtCode.setText("");
        txtName.setText("");
        txtLocation.setText("");
        txtCapacity.setText("");
        txtContact.setText("");
        loadManagers(0, null);
        rbActive.setSelected(true);
        lblUsage.setText(" ");
    }

    @Override
    protected void setFieldsEditable(boolean editable) {
        txtCode.setEditable(editable);
        txtName.setEditable(editable);
        txtLocation.setEditable(editable);
        txtCapacity.setEditable(editable);
        txtContact.setEditable(editable);
        cboManager.setEnabled(editable);
        rbActive.setEnabled(editable);
        rbInactive.setEnabled(editable);
    }

    @Override
    protected void focusFirstField() {
        txtCode.requestFocusInWindow();
    }

    private Warehouse readForm(int warehouseId, int currentStock) throws SQLException {
        if (Validator.isEmpty(txtCode.getText())) {
            Dialogs.invalid(this, "Warehouse code is required.", txtCode);
            return null;
        }
        if (!Validator.isValidCode(txtCode.getText())) {
            Dialogs.invalid(this, "Warehouse code must be 2-20 letters, digits or dashes (example: WH-004).", txtCode);
            return null;
        }
        if (warehouseDAO.isCodeTaken(text(txtCode), warehouseId)) {
            Dialogs.invalid(this, "The code " + text(txtCode).toUpperCase() + " is already used.", txtCode);
            return null;
        }
        if (Validator.isEmpty(txtName.getText())) {
            Dialogs.invalid(this, "Warehouse name is required.", txtName);
            return null;
        }
        if (warehouseDAO.isNameTaken(text(txtName), warehouseId)) {
            Dialogs.invalid(this, "Another warehouse is already named \"" + text(txtName) + "\".", txtName);
            return null;
        }
        if (Validator.isEmpty(txtLocation.getText())) {
            Dialogs.invalid(this, "Location is required.", txtLocation);
            return null;
        }
        Integer capacity = Validator.parseInteger(txtCapacity.getText());
        if (capacity == null || capacity <= 0) {
            Dialogs.invalid(this, "Capacity must be a whole number greater than 0.", txtCapacity);
            return null;
        }
        if (capacity < currentStock) {
            Dialogs.invalid(this, "Capacity cannot be lower than the units currently stored ("
                    + Formats.number(currentStock) + ").", txtCapacity);
            return null;
        }
        if (!Validator.isEmpty(txtContact.getText()) && !Validator.isValidPhone(txtContact.getText())) {
            Dialogs.invalid(this, "Contact number must have 7 to 15 digits (example: 0917-555-0101).", txtContact);
            return null;
        }
        Warehouse warehouse = new Warehouse(warehouseId, text(txtCode).toUpperCase(), text(txtName),
                text(txtLocation), capacity);
        warehouse.setContactNo(Validator.clean(txtContact.getText()));
        User manager = (User) cboManager.getSelectedItem();
        warehouse.setManagerId(manager == null || manager == NO_MANAGER ? null : manager.getUserId());
        warehouse.setStatus(statusOf(rbActive));
        return warehouse;
    }

    @Override
    protected void saveRecord() {
        try {
            Warehouse warehouse = readForm(0, 0);
            if (warehouse != null) {
                int id = warehouseDAO.addWarehouse(warehouse);
                afterChange("Warehouse " + warehouse.getWarehouseCode() + " - " + warehouse.getWarehouseName()
                        + " was saved (ID " + id + ").");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "save the warehouse");
        }
    }

    @Override
    protected void updateRecord() {
        try {
            Warehouse warehouse = readForm(selected.getWarehouseId(), selected.getStockOnHand());
            if (warehouse == null) {
                return;
            }
            if (Status.INACTIVE.equals(warehouse.getStatus()) && Status.ACTIVE.equals(selected.getStatus())
                    && selected.getStockOnHand() > 0
                    && !Dialogs.confirm(this, selected.getWarehouseName() + " still holds "
                            + Formats.number(selected.getStockOnHand()) + " units.\n"
                            + "An inactive warehouse cannot be used in new transactions.\n\nDeactivate it anyway?",
                            "Deactivate Warehouse")) {
                return;
            }
            if (warehouseDAO.updateWarehouse(warehouse)) {
                afterChange("Warehouse " + warehouse.getWarehouseCode() + " was updated.");
            } else {
                Dialogs.warning(this, "This warehouse no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "update the warehouse");
        }
    }

    @Override
    protected void deleteRecord() {
        if (selected.getStockOnHand() > 0) {
            Dialogs.warning(this, selected.getWarehouseName() + " cannot be deleted because it still holds "
                    + Formats.number(selected.getStockOnHand()) + " units.\n\nTip: set its status to INACTIVE instead.");
            return;
        }
        if (!Dialogs.confirmDelete(this, "Warehouse: " + selected.getWarehouseCode() + " - " + selected.getWarehouseName())) {
            return;
        }
        try {
            if (warehouseDAO.deleteWarehouse(selected.getWarehouseId())) {
                afterChange("The warehouse was deleted.");
            } else {
                Dialogs.warning(this, "This warehouse no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "delete the warehouse");
        }
    }
}
