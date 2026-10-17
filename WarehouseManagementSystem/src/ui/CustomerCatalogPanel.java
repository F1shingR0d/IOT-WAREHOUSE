package ui;

import dao.CategoryDAO;
import dao.StockDAO;
import dao.WarehouseDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
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
import util.TableUtil;

/**
 * Customer Portal catalog: the active products of one warehouse with their
 * availability (IN STOCK / LOW STOCK / OUT OF STOCK). Exact quantities are
 * not shown to customers.
 */
public class CustomerCatalogPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private static final String ALL_CATEGORIES = "All categories";

    private final CustomerDashboardFrame frame;
    private final StockDAO stockDAO = new StockDAO();
    private final WarehouseDAO warehouseDAO = new WarehouseDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final JComboBox<Warehouse> cboWarehouse = new JComboBox<>();
    private final JComboBox<Object> cboCategory = new JComboBox<>();
    private final JTextField txtSearch = new JTextField(16);
    private final FlatButton btnSearch = new FlatButton("Search", UITheme.PRIMARY, ModuleIcon.Type.SEARCH);
    private final FlatButton btnShowAll = FlatButton.outline("Show All");
    private final FlatButton btnRequest = new FlatButton("Request Selected Item", UITheme.PRIMARY, ModuleIcon.Type.ADD);
    private final DefaultTableModel model = TableUtil.createModel(
            new String[]{"SKU", "Product", "Category", "Unit", "Description", "Availability"},
            new Class<?>[]{String.class, String.class, String.class, String.class, String.class, String.class});
    private final JTable table = TableUtil.createTable(model);
    private final JLabel lblCount = UITheme.mutedLabel(" ");
    private List<WarehouseStock> records = new ArrayList<>();
    private boolean quiet;

    public CustomerCatalogPanel(CustomerDashboardFrame frame) {
        super(new BorderLayout(0, 16));
        this.frame = frame;
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader("Product Catalog",
                "Browse the products of each warehouse and check whether they are in stock.", btnRequest),
                BorderLayout.NORTH);

        cboWarehouse.setRenderer(UITheme.placeholderRenderer("Select a warehouse"));
        txtSearch.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.SEARCH, 18, UITheme.TEXT_MUTED));
        UITheme.setPlaceholder(txtSearch, "Search products...");

        JPanel filters = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        filters.setOpaque(false);
        filters.add(UITheme.accentLabel("Warehouse:"));
        filters.add(cboWarehouse);
        filters.add(UITheme.accentLabel("Category:"));
        filters.add(cboCategory);
        filters.add(txtSearch);
        filters.add(btnSearch);
        filters.add(btnShowAll);

        TableUtil.setColumnWidths(table, 90, 230, 130, 55, 300, 135);
        JLabel legend = UITheme.mutedLabel("<html><b style='color:" + UITheme.readableHex(UITheme.SUCCESS)
                + "'>IN STOCK</b> available now &nbsp;&nbsp; "
                + "<b style='color:" + UITheme.readableHex(UITheme.WARNING) + "'>LOW STOCK</b> limited quantity &nbsp;&nbsp; "
                + "<b style='color:" + UITheme.readableHex(UITheme.DANGER)
                + "'>OUT OF STOCK</b> not available at the moment &nbsp;&nbsp; "
                + "(double-click a product to request it)</html>");
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(false);
        bottom.add(legend, BorderLayout.NORTH);
        bottom.add(lblCount, BorderLayout.SOUTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.add(filters, BorderLayout.NORTH);
        card.add(UITheme.scroll(table), BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        cboWarehouse.addActionListener(e -> {
            if (!quiet) {
                loadTable();
            }
        });
        cboCategory.addActionListener(e -> {
            if (!quiet) {
                loadTable();
            }
        });
        btnSearch.addActionListener(e -> loadTable());
        txtSearch.addActionListener(e -> loadTable());
        btnShowAll.addActionListener(e -> {
            txtSearch.setText("");
            quiet = true;
            cboCategory.setSelectedIndex(0);
            quiet = false;
            loadTable();
        });
        btnRequest.addActionListener(e -> requestSelected());
        table.getSelectionModel().addListSelectionListener(e -> btnRequest.setEnabled(table.getSelectedRow() >= 0));
        table.addMouseListener(new MouseAdapter() {   // double-click requests the product
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    requestSelected();
                }
            }
        });
        btnRequest.setEnabled(false);
    }

    @Override
    public void refreshData() {
        quiet = true;
        try {
            Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
            cboWarehouse.setModel(new DefaultComboBoxModel<>(warehouseDAO.getActiveWarehouses().toArray(new Warehouse[0])));
            TransactionPanel.reselect(cboWarehouse, warehouse);
            if (cboWarehouse.getSelectedItem() == null && cboWarehouse.getItemCount() > 0) {
                cboWarehouse.setSelectedIndex(0);
            }
            Object category = cboCategory.getSelectedItem();
            DefaultComboBoxModel<Object> categories = new DefaultComboBoxModel<>();
            categories.addElement(ALL_CATEGORIES);
            for (Category c : categoryDAO.getCategories()) {
                categories.addElement(c);
            }
            cboCategory.setModel(categories);
            TransactionPanel.reselect(cboCategory, category);
            if (cboCategory.getSelectedItem() == null) {
                cboCategory.setSelectedIndex(0);
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the warehouses and categories");
        } finally {
            quiet = false;
        }
        loadTable();
    }

    private void loadTable() {
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        if (warehouse == null) {
            model.setRowCount(0);
            records = new ArrayList<>();
            lblCount.setText("No warehouse is available.");
            return;
        }
        Object category = cboCategory.getSelectedItem();
        int categoryId = category instanceof Category ? ((Category) category).getCategoryId() : 0;
        try {
            records = stockDAO.getCatalog(warehouse.getWarehouseId(), categoryId, txtSearch.getText().trim());
            List<Object[]> rows = new ArrayList<>();
            int inStock = 0;
            for (WarehouseStock s : records) {
                rows.add(new Object[]{s.getSku(), s.getProductName(), s.getCategoryName(), s.getUnit(),
                    s.getDescription(), s.getAvailability()});
                if (!WarehouseStock.OUT_OF_STOCK.equals(s.getAvailability())) {
                    inStock++;
                }
            }
            TableUtil.setRows(model, rows);
            lblCount.setText(records.size() + " product(s) in " + warehouse.getWarehouseName() + ", "
                    + inStock + " available now");
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the catalog");
        }
        btnRequest.setEnabled(false);
    }

    private void requestSelected() {
        int row = TableUtil.getSelectedModelRow(table);
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        if (row < 0 || row >= records.size() || warehouse == null) {
            Dialogs.info(this, "Select a product in the list first.");
            return;
        }
        frame.requestProduct(warehouse, records.get(row).getProductId());
    }
}
