package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import ui.components.FlatButton;
import ui.components.ModuleIcon;

/**
 * Information about the system and the project team.
 * Replace the placeholders below with your group's details.
 */
public class AboutDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    // ---- EDIT THESE LINES WITH YOUR GROUP'S INFORMATION ----
    private static final String[] TEAM_MEMBERS = {
        "[Member 1 - Full Name]",
        "[Member 2 - Full Name]",
        "[Member 3 - Full Name]",
        "[Member 4 - Full Name]"
    };
    private static final String COURSE = "[Course / Subject Code]";
    private static final String SCHOOL = "[School Name]";
    private static final String INSTRUCTOR = "[Instructor Name]";
    // ---------------------------------------------------------

    public AboutDialog(Window owner) {
        super(owner, "About", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        StringBuilder team = new StringBuilder();
        for (String member : TEAM_MEMBERS) {
            team.append("&#8226; ").append(member).append("<br>");
        }
        JLabel text = new JLabel("<html><div style='width:430px'>"
                + "<span style='font-size:18px'><b>" + UITheme.APP_NAME + "</b></span><br>"
                + "<b>" + UITheme.APP_SUBTITLE + "</b><br>"
                + "<span style='color:#a5aeb8'>Version 1.0</span><br><br>"
                + "A desktop information system that records warehouses, products, receiving, "
                + "releasing and stock transfers, keeping a live stock balance for every product "
                + "in every warehouse.<br><br>"
                + "<b>Built with:</b> Java (Swing and AWT), JDBC and Apache Derby 10.16<br><br>"
                + "<b>Developed by:</b><br>" + team
                + "<br><b>Course:</b> " + COURSE + "<br>"
                + "<b>School:</b> " + SCHOOL + "<br>"
                + "<b>Instructor:</b> " + INSTRUCTOR
                + "</div></html>");
        text.setFont(UITheme.FONT);
        text.setForeground(UITheme.TEXT);
        text.setVerticalAlignment(JLabel.TOP);

        // black banner with the I.O.T Warehouse logo
        JPanel banner = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        banner.setBackground(UITheme.BACKGROUND);
        banner.setBorder(new javax.swing.border.MatteBorder(0, 0, 3, 0, UITheme.BRAND));
        banner.add(UITheme.logoLabel(96));

        FlatButton btnClose = new FlatButton("Close", UITheme.PRIMARY, ModuleIcon.Type.CLOSE);
        btnClose.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(btnClose);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.CARD);
        root.setBorder(new EmptyBorder(22, 24, 18, 24));
        root.add(banner, BorderLayout.NORTH);
        root.add(text, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(btnClose);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}
