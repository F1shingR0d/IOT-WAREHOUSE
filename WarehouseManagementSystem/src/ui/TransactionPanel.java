package ui;

import dao.ProductDAO;
import dao.StockDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Product;
import model.Status;
import model.Warehouse;
import ui.components.DateField;
import ui.components.FlatButton;
import ui.components.InputBorder;
import ui.components.ModuleIcon;
import ui.components.FormGrid;
import ui.components.RoundedPanel;
import ui.components.WrapLayout;
import util.BusinessRuleException;
import util.ErrorHandler;
import util.Formats;
import util.LengthFilter;
import util.NumericFilter;
import util.Session;
import util.TableUtil;
import util.Validator;

/**
 * Base class of the Receiving, Releasing and Stock Transfer screens.
 *
 * Tab 1 "New ..." : document header (date, warehouse, supplier/customer,
 *                   remarks) plus a list of product lines, saved as one
 *                   database transaction.
 * Tab 2 "History" : search by keyword, date range and status; view the
 *                   details of a document or void it (managers and admins).
 */
public abstract class TransactionPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_QUANTITY = 1_000_000;

    /** One product line of the document being entered. */
    protected static final class Line {
        final Product product;
        int quantity;
        BigDecimal unitCost;
        int available;

        Line(Product product, int quantity, BigDecimal unitCost, int available) {
            this.product = product;
            this.quantity = quantity;
            this.unitCost = unitCost;
            this.available = available;
        }
    }

    protected final StockDAO stockDAO = new StockDAO();
    private final ProductDAO productDAO = new ProductDAO();

    // ---------------- "New" tab
    protected final DateField dateField = new DateField();
    protected final JTextArea txtRemarks = new JTextArea(2, 20);
    protected final JComboBox<Product> cboProduct = new JComboBox<>();
    protected final JTextField txtQuantity = new JTextField(7);
    protected final JTextField txtUnitCost = new JTextField(9);
    private final JLabel lblProductInfo = UITheme.mutedLabel(" ");
    private final FlatButton btnAddItem = new FlatButton("Add Item", UITheme.PRIMARY, ModuleIcon.Type.ADD);
    private final FlatButton btnRemoveItem = FlatButton.outline("Remove Selected Item", ModuleIcon.Type.REMOVE).withAccent(UITheme.DANGER);
    private final FlatButton btnSave;
    private final FlatButton btnClear = FlatButton.outline("Clear", ModuleIcon.Type.CLEAR);
    private final DefaultTableModel itemsModel;
    private final JTable itemsTable;
    private final JLabel lblTotals = new JLabel(" ");
    protected final List<Line> lines = new ArrayList<>();

    // ---------------- "History" tab
    private final JTextField txtHistorySearch = new JTextField(16);
    private final DateField historyFrom = new DateField(LocalDate.now().minusDays(30));
    private final DateField historyTo = new DateField(LocalDate.now());
    private final JComboBox<String> cboHistoryStatus =
            new JComboBox<>(new String[]{"All statuses", Status.POSTED, Status.VOIDED});
    private final FlatButton btnHistorySearch = new FlatButton("Search", UITheme.PRIMARY, ModuleIcon.Type.SEARCH);
    private final FlatButton btnHistoryReset = FlatButton.outline("Reset", ModuleIcon.Type.CLEAR);
    private final FlatButton btnViewDetails = FlatButton.outline("View Details", ModuleIcon.Type.VIEW);
    private final FlatButton btnVoid = FlatButton.outline("Void", ModuleIcon.Type.VOID).withAccent(UITheme.WARNING);
    private final FlatButton btnPrintList = FlatButton.outline("Print List", ModuleIcon.Type.PRINT);
    protected final DefaultTableModel historyModel;
    protected final JTable historyTable;
    private final JLabel lblHistoryCount = UITheme.mutedLabel(" ");
    private final JTabbedPane tabs = new JTabbedPane();

    private final String title;
    private final String subtitle;
    private final String documentName;
    private final boolean withUnitCost;
    private final boolean checksAvailability;

    /** true while combo boxes are changed by the program (not by the user). */
    private boolean quiet;
    private Warehouse lastSourceWarehouse;

    protected TransactionPanel(String title, String subtitle, String documentName, boolean withUnitCost,
                               boolean checksAvailability, String[] historyColumns, Class<?>[] historyTypes) {
        this.title = title;
        this.subtitle = subtitle;
        this.documentName = documentName;
        this.withUnitCost = withUnitCost;
        this.checksAvailability = checksAvailability;
        this.btnSave = new FlatButton("Save " + documentName, UITheme.PRIMARY, ModuleIcon.Type.SAVE);

        if (withUnitCost) {
            itemsModel = TableUtil.createModel(
                    new String[]{"#", "SKU", "Product", "Unit", "Quantity", "Unit Cost", "Subtotal"},
                    new Class<?>[]{Integer.class, String.class, String.class, String.class, Integer.class,
                        BigDecimal.class, BigDecimal.class});
        } else {
            itemsModel = TableUtil.createModel(
                    new String[]{"#", "SKU", "Product", "Unit", "Quantity", "Available"},
                    new Class<?>[]{Integer.class, String.class, String.class, String.class, Integer.class,
                        Integer.class});
        }
        itemsTable = TableUtil.createTable(itemsModel);
        itemsTable.setAutoCreateRowSorter(false);
        itemsTable.setRowSorter(null);   // row numbers must match the order of the lines
        historyModel = TableUtil.createModel(historyColumns, historyTypes);
        historyTable = TableUtil.createTable(historyModel);
    }

    // ------------------------------------------------------------------ methods for the subclasses

    /** Adds the header fields (warehouse, supplier, customer...). */
    protected abstract void addHeaderFields(HeaderGrid grid);

    /** Reloads the header combo boxes (keep the current selection when possible). */
    protected abstract void reloadHeaderLookups() throws SQLException;

    /** The warehouse whose stock is shown/checked while adding items. */
    protected abstract Warehouse getSourceWarehouse();

    /** The component to focus when the warehouse is missing. */
    protected abstract JComponent getSourceWarehouseField();

    /** Checks the header fields; shows a message and returns false when something is missing. */
    protected abstract boolean validateHeader();

    /** Saves the document with the current lines; returns the new reference number. */
    protected abstract String saveDocument(LocalDate date, String remarks) throws SQLException, BusinessRuleException;

    protected abstract void clearHeaderFields();

    /** History rows: the first column must be the ID and the last column the status. */
    protected abstract List<Object[]> fetchHistory(String keyword, LocalDate from, LocalDate to, String status)
            throws SQLException;

    protected abstract void showDetails(int documentId) throws SQLException;

    protected abstract void voidDocument(int documentId) throws SQLException, BusinessRuleException;

    /** What voiding does to the stock (shown in the confirmation). */
    protected abstract String describeVoid();

    // ------------------------------------------------------------------ layout

    protected final void buildUI() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader(title, subtitle), BorderLayout.NORTH);

        tabs.setFont(UITheme.FONT_BOLD);
        tabs.addTab("New " + documentName, buildNewTab());
        tabs.addTab(documentName + " History", buildHistoryTab());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) {
                loadHistory();
            }
        });
        add(tabs, BorderLayout.CENTER);
    }

    private JComponent buildNewTab() {
        // ---------- document header card
        HeaderGrid grid = new HeaderGrid();
        JLabel lblReference = UITheme.mutedLabel("Assigned automatically when saved");
        grid.add("Reference No.", lblReference, false);
        grid.add("Date (yyyy-MM-dd)", dateField, true);
        addHeaderFields(grid);
        LengthFilter.apply(txtRemarks, 255);
        txtRemarks.setLineWrap(true);
        txtRemarks.setWrapStyleWord(true);
        grid.addWide("Remarks", UITheme.scroll(txtRemarks));

        RoundedPanel headerCard = new RoundedPanel(new BorderLayout(0, 8));
        headerCard.add(UITheme.headingLabel(documentName + " Details"), BorderLayout.NORTH);
        headerCard.add(grid, BorderLayout.CENTER);

        // ---------- item entry row
        NumericFilter.integer(txtQuantity, 7);
        NumericFilter.decimal(txtUnitCost, 8, 2);
        cboProduct.setRenderer(UITheme.placeholderRenderer("Select a product"));
        cboProduct.setMaximumRowCount(14);

        JPanel entry = new JPanel(new GridBagLayout());
        entry.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 8);
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        entry.add(UITheme.label("Product"), c);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        entry.add(cboProduct, c);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        entry.add(UITheme.label("Quantity"), c);
        entry.add(txtQuantity, c);
        if (withUnitCost) {
            entry.add(UITheme.label("Unit Cost (" + Formats.PESO + ")"), c);
            entry.add(txtUnitCost, c);
        }
        c.insets = new Insets(0, 0, 0, 0);
        entry.add(btnAddItem, c);

        JPanel entryBlock = new JPanel(new BorderLayout(0, 6));
        entryBlock.setOpaque(false);
        entryBlock.add(UITheme.headingLabel("Items"), BorderLayout.NORTH);
        entryBlock.add(entry, BorderLayout.CENTER);
        entryBlock.add(lblProductInfo, BorderLayout.SOUTH);

        // ---------- items table and footer
        TableUtil.setColumnWidths(itemsTable, withUnitCost
                ? new int[]{40, 90, 280, 70, 90, 110, 120}
                : new int[]{40, 90, 300, 70, 90, 110});
        JScrollPane itemsScroll = UITheme.scroll(itemsTable);

        lblTotals.setFont(UITheme.FONT_BOLD);
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.add(btnRemoveItem, BorderLayout.WEST);
        JPanel totalsBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        totalsBox.setOpaque(false);
        totalsBox.add(lblTotals);
        footer.add(totalsBox, BorderLayout.CENTER);
        JPanel saveButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        saveButtons.setOpaque(false);
        saveButtons.add(btnSave);
        saveButtons.add(btnClear);
        footer.add(saveButtons, BorderLayout.EAST);

        RoundedPanel itemsCard = new RoundedPanel(new BorderLayout(0, 10));
        itemsCard.add(entryBlock, BorderLayout.NORTH);
        itemsCard.add(itemsScroll, BorderLayout.CENTER);
        itemsCard.add(footer, BorderLayout.SOUTH);

        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setBackground(UITheme.BACKGROUND);
        tab.setBorder(new EmptyBorder(12, 2, 2, 2));
        tab.add(headerCard, BorderLayout.NORTH);
        tab.add(itemsCard, BorderLayout.CENTER);

        // ---------- events
        cboProduct.addActionListener(e -> {
            if (!quiet) {
                productChanged();
            }
        });
        txtQuantity.addActionListener(e -> addItem());       // Enter adds the item
        txtUnitCost.addActionListener(e -> addItem());
        btnAddItem.addActionListener(e -> addItem());
        btnRemoveItem.addActionListener(e -> removeSelectedItem());
        itemsTable.addKeyListener(new KeyAdapter() {        // Delete key removes the selected line
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedItem();
                }
            }
        });
        btnSave.addActionListener(e -> saveClicked());
        btnClear.addActionListener(e -> {
            if (lines.isEmpty() || Dialogs.confirm(this, "Discard the items of this " + documentName.toLowerCase()
                    + "?", "Clear Form")) {
                resetDocument();
            }
        });
        updateTotals();
        return tab;
    }

    private JComponent buildHistoryTab() {
        JPanel filters = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        filters.setOpaque(false);
        txtHistorySearch.setToolTipText("Reference no., warehouse, supplier/customer, user or product");
        txtHistorySearch.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.SEARCH, 18, UITheme.TEXT_MUTED));
        UITheme.setPlaceholder(txtHistorySearch, "Reference, supplier, product...");
        filters.add(UITheme.accentLabel("Search:"));
        filters.add(txtHistorySearch);
        filters.add(UITheme.accentLabel("From:"));
        filters.add(historyFrom);
        filters.add(UITheme.accentLabel("To:"));
        filters.add(historyTo);
        filters.add(cboHistoryStatus);
        filters.add(btnHistorySearch);
        filters.add(btnHistoryReset);

        TableUtil.hideColumn(historyTable, 0);   // the ID column stays in the model only

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblHistoryCount, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnViewDetails);
        actions.add(btnVoid);
        actions.add(btnPrintList);
        bottom.add(actions, BorderLayout.EAST);
        if (!Session.canVoid()) {
            btnVoid.setToolTipText("Only managers and administrators can void documents.");
        }

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.add(filters, BorderLayout.NORTH);
        card.add(UITheme.scroll(historyTable), BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(UITheme.BACKGROUND);
        tab.setBorder(new EmptyBorder(12, 2, 2, 2));
        tab.add(card, BorderLayout.CENTER);

        btnHistorySearch.addActionListener(e -> loadHistory());
        txtHistorySearch.addActionListener(e -> loadHistory());
        btnHistoryReset.addActionListener(e -> {
            txtHistorySearch.setText("");
            historyFrom.setDate(LocalDate.now().minusDays(30));
            historyTo.setDate(LocalDate.now());
            cboHistoryStatus.setSelectedIndex(0);
            loadHistory();
        });
        cboHistoryStatus.addActionListener(e -> loadHistory());
        historyTable.getSelectionModel().addListSelectionListener(e -> updateHistoryButtons());
        historyTable.addMouseListener(new MouseAdapter() {   // double-click opens the details
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && historyTable.getSelectedRow() >= 0) {
                    viewDetailsClicked();
                }
            }
        });
        btnViewDetails.addActionListener(e -> viewDetailsClicked());
        btnVoid.addActionListener(e -> voidClicked());
        btnPrintList.addActionListener(e -> TableUtil.print(this, historyTable, documentName + " History"));
        updateHistoryButtons();
        return tab;
    }

    // ------------------------------------------------------------------ data loading

    @Override
    public void refreshData() {
        runQuietly(() -> {
            try {
                reloadProducts();
                reloadHeaderLookups();
            } catch (SQLException e) {
                ErrorHandler.showDatabaseError(this, e, "load the lists");
            }
        });
        lastSourceWarehouse = getSourceWarehouse();
        productChanged();
        if (tabs.getSelectedIndex() == 1) {
            loadHistory();
        }
    }

    /** Active products for the product picker; keeps the current choice. */
    private void reloadProducts() throws SQLException {
        Product current = (Product) cboProduct.getSelectedItem();
        List<Product> products = productDAO.getActiveProducts();
        cboProduct.setModel(new DefaultComboBoxModel<>(products.toArray(new Product[0])));
        cboProduct.setSelectedItem(null);
        if (current != null) {
            for (Product p : products) {
                if (p.getProductId() == current.getProductId()) {
                    cboProduct.setSelectedItem(p);
                }
            }
        }
    }

    /** Changes combo boxes without triggering the "warehouse changed" questions. */
    protected void runQuietly(Runnable action) {
        boolean previous = quiet;
        quiet = true;
        try {
            action.run();
        } finally {
            quiet = previous;
        }
    }

    /** Selects the item whose toString() matches, or nothing. */
    protected static <T> void reselect(JComboBox<T> combo, T previous) {
        combo.setSelectedItem(null);
        if (previous == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).toString().equals(previous.toString())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * Subclasses call this when the source warehouse combo changes. Existing
     * lines were checked against the old warehouse, so they must be cleared.
     */
    protected void sourceWarehouseChanged() {
        if (quiet) {
            return;
        }
        Warehouse current = getSourceWarehouse();
        if (checksAvailability && !lines.isEmpty() && current != lastSourceWarehouse) {
            if (Dialogs.confirm(this, "The item list was checked against the stock of the previous warehouse.\n"
                    + "Changing the warehouse will clear the item list. Continue?", "Change Warehouse")) {
                lines.clear();
                refreshItemsTable();
            } else {
                runQuietly(() -> ((JComboBox<?>) getSourceWarehouseField()).setSelectedItem(lastSourceWarehouse));
                return;
            }
        }
        lastSourceWarehouse = current;
        productChanged();
    }

    /** Shows unit, cost and available stock of the chosen product. */
    private void productChanged() {
        Product product = (Product) cboProduct.getSelectedItem();
        if (product == null) {
            lblProductInfo.setText(" ");
            return;
        }
        if (withUnitCost && selectedLine(product) == null) {
            txtUnitCost.setText(product.getUnitCost().toPlainString());
        }
        StringBuilder info = new StringBuilder("Unit: " + product.getUnit());
        if (withUnitCost) {
            info.append("     Standard cost: ").append(Formats.money(product.getUnitCost()));
        }
        Warehouse warehouse = getSourceWarehouse();
        if (warehouse == null) {
            info.append("     Select a warehouse to see its stock.");
        } else {
            try {
                int stock = stockDAO.getAvailable(warehouse.getWarehouseId(), product.getProductId());
                Line line = selectedLine(product);
                int inList = line == null ? 0 : line.quantity;
                if (checksAvailability) {
                    info.append("     Available in ").append(warehouse.getWarehouseName()).append(": ")
                        .append(Formats.number(stock - inList)).append(' ').append(product.getUnit());
                    if (inList > 0) {
                        info.append("  (").append(Formats.number(inList)).append(" already in the list)");
                    }
                } else {
                    info.append("     Current stock in ").append(warehouse.getWarehouseName()).append(": ")
                        .append(Formats.number(stock)).append(' ').append(product.getUnit());
                }
            } catch (SQLException e) {
                info.append("     (stock could not be read)");
                ErrorHandler.log(e);
            }
        }
        lblProductInfo.setText(info.toString());
    }

    private Line selectedLine(Product product) {
        for (Line line : lines) {
            if (line.product.getProductId() == product.getProductId()) {
                return line;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ item list

    private void addItem() {
        Product product = (Product) cboProduct.getSelectedItem();
        if (product == null) {
            Dialogs.invalid(this, "Please select a product.", cboProduct);
            return;
        }
        Warehouse warehouse = getSourceWarehouse();
        if (warehouse == null) {
            Dialogs.invalid(this, "Please select the warehouse first.", getSourceWarehouseField());
            return;
        }
        Integer quantity = Validator.parseInteger(txtQuantity.getText());
        if (quantity == null || quantity <= 0) {
            Dialogs.invalid(this, "Quantity must be a whole number greater than 0.", txtQuantity);
            return;
        }
        BigDecimal unitCost = null;
        if (withUnitCost) {
            unitCost = Validator.parseAmount(txtUnitCost.getText());
            if (unitCost == null) {
                Dialogs.invalid(this, "Unit cost must be a valid amount (0 or more, up to 2 decimals).", txtUnitCost);
                return;
            }
        }
        Line existing = selectedLine(product);
        int newQuantity = quantity + (existing == null ? 0 : existing.quantity);
        if (newQuantity > MAX_QUANTITY) {
            Dialogs.invalid(this, "The quantity is too large (maximum " + Formats.number(MAX_QUANTITY) + ").", txtQuantity);
            return;
        }
        int available = 0;
        if (checksAvailability) {
            try {
                available = stockDAO.getAvailable(warehouse.getWarehouseId(), product.getProductId());
            } catch (SQLException e) {
                ErrorHandler.showDatabaseError(this, e, "read the available stock");
                return;
            }
            if (newQuantity > available) {
                Dialogs.invalid(this, "Not enough stock of " + product.getProductName() + " in "
                        + warehouse.getWarehouseName() + ".\n\nAvailable: " + Formats.number(available) + " "
                        + product.getUnit() + (existing == null ? "" : "\nAlready in the list: "
                        + Formats.number(existing.quantity)) + "\nRequested: " + Formats.number(quantity), txtQuantity);
                return;
            }
        }
        if (existing != null) {
            existing.quantity = newQuantity;       // same product twice = one line with the total
            existing.available = available;
            if (withUnitCost) {
                existing.unitCost = unitCost;
            }
        } else {
            lines.add(new Line(product, quantity, unitCost, available));
        }
        refreshItemsTable();
        txtQuantity.setText("");
        runQuietly(() -> cboProduct.setSelectedItem(null));
        lblProductInfo.setText(" ");
        cboProduct.requestFocusInWindow();
    }

    private void removeSelectedItem() {
        int row = itemsTable.getSelectedRow();
        if (row < 0) {
            Dialogs.info(this, "Select an item in the list first.");
            return;
        }
        lines.remove(row);
        refreshItemsTable();
        productChanged();
    }

    private void refreshItemsTable() {
        itemsModel.setRowCount(0);
        int number = 1;
        for (Line line : lines) {
            Product p = line.product;
            if (withUnitCost) {
                itemsModel.addRow(new Object[]{number++, p.getSku(), p.getProductName(), p.getUnit(), line.quantity,
                    line.unitCost, line.unitCost.multiply(BigDecimal.valueOf(line.quantity))});
            } else {
                itemsModel.addRow(new Object[]{number++, p.getSku(), p.getProductName(), p.getUnit(), line.quantity,
                    line.available});
            }
        }
        updateTotals();
    }

    private void updateTotals() {
        int units = 0;
        BigDecimal amount = BigDecimal.ZERO;
        for (Line line : lines) {
            units += line.quantity;
            if (line.unitCost != null) {
                amount = amount.add(line.unitCost.multiply(BigDecimal.valueOf(line.quantity)));
            }
        }
        String text = lines.size() + " item(s)     Total units: " + Formats.number(units);
        if (withUnitCost) {
            text += "     Total cost: " + Formats.money(amount);
        }
        lblTotals.setText(text);
    }

    protected int getTotalUnits() {
        int units = 0;
        for (Line line : lines) {
            units += line.quantity;
        }
        return units;
    }

    // ------------------------------------------------------------------ save

    private void saveClicked() {
        if (!validateHeader()) {
            return;
        }
        if (dateField.isEmpty()) {
            Dialogs.invalid(this, "Date is required.", dateField);
            return;
        }
        LocalDate date = dateField.getDate();
        if (date == null) {
            Dialogs.invalid(this, "Date must be a real date in the yyyy-MM-dd format (example: "
                    + LocalDate.now() + ").", dateField);
            return;
        }
        if (Validator.isFutureDate(date)) {
            Dialogs.invalid(this, "The date cannot be in the future.", dateField);
            return;
        }
        if (lines.isEmpty()) {
            Dialogs.invalid(this, "Add at least one product to the item list.", cboProduct);
            return;
        }
        int itemCount = lines.size();
        int units = getTotalUnits();
        try {
            String reference = saveDocument(date, Validator.clean(txtRemarks.getText()));
            Dialogs.success(this, documentName + " " + reference + " was saved.\n\n"
                    + itemCount + " product(s), " + Formats.number(units) + " units.\n"
                    + "The warehouse stock has been updated.");
            resetDocument();
        } catch (BusinessRuleException e) {
            ErrorHandler.showRuleViolation(this, e);
            productChanged();
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "save the " + documentName.toLowerCase());
        }
    }

    /** Empties the form for the next document. */
    protected void resetDocument() {
        lines.clear();
        refreshItemsTable();
        runQuietly(() -> {
            clearHeaderFields();
            cboProduct.setSelectedItem(null);
        });
        lastSourceWarehouse = getSourceWarehouse();
        dateField.setDate(LocalDate.now());
        txtRemarks.setText("");
        txtQuantity.setText("");
        txtUnitCost.setText("");
        lblProductInfo.setText(" ");
    }

    // ------------------------------------------------------------------ history

    private void loadHistory() {
        LocalDate from = null;
        LocalDate to = null;
        if (!historyFrom.isEmpty()) {
            from = historyFrom.getDate();
            if (from == null) {
                Dialogs.invalid(this, "The From date must be in the yyyy-MM-dd format.", historyFrom);
                return;
            }
        }
        if (!historyTo.isEmpty()) {
            to = historyTo.getDate();
            if (to == null) {
                Dialogs.invalid(this, "The To date must be in the yyyy-MM-dd format.", historyTo);
                return;
            }
        }
        if (from != null && to != null && from.isAfter(to)) {
            Dialogs.invalid(this, "The From date must be before the To date.", historyFrom);
            return;
        }
        String status = cboHistoryStatus.getSelectedIndex() == 0 ? null : (String) cboHistoryStatus.getSelectedItem();
        try {
            List<Object[]> rows = fetchHistory(txtHistorySearch.getText().trim(), from, to, status);
            TableUtil.setRows(historyModel, rows);
            lblHistoryCount.setText(rows.size() + " document(s) found");
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the " + documentName.toLowerCase() + " history");
        }
        updateHistoryButtons();
    }

    private void updateHistoryButtons() {
        int row = TableUtil.getSelectedModelRow(historyTable);
        btnViewDetails.setEnabled(row >= 0);
        boolean posted = row >= 0 && Status.POSTED.equals(historyModel.getValueAt(row, historyModel.getColumnCount() - 1));
        btnVoid.setEnabled(posted && Session.canVoid());
    }

    private void viewDetailsClicked() {
        int row = TableUtil.getSelectedModelRow(historyTable);
        if (row < 0) {
            return;
        }
        try {
            showDetails((Integer) historyModel.getValueAt(row, 0));
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the document details");
        }
    }

    private void voidClicked() {
        int row = TableUtil.getSelectedModelRow(historyTable);
        if (row < 0 || !Session.canVoid()) {
            return;
        }
        int id = (Integer) historyModel.getValueAt(row, 0);
        String reference = (String) historyModel.getValueAt(row, 1);
        if (!Dialogs.confirm(this, "Void " + documentName.toLowerCase() + " " + reference + "?\n\n"
                + describeVoid() + "\nThe document is kept with the status VOIDED.\nThis cannot be undone.",
                "Confirm Void")) {
            return;
        }
        try {
            voidDocument(id);
            Dialogs.success(this, reference + " was voided and the stock was reversed.");
            loadHistory();
        } catch (BusinessRuleException e) {
            ErrorHandler.showRuleViolation(this, e);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "void the " + documentName.toLowerCase());
        }
    }

    protected void setHistoryColumnWidths(int... widths) {
        TableUtil.setColumnWidths(historyTable, widths);
    }

    // ------------------------------------------------------------------ header layout helper

    /** Two label/field pairs per row, built with GridBagLayout. */
    protected static final class HeaderGrid extends JPanel {
        private static final long serialVersionUID = 1L;
        private int index;

        HeaderGrid() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        public void add(String label, JComponent field, boolean required) {
            FormGrid.keepNarrow(field);
            int row = index / 2;
            int column = (index % 2) * 2;
            GridBagConstraints lc = new GridBagConstraints();
            lc.gridx = column;
            lc.gridy = row;
            lc.anchor = GridBagConstraints.WEST;
            lc.insets = new Insets(5, column == 0 ? 0 : 24, 5, 10);
            add(fieldLabel(label, required), lc);
            GridBagConstraints fc = new GridBagConstraints();
            fc.gridx = column + 1;
            fc.gridy = row;
            fc.weightx = 1;
            fc.fill = GridBagConstraints.HORIZONTAL;
            fc.insets = new Insets(5, 0, 5, 0);
            add(field, fc);
            index++;
        }

        /** A field that uses the whole row (e.g. Remarks). */
        public void addWide(String label, JComponent field) {
            if (index % 2 == 1) {
                index++;
            }
            int row = index / 2;
            GridBagConstraints lc = new GridBagConstraints();
            lc.gridx = 0;
            lc.gridy = row;
            lc.anchor = GridBagConstraints.NORTHWEST;
            lc.insets = new Insets(8, 0, 5, 10);
            add(fieldLabel(label, false), lc);
            GridBagConstraints fc = new GridBagConstraints();
            fc.gridx = 1;
            fc.gridy = row;
            fc.gridwidth = 3;
            fc.weightx = 1;
            fc.fill = GridBagConstraints.HORIZONTAL;
            fc.insets = new Insets(5, 0, 5, 0);
            add(field, fc);
            index += 2;
        }

        private static JLabel fieldLabel(String text, boolean required) {
            return FormGrid.createLabel(text, required);
        }
    }
}
