package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.Status;
import ui.components.FlatButton;
import ui.components.ModuleIcon;
import util.TableUtil;

/**
 * Shows one receiving, release or transfer document: the header fields and
 * all its item lines (a master-detail view built from a JOIN query).
 */
public class TransactionDetailsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /**
     * @param headerFields label/value pairs, e.g. {"Warehouse", "Main Warehouse"}
     */
    public TransactionDetailsDialog(Window owner, String title, String status, String[][] headerFields,
                                    String[] itemColumns, Class<?>[] itemTypes, List<Object[]> itemRows,
                                    String totals) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UITheme.CARD);
        root.setBorder(new EmptyBorder(18, 20, 16, 20));

        // title and status
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.FONT_TITLE.deriveFont(28f));
        lblTitle.setForeground(UITheme.TEXT);
        top.add(lblTitle, BorderLayout.WEST);
        JLabel lblStatus = new JLabel(status);
        lblStatus.setFont(UITheme.FONT_HEADING);
        lblStatus.setForeground(UITheme.readable(statusColor(status)));
        top.add(lblStatus, BorderLayout.EAST);

        // header fields, two pairs per row
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        for (int i = 0; i < headerFields.length; i++) {
            int row = i / 2;
            int column = (i % 2) * 2;
            GridBagConstraints lc = new GridBagConstraints();
            lc.gridx = column;
            lc.gridy = row;
            lc.anchor = GridBagConstraints.NORTHWEST;
            lc.insets = new Insets(3, column == 0 ? 0 : 28, 3, 10);
            JLabel label = UITheme.mutedLabel(headerFields[i][0]);
            fields.add(label, lc);
            GridBagConstraints vc = new GridBagConstraints();
            vc.gridx = column + 1;
            vc.gridy = row;
            vc.weightx = 1;
            vc.anchor = GridBagConstraints.NORTHWEST;
            vc.insets = new Insets(3, 0, 3, 0);
            JLabel value = new JLabel(headerFields[i][1] == null || headerFields[i][1].isEmpty() ? "-" : headerFields[i][1]);
            value.setFont(UITheme.FONT_BOLD);
            value.setForeground(UITheme.TEXT);
            fields.add(value, vc);
        }

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(fields, BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);

        // item lines
        DefaultTableModel model = TableUtil.createModel(itemColumns, itemTypes);
        TableUtil.setRows(model, itemRows);
        JTable table = TableUtil.createTable(model);
        TableUtil.autoSizeColumns(table);
        root.add(UITheme.scroll(table), BorderLayout.CENTER);

        // totals and buttons
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel lblTotals = new JLabel(totals);
        lblTotals.setFont(UITheme.FONT_BOLD);
        lblTotals.setForeground(UITheme.TEXT);
        bottom.add(lblTotals, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        FlatButton btnPrint = FlatButton.outline("Print", ModuleIcon.Type.PRINT);
        FlatButton btnClose = new FlatButton("Close", UITheme.PRIMARY, ModuleIcon.Type.CLOSE);
        buttons.add(btnPrint);
        buttons.add(btnClose);
        bottom.add(buttons, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);

        btnPrint.addActionListener(e -> TableUtil.print(this, table, title + "  (" + status + ")"));
        btnClose.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(btnClose);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setContentPane(root);
        setSize(new Dimension(960, 600));
        setMinimumSize(new Dimension(640, 420));
        setLocationRelativeTo(owner);
    }

    /** Documents: POSTED green, VOIDED red. Customer requests use their own status colors. */
    private static java.awt.Color statusColor(String status) {
        switch (status) {
            case Status.VOIDED:
            case Status.REJECTED:
                return UITheme.DANGER;
            case Status.PENDING:
                return UITheme.WARNING;
            case Status.APPROVED:
                return UITheme.INFO;
            case Status.CANCELLED:
                return UITheme.NEUTRAL;
            default:
                return UITheme.SUCCESS;
        }
    }
}
