package ui;

import dao.CategoryDAO;
import dao.ProductDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.Category;
import model.Product;
import model.Status;
import ui.components.FormGrid;
import util.ErrorHandler;
import util.Formats;
import util.LengthFilter;
import util.NumericFilter;
import util.TableUtil;
import util.Validator;

/**
 * Product management. The table joins PRODUCTS with CATEGORIES (category
 * name) and WAREHOUSE_STOCK (total stock in all warehouses).
 */
public class ProductPanel extends CrudPanel<Product> {

    private static final long serialVersionUID = 1L;
    private static final String[] UNITS = {"pcs", "box", "pack", "case", "set", "ream", "roll", "bottle",
        "gallon", "sack", "kg", "liter", "meter"};

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    private final JTextField txtSku = new JTextField();
    private final JTextField txtName = new JTextField();
    private final JComboBox<Category> cboCategory = new JComboBox<>();
    private final JComboBox<String> cboUnit = new JComboBox<>(UNITS);
    private final JTextField txtUnitCost = new JTextField();
    private final JTextField txtReorderLevel = new JTextField();
    private final JRadioButton rbActive = new JRadioButton("Active");
    private final JRadioButton rbInactive = new JRadioButton("Inactive");
    private final JTextArea txtDescription = new JTextArea(3, 20);
    private final JLabel lblStockInfo = UITheme.mutedLabel(" ");

    private final JComboBox<Category> cboFilterCategory = new JComboBox<>();
    private final JComboBox<String> cboFilterStatus = new JComboBox<>(new String[]{"All statuses", Status.ACTIVE, Status.INACTIVE});

    public ProductPanel() {
        super("Products", "Items stored in the warehouses. Stock is tracked per warehouse.",
                "Product", new String[]{"ID", "SKU", "Product Name", "Category", "Unit", "Unit Cost",
                    "Reorder Lvl", "Total Stock", "Status"},
                new Class<?>[]{Integer.class, String.class, String.class, String.class, String.class,
                    BigDecimal.class, Integer.class, Integer.class, String.class});
        LengthFilter.code(txtSku, 30);
        LengthFilter.apply(txtName, 100);
        NumericFilter.decimal(txtUnitCost, 8, 2);
        NumericFilter.integer(txtReorderLevel, 7);
        LengthFilter.apply(txtDescription, 255);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtSku.setToolTipText("Unique product code, e.g. ELE-1001 (letters, digits and dashes)");
        cboCategory.setRenderer(UITheme.placeholderRenderer("Select a category"));
        cboFilterCategory.setRenderer(UITheme.placeholderRenderer("All categories"));
        buildUI();
        TableUtil.setColumnWidths(table, 40, 78, 210, 130, 56, 90, 80, 82, 76);
    }

    @Override
    protected void buildForm(FormGrid form) {
        form.addField("SKU / Code", txtSku, true);
        form.addField("Product Name", txtName, true);
        form.addField("Category", cboCategory, true);
        form.addField("Unit", cboUnit, true);
        form.addField("Unit Cost (" + Formats.PESO + ")", txtUnitCost, true);
        form.addField("Reorder Level", txtReorderLevel, true);
        JPanel status = statusPanel(rbActive, rbInactive);
        form.addField("Status", status, true);
        form.addField("Description", UITheme.scroll(txtDescription));
        form.addWide(lblStockInfo);
    }

    @Override
    protected void addFilters(JPanel filterBar) {
        filterBar.add(cboFilterCategory);
        filterBar.add(cboFilterStatus);
        cboFilterCategory.addActionListener(e -> loadTable());
        cboFilterStatus.addActionListener(e -> loadTable());
    }

    @Override
    protected void resetFilters() {
        cboFilterCategory.setSelectedItem(null);
        cboFilterStatus.setSelectedIndex(0);
    }

    @Override
    protected void reloadLookups() {
        try {
            List<Category> categories = categoryDAO.getCategories();
            cboCategory.setModel(new DefaultComboBoxModel<>(categories.toArray(new Category[0])));
            cboCategory.setSelectedItem(null);
            DefaultComboBoxModel<Category> filterModel = new DefaultComboBoxModel<>(categories.toArray(new Category[0]));
            filterModel.insertElementAt(null, 0);
            cboFilterCategory.setModel(filterModel);
            cboFilterCategory.setSelectedItem(null);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the categories");
        }
    }

    @Override
    protected List<Product> fetchRecords(String keyword) throws SQLException {
        Category category = (Category) cboFilterCategory.getSelectedItem();
        String status = cboFilterStatus.getSelectedIndex() == 0 ? null : (String) cboFilterStatus.getSelectedItem();
        return productDAO.searchProducts(keyword, category == null ? 0 : category.getCategoryId(), status);
    }

    @Override
    protected Object[] toRow(Product p) {
        return new Object[]{p.getProductId(), p.getSku(), p.getProductName(), p.getCategoryName(), p.getUnit(),
            p.getUnitCost(), p.getReorderLevel(), p.getTotalStock(), p.getStatus()};
    }

    @Override
    protected int getId(Product p) {
        return p.getProductId();
    }

