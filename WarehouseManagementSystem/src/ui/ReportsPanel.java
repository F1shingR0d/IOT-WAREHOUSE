package ui;

import dao.ReportDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.ReportData;
import model.ReportType;
import model.Warehouse;
import ui.components.DateField;
import ui.components.FlatButton;
import ui.components.ModuleIcon;
import ui.components.RoundedPanel;
import ui.components.WrapLayout;
import util.ErrorHandler;
import util.Session;
import util.TableUtil;

/**
 * Reports module (managers and administrators): pick a report, set the
 * filters, then view, print or export the result.
 */
public class ReportsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final ReportDAO reportDAO = new ReportDAO();
    private final JComboBox<ReportType> cboReport = new JComboBox<>();
    private final DateField dateFrom = new DateField(LocalDate.now().minusDays(30));
    private final DateField dateTo = new DateField(LocalDate.now());
    private final JComboBox<Warehouse> cboWarehouse = new JComboBox<>();
    private final FlatButton btnGenerate = new FlatButton("Generate", UITheme.PRIMARY, ModuleIcon.Type.REPORTS);
    private final FlatButton btnPrint = FlatButton.outline("Print", ModuleIcon.Type.PRINT);
    private final FlatButton btnExport = FlatButton.outline("Export CSV", ModuleIcon.Type.EXPORT);
    private final JLabel lblDescription = UITheme.mutedLabel(" ");
    private final JLabel lblReportTitle = UITheme.headingLabel(" ");
    private final JLabel lblGenerated = UITheme.mutedLabel(" ");
    private final JLabel lblSummary = new JLabel(" ");
    private final JTable table;
    private DefaultTableModel model;
    private ReportData current;
    private boolean loading;

    public ReportsPanel() {
        super(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(20, 24, 20, 24));
        add(UITheme.pageHeader("Reports", "Inventory, transaction and master-data reports. "
                + "Results can be printed or exported to CSV (Excel)."), BorderLayout.NORTH);

        for (ReportType type : ReportType.values()) {
            if (!type.isAdminOnly() || Session.canManageUsers()) {
                cboReport.addItem(type);
            }
        }
        cboReport.setMaximumRowCount(12);
        cboWarehouse.setRenderer(UITheme.placeholderRenderer("All warehouses"));
        cboWarehouse.setPreferredSize(new java.awt.Dimension(210, cboWarehouse.getPreferredSize().height));

        JPanel filters = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        filters.setOpaque(false);
        filters.add(UITheme.accentLabel("Report:"));
        filters.add(cboReport);
        filters.add(UITheme.accentLabel("From:"));
        filters.add(dateFrom);
        filters.add(UITheme.accentLabel("To:"));
        filters.add(dateTo);
        filters.add(UITheme.accentLabel("Warehouse:"));
        filters.add(cboWarehouse);
        filters.add(btnGenerate);
        filters.add(btnPrint);
        filters.add(btnExport);

        RoundedPanel filterCard = new RoundedPanel(new BorderLayout(0, 6));
        filterCard.add(filters, BorderLayout.CENTER);
        filterCard.add(lblDescription, BorderLayout.SOUTH);

        model = TableUtil.createModel(new String[]{" "}, null);
        table = TableUtil.createTable(model);
        lblSummary.setFont(UITheme.FONT_BOLD);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(lblReportTitle, BorderLayout.WEST);
        titleRow.add(lblGenerated, BorderLayout.EAST);

        RoundedPanel resultCard = new RoundedPanel(new BorderLayout(0, 10));
        resultCard.add(titleRow, BorderLayout.NORTH);
        resultCard.add(UITheme.scroll(table), BorderLayout.CENTER);
        resultCard.add(lblSummary, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(filterCard, BorderLayout.NORTH);
        body.add(resultCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        cboReport.addActionListener(e -> {
            if (!loading) {
                updateFilters();
                generate();
            }
        });
        cboWarehouse.addActionListener(e -> {
            if (!loading) {
                generate();
            }
        });
        dateFrom.addChangeListener(this::generate);
        dateTo.addChangeListener(this::generate);
        btnGenerate.addActionListener(e -> generate());
        btnPrint.addActionListener(e -> {
            if (current != null) {
                TableUtil.print(this, table, current.getTitle() + describeFilters());
            }
        });
        btnExport.addActionListener(e -> {
            if (current != null) {
                TableUtil.exportToCsv(this, table, current.getTitle());
            }
        });
        updateFilters();
    }

    @Override
    public void refreshData() {
        loading = true;
        try {
            Warehouse selectedWarehouse = (Warehouse) cboWarehouse.getSelectedItem();
            DefaultComboBoxModel<Warehouse> warehouses =
                    new DefaultComboBoxModel<>(reportDAO.getWarehouses().toArray(new Warehouse[0]));
            warehouses.insertElementAt(null, 0);
            cboWarehouse.setModel(warehouses);
            TransactionPanel.reselect(cboWarehouse, selectedWarehouse);
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "load the warehouses");
        } finally {
            loading = false;
        }
        generate();
    }

    /** Enables only the filters that the selected report uses. */
    private void updateFilters() {
        ReportType type = (ReportType) cboReport.getSelectedItem();
        if (type == null) {
            return;
        }
        dateFrom.setEnabled(type.usesDateRange());
        dateTo.setEnabled(type.usesDateRange());
        cboWarehouse.setEnabled(type.usesWarehouse());
        lblDescription.setText(type.getDescription());
    }

    private void generate() {
        ReportType type = (ReportType) cboReport.getSelectedItem();
        if (type == null) {
            return;
        }
        LocalDate from = dateFrom.getDate();
        LocalDate to = dateTo.getDate();
        if (type.usesDateRange()) {
            if (from == null) {
                Dialogs.invalid(this, "The From date must be a valid yyyy-MM-dd date.", dateFrom);
                return;
            }
            if (to == null) {
                Dialogs.invalid(this, "The To date must be a valid yyyy-MM-dd date.", dateTo);
                return;
            }
            if (from.isAfter(to)) {
                Dialogs.invalid(this, "The From date must be before the To date.", dateFrom);
                return;
            }
        }
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        int warehouseId = type.usesWarehouse() && warehouse != null ? warehouse.getWarehouseId() : 0;
        try {
            current = reportDAO.generate(type, from, to, warehouseId);
            model = TableUtil.createModel(current.getColumns(), current.getColumnTypes());
            TableUtil.setRows(model, current.getRows());
            table.setModel(model);
            TableUtil.autoSizeColumns(table);
            lblReportTitle.setText(current.getTitle() + describeFilters());
            lblGenerated.setText("Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")));
            lblSummary.setText(current.getSummary());
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "generate the report");
        }
    }

    private String describeFilters() {
        ReportType type = (ReportType) cboReport.getSelectedItem();
        StringBuilder text = new StringBuilder();
        if (type != null && type.usesDateRange()) {
            text.append("  (").append(dateFrom.getText()).append(" to ").append(dateTo.getText()).append(")");
        }
        Warehouse warehouse = (Warehouse) cboWarehouse.getSelectedItem();
        if (type != null && type.usesWarehouse() && warehouse != null) {
            text.append("  -  ").append(warehouse.getWarehouseName());
        }
        return text.toString();
    }
}
