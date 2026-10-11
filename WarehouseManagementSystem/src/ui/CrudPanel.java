package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Status;
import ui.components.FlatButton;
import ui.components.InputBorder;
import ui.components.ModuleIcon;
import ui.components.FormGrid;
import ui.components.RoundedPanel;
import ui.components.WrapLayout;
import util.ErrorHandler;
import util.Session;
import util.TableUtil;

/**
 * Base class of every "management form" (Warehouses, Categories, Products,
 * Suppliers, Customers, Users). It follows the layout of the requirements:
 *
 *   form fields  +  [SAVE] [UPDATE] [DELETE] [CLEAR]  +  Search  +  JTable
 *
 * Clicking a row loads the record into the form (select, modify, Update).
 * Each subclass only supplies its own fields, validation and DAO calls
 * (template method pattern).
 *
 * @param <T> the model class shown on the screen, e.g. Product
 */
public abstract class CrudPanel<T> extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    protected final JTextField txtId = new JTextField();
    protected final FlatButton btnSave = new FlatButton("Save", UITheme.PRIMARY, ModuleIcon.Type.SAVE);
    protected final FlatButton btnUpdate = FlatButton.outline("Update", ModuleIcon.Type.EDIT);
    protected final FlatButton btnDelete = FlatButton.outline("Delete", ModuleIcon.Type.DELETE).withAccent(UITheme.DANGER);
    protected final FlatButton btnClear = FlatButton.outline("Clear", ModuleIcon.Type.CLEAR);
    protected final JTextField txtSearch = new JTextField(17);
    protected final FlatButton btnSearch = new FlatButton("Search", UITheme.PRIMARY);
    protected final FlatButton btnShowAll = FlatButton.outline("Show All");
    protected final DefaultTableModel tableModel;
    protected final JTable table;

    private final JLabel lblCount = UITheme.mutedLabel(" ");
    private final JLabel lblMode = UITheme.mutedLabel(" ");
    private final String title;
    private final String subtitle;
    private final String entityName;

    /** The records currently shown in the table (same order as the table model). */
    protected List<T> records = new ArrayList<>();
    /** The record loaded in the form, or null while a new record is being added. */
    protected T selected;
    private boolean loadingTable;

    protected CrudPanel(String title, String subtitle, String entityName, String[] columns, Class<?>[] types) {
        this.title = title;
        this.subtitle = subtitle;
        this.entityName = entityName;
        this.tableModel = TableUtil.createModel(columns, types);
        this.table = TableUtil.createTable(tableModel);
    }

    // ------------------------------------------------------------------ methods for the subclasses

    /** Adds the input fields of the module to the form. */
    protected abstract void buildForm(FormGrid form);

    /** Reads the records for the table (using the search keyword and filters). */
    protected abstract List<T> fetchRecords(String keyword) throws SQLException;

    /** One table row for a record. */
    protected abstract Object[] toRow(T record);

    protected abstract int getId(T record);

    /** Copies a record into the input fields. */
    protected abstract void fillForm(T record);

    /** Empties the input fields. */
    protected abstract void clearFields();

    /** Enables or disables typing in the fields (view-only roles). */
    protected abstract void setFieldsEditable(boolean editable);

    /** Validates the fields and inserts a new record. */
    protected abstract void saveRecord();

    /** Validates the fields and updates the selected record. */
    protected abstract void updateRecord();

    /** Deletes the selected record after confirmation. */
    protected abstract void deleteRecord();

    /** Optional extra filters next to the search box. */
    protected void addFilters(JPanel filterBar) {
    }

    protected void resetFilters() {
    }

    /** Reloads combo boxes that come from other tables. */
    protected void reloadLookups() {
    }

    protected void focusFirstField() {
    }

    protected boolean canAdd() {
        return Session.canEditMasterData();
    }

    protected boolean canEdit() {
        return Session.canEditMasterData();
    }

    protected boolean canDelete() {
        return Session.canDelete();
    }

    // ------------------------------------------------------------------ layout

    /** Builds the screen. Called at the end of each subclass constructor. */
    protected final void buildUI() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader(title, subtitle), BorderLayout.NORTH);

        // ---------- left: the form
        txtId.setEditable(false);
        txtId.setFocusable(false);
        UITheme.setPlaceholder(txtId, "(auto-generated)");
        FormGrid form = new FormGrid();
        form.addField("ID", txtId);
        buildForm(form);
        form.addFiller();

        JPanel formHeader = new JPanel(new BorderLayout(0, 2));
        formHeader.setOpaque(false);
        formHeader.add(UITheme.headingLabel(entityName + " Details"), BorderLayout.NORTH);
        formHeader.add(lblMode, BorderLayout.SOUTH);

        boolean canModify = canAdd() || canEdit();
        JPanel buttons = new JPanel(new GridLayout(canModify ? 2 : 1, canModify ? 2 : 1, 12, 12));
        buttons.setOpaque(false);
        if (canModify) {
            buttons.add(btnSave);
            buttons.add(btnUpdate);
            buttons.add(btnDelete);
        }
        buttons.add(btnClear);

        JPanel formFooter = new JPanel(new BorderLayout(0, 8));
        formFooter.setOpaque(false);
        JLabel lblAccess = UITheme.mutedLabel(" ");
        if (!canModify) {
            lblAccess.setText("View only - your role cannot change these records.");
            setFieldsEditable(false);
        } else if (!canDelete()) {
            lblAccess.setText("Only administrators can delete records.");
            btnDelete.setToolTipText("Only administrators can delete records.");
        }
        lblAccess.setVisible(!lblAccess.getText().trim().isEmpty());   // no empty line under the form
        formFooter.add(lblAccess, BorderLayout.NORTH);
        formFooter.add(buttons, BorderLayout.CENTER);

        RoundedPanel formCard = new RoundedPanel(new BorderLayout(0, 8));
        formCard.add(formHeader, BorderLayout.NORTH);
        formCard.add(UITheme.plainScroll(form), BorderLayout.CENTER);
        formCard.add(formFooter, BorderLayout.SOUTH);
        formCard.setPreferredSize(new Dimension(400, 100));

        // ---------- right: search bar and table
        JPanel searchBar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        searchBar.setOpaque(false);
        searchBar.add(UITheme.accentLabel("Search:"));
        txtSearch.setToolTipText("Type a keyword and press Enter");
        txtSearch.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.SEARCH, 18, UITheme.TEXT_MUTED));
        UITheme.setPlaceholder(txtSearch, "Search " + entityName.toLowerCase() + "...");
        searchBar.add(txtSearch);
        addFilters(searchBar);
        searchBar.add(btnSearch);
        searchBar.add(btnShowAll);

        RoundedPanel listCard = new RoundedPanel(new BorderLayout(0, 10));
        listCard.add(searchBar, BorderLayout.NORTH);
        listCard.add(UITheme.scroll(table), BorderLayout.CENTER);
        listCard.add(lblCount, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(formCard, BorderLayout.WEST);
        body.add(listCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // ---------- events (ActionListener, ListSelectionListener)
        btnSave.addActionListener(e -> saveRecord());
        btnUpdate.addActionListener(e -> {
            if (selected != null) {
                updateRecord();
            }
        });
        btnDelete.addActionListener(e -> {
            if (selected != null) {
                deleteRecord();
            }
        });
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> loadTable());
        txtSearch.addActionListener(e -> loadTable());          // Enter key in the search box
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            resetFilters();
            loadTable();
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !loadingTable) {
                onRowSelected();
            }
        });
        updateButtons();
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    public void refreshData() {
        reloadLookups();
        clearForm();
        loadTable();
    }

    /** Reads the records from the database into the JTable. */
    protected final void loadTable() {
        Integer keepId = selected == null ? null : getId(selected);
        loadingTable = true;
        try {
            records = fetchRecords(txtSearch.getText().trim());
            tableModel.setRowCount(0);
            for (T record : records) {
                tableModel.addRow(toRow(record));
            }
            lblCount.setText(records.size() == 1 ? "1 record found" : records.size() + " records found");
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the " + entityName.toLowerCase() + " list");
        } finally {
            loadingTable = false;
        }
        if (keepId != null) {
            selectById(keepId);
        }
    }

    /** Selects the row of a record (or clears the form when it is not in the list). */
    protected void selectById(int id) {
        for (int i = 0; i < records.size(); i++) {
            if (getId(records.get(i)) == id) {
                int viewRow = table.convertRowIndexToView(i);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                    return;
                }
            }
        }
        clearForm();
    }

    private void onRowSelected() {
        int modelRow = TableUtil.getSelectedModelRow(table);
        if (modelRow < 0 || modelRow >= records.size()) {
            return;
        }
        selected = records.get(modelRow);
        txtId.setText(String.valueOf(getId(selected)));
        fillForm(selected);
        updateButtons();
    }

    /** Resets the form to "add a new record" mode. */
    public void clearForm() {
        selected = null;
        txtId.setText("(auto-generated)");
        clearFields();
        loadingTable = true;
        table.clearSelection();
        loadingTable = false;
        updateButtons();
        focusFirstField();
    }

    protected void updateButtons() {
        boolean editing = selected != null;
        btnSave.setEnabled(!editing && canAdd());
        btnUpdate.setEnabled(editing && canEdit());
        btnDelete.setEnabled(editing && canDelete());
        if (editing) {
            lblMode.setText(canEdit() ? "Editing record #" + getId(selected) + ". Change the fields, then click Update."
                    : "Viewing record #" + getId(selected) + ".");
        } else {
            lblMode.setText(canAdd() ? "New record. Fill in the fields, then click Save."
                    : "Select a record in the table to view its details.");
        }
    }

    /** Called after a successful insert, update or delete. */
    protected void afterChange(String message) {
        Dialogs.success(this, message);
        selected = null;
        loadTable();
        clearForm();
    }

    protected String getEntityName() {
        return entityName;
    }

    // ------------------------------------------------------------------ helpers for subclasses

    protected static String text(javax.swing.text.JTextComponent field) {
        return field.getText().trim();
    }

    /** Active / Inactive radio buttons in one group. */
    protected static JPanel statusPanel(JRadioButton active, JRadioButton inactive) {
        ButtonGroup group = new ButtonGroup();
        group.add(active);
        group.add(inactive);
        active.setOpaque(false);
        inactive.setOpaque(false);
        active.setSelected(true);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.add(active);
        panel.add(javax.swing.Box.createHorizontalStrut(16));
        panel.add(inactive);
        return panel;
    }

    protected static String statusOf(JRadioButton active) {
        return active.isSelected() ? Status.ACTIVE : Status.INACTIVE;
    }
}