    @Override
    protected void fillForm(Product p) {
        txtSku.setText(p.getSku());
        txtName.setText(p.getProductName());
        selectCategory(p.getCategoryId());
        cboUnit.setSelectedItem(p.getUnit());
        txtUnitCost.setText(p.getUnitCost().toPlainString());
        txtReorderLevel.setText(String.valueOf(p.getReorderLevel()));
        rbActive.setSelected(Status.ACTIVE.equals(p.getStatus()));
        rbInactive.setSelected(!Status.ACTIVE.equals(p.getStatus()));
        txtDescription.setText(p.getDescription() == null ? "" : p.getDescription());
        lblStockInfo.setText("Total stock in all warehouses: " + Formats.number(p.getTotalStock()) + " "
                + p.getUnit() + (p.isLowStock() ? "   (at or below the reorder level)" : ""));
        lblStockInfo.setForeground(p.isLowStock() ? UITheme.WARNING : UITheme.TEXT_MUTED);
    }

    private void selectCategory(int categoryId) {
        for (int i = 0; i < cboCategory.getItemCount(); i++) {
            if (cboCategory.getItemAt(i).getCategoryId() == categoryId) {
                cboCategory.setSelectedIndex(i);
                return;
            }
        }
        cboCategory.setSelectedItem(null);
    }

    @Override
    protected void clearFields() {
        txtSku.setText("");
        txtName.setText("");
        cboCategory.setSelectedItem(null);
        cboUnit.setSelectedIndex(0);
        txtUnitCost.setText("");
        txtReorderLevel.setText("10");
        rbActive.setSelected(true);
        txtDescription.setText("");
        lblStockInfo.setText(" ");
    }

    @Override
    protected void setFieldsEditable(boolean editable) {
        txtSku.setEditable(editable);
        txtName.setEditable(editable);
        cboCategory.setEnabled(editable);
        cboUnit.setEnabled(editable);
        txtUnitCost.setEditable(editable);
        txtReorderLevel.setEditable(editable);
        rbActive.setEnabled(editable);
        rbInactive.setEnabled(editable);
        txtDescription.setEditable(editable);
    }

    @Override
    protected void focusFirstField() {
        txtSku.requestFocusInWindow();
    }

    /** Validates every field; returns the product or null when something is wrong. */
    private Product readForm(int productId) throws SQLException {
        if (Validator.isEmpty(txtSku.getText())) {
            Dialogs.invalid(this, "SKU / Code is required.", txtSku);
            return null;
        }
        if (!Validator.isValidCode(txtSku.getText())) {
            Dialogs.invalid(this, "SKU must be 2-30 letters, digits or dashes (example: ELE-1001).", txtSku);
            return null;
        }
        if (productDAO.isSkuTaken(text(txtSku), productId)) {
            Dialogs.invalid(this, "The SKU " + text(txtSku).toUpperCase() + " is already used by another product.", txtSku);
            return null;
        }
        if (Validator.isEmpty(txtName.getText())) {
            Dialogs.invalid(this, "Product name is required.", txtName);
            return null;
        }
        Category category = (Category) cboCategory.getSelectedItem();
        if (category == null) {
            Dialogs.invalid(this, "Please select a category.", cboCategory);
            return null;
        }
        BigDecimal cost = Validator.parseAmount(txtUnitCost.getText());
        if (cost == null) {
            Dialogs.invalid(this, "Unit cost must be a valid amount (0 or more, up to 2 decimals).", txtUnitCost);
            return null;
        }
        Integer reorder = Validator.parseInteger(txtReorderLevel.getText());
        if (reorder == null || reorder < 0) {
            Dialogs.invalid(this, "Reorder level must be a whole number (0 or more).", txtReorderLevel);
            return null;
        }
        Product product = new Product(productId, text(txtSku).toUpperCase(), text(txtName),
                category.getCategoryId(), (String) cboUnit.getSelectedItem(), cost, reorder);
        product.setStatus(statusOf(rbActive));
        product.setDescription(Validator.clean(txtDescription.getText()));
        return product;
    }

    @Override
    protected void saveRecord() {
        try {
            Product product = readForm(0);
            if (product != null) {
                int id = productDAO.addProduct(product);
                afterChange("Product " + product.getSku() + " - " + product.getProductName()
                        + " was saved (ID " + id + ").");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "save the product");
        }
    }

    @Override
    protected void updateRecord() {
        try {
            Product product = readForm(selected.getProductId());
            if (product == null) {
                return;
            }
            if (productDAO.updateProduct(product)) {
                afterChange("Product " + product.getSku() + " was updated.");
            } else {
                Dialogs.warning(this, "This product no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "update the product");
        }
    }

    @Override
    protected void deleteRecord() {
        try {
            int lines = productDAO.countTransactionLines(selected.getProductId());
            if (lines > 0 || selected.getTotalStock() > 0) {
                Dialogs.warning(this, selected.getSku() + " cannot be deleted because it already appears in "
                        + lines + " transaction line(s).\n\n"
                        + "Tip: set its status to INACTIVE instead. Inactive products are kept for\n"
                        + "history but cannot be used in new transactions.");
                return;
            }
            if (!Dialogs.confirmDelete(this, "Product: " + selected.getSku() + " - " + selected.getProductName())) {
                return;
            }
            if (productDAO.deleteProduct(selected.getProductId())) {
                afterChange("The product was deleted.");
            } else {
                Dialogs.warning(this, "This product no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "delete the product");
        }
    }
}
