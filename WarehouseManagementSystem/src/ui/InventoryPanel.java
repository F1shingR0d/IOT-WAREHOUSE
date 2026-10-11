package ui;

import dao.CategoryDAO;
import dao.StockDAO;
import dao.WarehouseDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Category;
import model.Warehouse;
import model.WarehouseStock;
import ui.components.FlatButton;
import ui.components.InputBorder;
import ui.components.ModuleIcon;
import ui.components.RoundedPanel;
import ui.components.WrapLayout;
import util.ErrorHandler;
import util.Formats;
import util.TableUtil;

/**
 * Stock on hand of every product in every warehouse (read-only for all roles).
 * Rows are marked OK, LOW (at or below the reorder level) or OUT (zero).
 */
public class InventoryPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final StockDAO stockDAO = new StockDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    private final JComboBox<Warehouse> cboWarehouse = new JComboBox<>();
    private final JComboBox<Category> cboCategory = new JComboBox<>();
    private final JTextField txtSearch = new JTextField(16);
    private final JCheckBox chkLowOnly = new JCheckBox("Show low / out of stock only");
    private final FlatButton btnSearch = new FlatButton("Search", UITheme.PRIMARY, ModuleIcon.Type.SEARCH);
    private final FlatButton btnReset = FlatButton.outline("Reset", ModuleIcon.Type.CLEAR);
    private final DefaultTableModel model = TableUtil.createModel(
            new String[]{"Warehouse", "SKU", "Product", "Category", "Unit", "On Hand", "Reorder Lvl", "Unit Cost",
                "Stock Value", "Status", "Last Updated"},
            new Class<?>[]{String.class, String.class, String.class, String.class, String.class, Integer.class,
                Integer.class, BigDecimal.class, BigDecimal.class, String.class, String.class});
    private final JTable table = TableUtil.createTable(model);
    private final JLabel lblSummary = new JLabel(" ");
    private boolean loadingFilters;

    public InventoryPanel() {
        super(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader("Inventory", "Stock on hand per warehouse. Updated automatically by every "
                + "receiving, release and transfer."), BorderLayout.NORTH);

        cboWarehouse.setRenderer(UITheme.placeholderRenderer("All warehouses"));
        cboCategory.setRenderer(UITheme.placeholderRenderer("All categories"));
        cboWarehouse.setPreferredSize(new java.awt.Dimension(210, cboWarehouse.getPreferredSize().height));
        cboCategory.setPreferredSize(new java.awt.Dimension(170, cboCategory.getPreferredSize().height));
        chkLowOnly.setOpaque(false);

        JPanel filters = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        filters.setOpaque(false);
        filters.add(UITheme.accentLabel("Warehouse:"));
        filters.add(cboWarehouse);
        filters.add(UITheme.accentLabel("Category:"));
        filters.add(cboCategory);
        filters.add(UITheme.accentLabel("Product:"));
        txtSearch.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.SEARCH, 18, UITheme.TEXT_MUTED));
        UITheme.setPlaceholder(txtSearch, "SKU or product name...");
        filters.add(txtSearch);
        filters.add(chkLowOnly);
        filters.add(btnSearch);
        filters.add(btnReset);

        lblSummary.setFont(UITheme.FONT_BOLD);
        TableUtil.setColumnWidths(table, 170, 80, 220, 130, 55, 75, 90, 95, 110, 60, 140);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.add(filters, BorderLayout.NORTH);
        card.add(UITheme.scroll(table), BorderLayout.CENTER);
        card.add(lblSummary, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        cboWarehouse.addActionListener(e -> filterChanged());
        cboCategory.addActionListener(e -> filterChanged());
        chkLowOnly.addItemListener(e -> loadStock());      // ItemListener reacts to the check box
        txtSearch.addActionListener(e -> loadStock());
        btnSearch.addActionListener(e -> loadStock());
        btnReset.addActionListener(e -> {
            loadingFilters = true;
            cboWarehouse.setSelectedItem(null);
            cboCategory.setSelectedItem(null);
            txtSearch.setText("");
            chkLowOnly.setSelected(false);
            loadingFilters = false;
            loadStock();
        });
    }

    private void filterChanged() {
        if (!loadingFilters) {
            loadStock();
        }
    }

    @Override
    public void refreshData() {
        loadingFilters = true;
        try {
            Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
            Category category = (Category) cboCategory.getSelectedItem();
            DefaultComboBoxModel<Warehouse> warehouses = new DefaultComboBoxModel<>(
                    warehouseDAO.getWarehouses().toArray(new Warehouse[0]));
            warehouses.insertElementAt(null, 0);
            cboWarehouse.setModel(warehouses);
            DefaultComboBoxModel<Category> categories = new DefaultComboBoxModel<>(
                    categoryDAO.getCategories().toArray(new Category[0]));
            categories.insertElementAt(null, 0);
            cboCategory.setModel(categories);
            TransactionPanel.reselect(cboWarehouse, warehouse);
            TransactionPanel.reselect(cboCategory, category);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the filters");
        } finally {
            loadingFilters = false;
        }
        loadStock();
    }

    private void loadStock() {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        Category category = (Category) cboCategory.getSelectedItem();
        try {
            List<WarehouseStock> stock = stockDAO.getStockOnHand(
                    warehouse == null ? 0 : warehouse.getWarehouseId(),
                    category == null ? 0 : category.getCategoryId(),
                    txtSearch.getText().trim(), chkLowOnly.isSelected());
            model.setRowCount(0);
            long units = 0;
            BigDecimal value = BigDecimal.ZERO;
            int alerts = 0;
            for (WarehouseStock s : stock) {
                model.addRow(new Object[]{s.getWarehouseName(), s.getSku(), s.getProductName(), s.getCategoryName(),
                    s.getUnit(), s.getQuantity(), s.getReorderLevel(), s.getUnitCost(), s.getStockValue(),
                    s.getStockStatus(), Formats.dateTime(s.getLastUpdated())});
                units += s.getQuantity();
                value = value.add(s.getStockValue());
                if (!WarehouseStock.STATUS_OK.equals(s.getStockStatus())) {
                    alerts++;
                }
            }
            lblSummary.setText(stock.size() + " stock line(s)     Units on hand: " + Formats.number(units)
                    + "     Stock value: " + Formats.money(value) + "     LOW / OUT lines: " + alerts);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the inventory");
        }
    }
}
