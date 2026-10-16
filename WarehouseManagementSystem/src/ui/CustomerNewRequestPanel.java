package ui;

import dao.ProductDAO;
import dao.StockDAO;
import dao.StockRequestDAO;
import dao.WarehouseDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Product;
import model.StockRequest;
import model.StockRequestItem;
import model.Warehouse;
import model.WarehouseStock;
import ui.components.DateField;
import ui.components.FlatButton;
import ui.components.ModuleIcon;
import ui.components.RoundedPanel;
import util.BusinessRuleException;
import util.ErrorHandler;
import util.Formats;
import util.LengthFilter;
import util.NumericFilter;
import util.Session;
import util.TableUtil;
import util.Validator;

/**
 * Customer Portal: a new stock request. The customer picks a warehouse,
 * adds products with quantities and submits. Nothing is deducted from the
 * stock until the warehouse releases the request.
 */
public class CustomerNewRequestPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_QUANTITY = 1_000_000;

    /** One product line of the request being entered. */
    private static final class Line {
        final Product product;
        int quantity;
        int available;

        Line(Product product, int quantity, int available) {
            this.product = product;
            this.quantity = quantity;
            this.available = available;
        }

        String availability() {
            return WarehouseStock.availabilityOf(available, product.getReorderLevel(), quantity);
        }
    }

    private final StockRequestDAO requestDAO = new StockRequestDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();

    private final JComboBox<Warehouse> cboWarehouse = new JComboBox<>();
    private final DateField neededBy = new DateField(null);
    private final JTextArea txtRemarks = new JTextArea(2, 20);
    private final JComboBox<Product> cboProduct = new JComboBox<>();
    private final JTextField txtQuantity = new JTextField(7);
    private final JLabel lblProductInfo = UITheme.mutedLabel(" ");
    private final FlatButton btnAddItem = new FlatButton("Add Item", UITheme.PRIMARY, ModuleIcon.Type.ADD);
    private final FlatButton btnRemoveItem = FlatButton.outline("Remove Selected Item", ModuleIcon.Type.REMOVE)
            .withAccent(UITheme.DANGER);
    private final FlatButton btnSubmit = new FlatButton("Submit Request", UITheme.PRIMARY, ModuleIcon.Type.SAVE);
    private final FlatButton btnClear = FlatButton.outline("Clear", ModuleIcon.Type.CLEAR);
    private final DefaultTableModel itemsModel = TableUtil.createModel(
            new String[]{"#", "SKU", "Product", "Unit", "Quantity", "Availability"},
            new Class<?>[]{Integer.class, String.class, String.class, String.class, Integer.class, String.class});
    private final JTable itemsTable = TableUtil.createTable(itemsModel);
    private final JLabel lblTotals = new JLabel(" ");
    private final List<Line> lines = new ArrayList<>();
    private boolean quiet;

    public CustomerNewRequestPanel() {
        super(new BorderLayout(0, 12));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader("New Stock Request",
                "Choose a warehouse and the products you need. The warehouse reviews every request before "
                + "releasing the stock."), BorderLayout.NORTH);

        // ---------- request header card
        cboWarehouse.setRenderer(UITheme.placeholderRenderer("Select the warehouse to request from"));
        LengthFilter.apply(txtRemarks, 255);
        txtRemarks.setLineWrap(true);
        txtRemarks.setWrapStyleWord(true);
        TransactionPanel.HeaderGrid grid = new TransactionPanel.HeaderGrid();
        grid.add("Reference No.", UITheme.mutedLabel("Assigned automatically when you submit"), false);
        grid.add("Request Date", UITheme.label(Formats.date(LocalDate.now())), false);
        grid.add("Warehouse", cboWarehouse, true);
        grid.add("Needed By (optional)", neededBy, false);
        grid.addWide("Remarks", UITheme.scroll(txtRemarks));

        RoundedPanel headerCard = new RoundedPanel(new BorderLayout(0, 8));
        headerCard.add(UITheme.headingLabel("Request Details"), BorderLayout.NORTH);
        headerCard.add(grid, BorderLayout.CENTER);

        // ---------- item entry row
        NumericFilter.integer(txtQuantity, 7);
        cboProduct.setRenderer(UITheme.placeholderRenderer("Select a product"));
        cboProduct.setMaximumRowCount(14);
        JPanel entry = new JPanel(new GridBagLayout());
        entry.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 8);
        c.anchor = GridBagConstraints.WEST;
        entry.add(UITheme.label("Product"), c);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        entry.add(cboProduct, c);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        entry.add(UITheme.label("Quantity"), c);
        entry.add(txtQuantity, c);
        c.insets = new Insets(0, 0, 0, 0);
        entry.add(btnAddItem, c);

        JPanel entryBlock = new JPanel(new BorderLayout(0, 6));
        entryBlock.setOpaque(false);
        entryBlock.add(UITheme.headingLabel("Items"), BorderLayout.NORTH);
        entryBlock.add(entry, BorderLayout.CENTER);
        entryBlock.add(lblProductInfo, BorderLayout.SOUTH);

        // ---------- items table and footer
        itemsTable.setAutoCreateRowSorter(false);
        itemsTable.setRowSorter(null);   // row numbers must match the order of the lines
        TableUtil.setColumnWidths(itemsTable, 40, 90, 320, 70, 90, 135);
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
        saveButtons.add(btnSubmit);
        saveButtons.add(btnClear);
        footer.add(saveButtons, BorderLayout.EAST);

        RoundedPanel itemsCard = new RoundedPanel(new BorderLayout(0, 10));
        itemsCard.add(entryBlock, BorderLayout.NORTH);
        itemsCard.add(UITheme.scroll(itemsTable), BorderLayout.CENTER);
        itemsCard.add(footer, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(headerCard, BorderLayout.NORTH);
        body.add(itemsCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        // ---------- events
        cboWarehouse.addActionListener(e -> {
            if (!quiet) {
                warehouseChanged();
            }
        });
        cboProduct.addActionListener(e -> {
            if (!quiet) {
                productChanged();
            }
        });
        txtQuantity.addActionListener(e -> addItem());      // Enter adds the item
        btnAddItem.addActionListener(e -> addItem());
        btnRemoveItem.addActionListener(e -> removeSelectedItem());
        itemsTable.addKeyListener(new KeyAdapter() {       // Delete key removes the selected line
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedItem();
                }
            }
        });
        btnSubmit.addActionListener(e -> submit());
        btnClear.addActionListener(e -> {
            if (lines.isEmpty() || Dialogs.confirm(this, "Discard the items of this request?", "Clear Form")) {
                resetForm();
            }
        });
        updateTotals();
    }

    // ------------------------------------------------------------------ data loading

    /** Reloads the warehouses and products but keeps what the customer already entered. */
    @Override
    public void refreshData() {
        quiet = true;
        try {
            Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
            Product product = (Product) cboProduct.getSelectedItem();
            cboWarehouse.setModel(new DefaultComboBoxModel<>(warehouseDAO.getActiveWarehouses().toArray(new Warehouse[0])));
            cboProduct.setModel(new DefaultComboBoxModel<>(productDAO.getActiveProducts().toArray(new Product[0])));
            TransactionPanel.reselect(cboWarehouse, warehouse);
            TransactionPanel.reselect(cboProduct, product);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the lists");
        } finally {
            quiet = false;
        }
        warehouseChanged();
    }

    /** Called from the catalog: selects the warehouse and the product, ready for the quantity. */
    public void preselect(Warehouse warehouse, int productId) {
        quiet = true;
        try {
            TransactionPanel.reselect(cboWarehouse, warehouse);
            for (int i = 0; i < cboProduct.getItemCount(); i++) {
                if (cboProduct.getItemAt(i).getProductId() == productId) {
                    cboProduct.setSelectedIndex(i);
                }
            }
        } finally {
            quiet = false;
        }
        warehouseChanged();
        txtQuantity.requestFocusInWindow();
    }

    /** The availability of every line depends on the warehouse, so it is read again. */
    private void warehouseChanged() {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        for (Line line : lines) {
            line.available = availableIn(warehouse, line.product);
        }
        refreshItemsTable();
        productChanged();
    }

    private int availableIn(Warehouse warehouse, Product product) {
        if (warehouse == null) {
            return 0;
        }
        try {
            return stockDAO.getAvailable(warehouse.getWarehouseId(), product.getProductId());
        } catch (SQLException e) {
            ErrorHandler.log(e);
            return 0;
        }
    }

    /** Shows the unit and the availability (not the exact stock) of the chosen product. */
    private void productChanged() {
        Product product = (Product) cboProduct.getSelectedItem();
        if (product == null) {
            lblProductInfo.setText(" ");
            return;
        }
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        StringBuilder info = new StringBuilder("Unit: " + product.getUnit());
        if (warehouse == null) {
            info.append("     Select a warehouse to see if it is in stock.");
        } else {
            String availability = WarehouseStock.availabilityOf(availableIn(warehouse, product),
                    product.getReorderLevel(), 0);
            info.append("     Availability in ").append(warehouse.getWarehouseName()).append(": ").append(availability);
        }
        if (product.getDescription() != null) {
            info.append("     ").append(product.getDescription());
        }
        lblProductInfo.setText(info.toString());
    }

    // ------------------------------------------------------------------ item list

    private Line lineOf(Product product) {
        for (Line line : lines) {
            if (line.product.getProductId() == product.getProductId()) {
                return line;
            }
        }
        return null;
    }

    private void addItem() {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        if (warehouse == null) {
            Dialogs.invalid(this, "Please select the warehouse first.", cboWarehouse);
            return;
        }
        Product product = (Product) cboProduct.getSelectedItem();
        if (product == null) {
            Dialogs.invalid(this, "Please select a product.", cboProduct);
            return;
        }
        Integer quantity = Validator.parseInteger(txtQuantity.getText());
        if (quantity == null || quantity <= 0) {
            Dialogs.invalid(this, "Quantity must be a whole number greater than 0.", txtQuantity);
            return;
        }
        Line existing = lineOf(product);
        int newQuantity = quantity + (existing == null ? 0 : existing.quantity);
        if (newQuantity > MAX_QUANTITY) {
            Dialogs.invalid(this, "The quantity is too large (maximum " + Formats.number(MAX_QUANTITY) + ").",
                    txtQuantity);
            return;
        }
        int available = availableIn(warehouse, product);
        if (newQuantity > available && !Dialogs.confirm(this, product.getProductName() + " is "
                + (available <= 0 ? "OUT OF STOCK" : "not fully in stock") + " at " + warehouse.getWarehouseName()
                + " right now.\n\nYou can still request it: the warehouse may release it later\n"
                + "or contact you about the quantity. Add it to the request?", "Limited Stock")) {
            return;
        }
        if (existing != null) {
            existing.quantity = newQuantity;   // same product twice = one line with the total
            existing.available = available;
        } else {
            lines.add(new Line(product, quantity, available));
        }
        refreshItemsTable();
        txtQuantity.setText("");
        quiet = true;
        cboProduct.setSelectedItem(null);
        quiet = false;
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
    }

    private void refreshItemsTable() {
        itemsModel.setRowCount(0);
        int number = 1;
        for (Line line : lines) {
            Product p = line.product;
            itemsModel.addRow(new Object[]{number++, p.getSku(), p.getProductName(), p.getUnit(), line.quantity,
                line.availability()});
        }
        updateTotals();
    }

    private void updateTotals() {
        int units = 0;
        for (Line line : lines) {
            units += line.quantity;
        }
        lblTotals.setText(lines.size() + " item(s)     Total units: " + Formats.number(units));
    }

    // ------------------------------------------------------------------ submit

    private void submit() {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        if (warehouse == null) {
            Dialogs.invalid(this, "Please select the warehouse you are requesting from.", cboWarehouse);
            return;
        }
        LocalDate needed = null;
        if (!neededBy.isEmpty()) {
            needed = neededBy.getDate();
            if (needed == null) {
                Dialogs.invalid(this, "\"Needed by\" must be a real date in the yyyy-MM-dd format (example: "
                        + LocalDate.now().plusDays(7) + "), or leave it empty.", neededBy);
                return;
            }
            if (needed.isBefore(LocalDate.now())) {
                Dialogs.invalid(this, "\"Needed by\" cannot be in the past.", neededBy);
                return;
            }
        }
        if (lines.isEmpty()) {
            Dialogs.invalid(this, "Add at least one product to the item list.", cboProduct);
            return;
        }
        StockRequest request = new StockRequest(Session.getCustomerId(), warehouse.getWarehouseId());
        request.setRequestDate(LocalDate.now());
        request.setNeededBy(needed);
        request.setRemarks(Validator.clean(txtRemarks.getText()));
        int units = 0;
        for (Line line : lines) {
            Product p = line.product;
            request.addItem(new StockRequestItem(p.getProductId(), p.getSku(), p.getProductName(), p.getUnit(),
                    line.quantity));
            units += line.quantity;
        }
        try {
            requestDAO.submitRequest(request);
            Dialogs.success(this, "Request " + request.getReferenceNo() + " was sent to " + warehouse.getWarehouseName()
                    + ".\n\n" + lines.size() + " product(s), " + Formats.number(units) + " units.\n"
                    + "Its status is PENDING. You can follow it under My Requests.");
            resetForm();
        } catch (BusinessRuleException e) {
            ErrorHandler.showRuleViolation(this, e);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "submit the request");
        }
    }

    private void resetForm() {
        lines.clear();
        refreshItemsTable();
        quiet = true;
        cboProduct.setSelectedItem(null);
        quiet = false;
        neededBy.setDate(null);
        txtRemarks.setText("");
        txtQuantity.setText("");
        lblProductInfo.setText(" ");
    }
}
