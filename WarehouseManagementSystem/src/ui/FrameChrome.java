package ui;

import database.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import ui.components.DarkScrollBarUI;
import ui.components.FlatButton;
import ui.components.ModuleIcon;

/**
 * The parts shared by the two main windows (the staff DashboardFrame and
 * the CustomerDashboardFrame): the black header with the logo, the sidebar
 * with the amber stripes and the user card, and the status bar. These stay
 * dark; only the work area in the middle uses the light theme.
 */
final class FrameChrome {

    private FrameChrome() {
    }

    /** Black bar with the logo, a title, the clock and a Logout button. */
    static JComponent header(String title, JLabel clock, Runnable logout) {
        // black bar with an amber line underneath that fades out to the right
        JPanel header = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                int y = getHeight() - 3;
                g.setPaint(new java.awt.GradientPaint(0, 0, UITheme.BRAND, getWidth(), 0,
                        new Color(251, 171, 6, 60)));
                g.fillRect(0, y, getWidth(), 3);
                g.dispose();
            }
        };
        header.setBackground(UITheme.HEADER);
        header.setBorder(new EmptyBorder(6, 22, 9, 18));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(UITheme.logoLabel(64));
        JComponent divider = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(UITheme.BRAND);
                g.fillRect(getWidth() / 2 - 1, 8, 2, getHeight() - 16);
            }
        };
        divider.setPreferredSize(new Dimension(44, 64));
        brand.add(divider);
        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new java.awt.Font(UITheme.FONT_NAME, java.awt.Font.BOLD, 30));
        lblTitle.setForeground(UITheme.BRAND);
        lblTitle.setPreferredSize(new Dimension(lblTitle.getPreferredSize().width, 64));
        brand.add(lblTitle);
        header.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        right.setOpaque(false);
        clock.setFont(UITheme.FONT_NAV_BOLD);
        clock.setForeground(UITheme.CHROME_TEXT);
        clock.setIcon(new ModuleIcon(ModuleIcon.Type.CALENDAR, 24, UITheme.BRAND));
        clock.setIconTextGap(10);
        right.add(clock);
        right.add(Box.createHorizontalStrut(6));
        FlatButton btnLogout = new FlatButton("Logout", UITheme.BRAND, ModuleIcon.Type.USER);
        btnLogout.addActionListener(e -> logout.run());
        right.add(btnLogout);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** An empty sidebar panel (vertical BoxLayout) with the amber stripes at the bottom. */
    static JPanel sidebarPanel() {
        JPanel sidebar = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                paintStripes((Graphics2D) graphics.create(), getWidth(), getHeight());
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.SIDEBAR);
        sidebar.setBorder(new EmptyBorder(16, 12, 70, 12));   // room for the amber stripes at the bottom
        return sidebar;
    }

    /** Puts the finished sidebar in a scroll pane of the standard sidebar width. */
    static JScrollPane sidebarScroll(JPanel sidebar) {
        sidebar.add(Box.createVerticalGlue());
        JScrollPane scroll = new JScrollPane(sidebar, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(new MatteBorder(0, 0, 0, 1, new Color(70, 52, 22)));
        scroll.getViewport().setBackground(UITheme.SIDEBAR);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setUI(DarkScrollBarUI.chrome());
        scroll.setPreferredSize(new Dimension(262, 100));
        return scroll;
    }

    /** Two diagonal amber bands in the bottom-left corner of the sidebar (decoration). */
    private static void paintStripes(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[][] bands = {{0, 22, 150}, {0, 52, 230}};   // {x, height above the bottom, width}
        g.setColor(new Color(251, 171, 6, 210));
        for (int[] band : bands) {
            int top = height - band[1];
            java.awt.geom.Path2D shape = new java.awt.geom.Path2D.Float();
            shape.moveTo(band[0], top + 16);
            shape.lineTo(band[2], top - 14);
            shape.lineTo(band[2] + 26, top - 14);
            shape.lineTo(band[0], top + 34);
            shape.closePath();
            g.fill(shape);
        }
        g.dispose();
    }

    /** Amber circle with the initials, the name in bold and a second line (role). */
    static JComponent userCard(String name, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(new EmptyBorder(0, 6, 10, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        String initials = initialsOf(name);
        JComponent avatar = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(UITheme.BRAND);
                g.fillOval(0, 0, 46, 46);
                g.setColor(Color.BLACK);
                g.setFont(UITheme.FONT_BOLD.deriveFont(18f));
                int w = g.getFontMetrics().stringWidth(initials);
                g.drawString(initials, 23 - w / 2, 23 + g.getFontMetrics().getAscent() / 2 - 3);
                g.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(46, 46));
        card.add(avatar, BorderLayout.WEST);

        JLabel lblName = new JLabel("<html><b>" + escape(name) + "</b><br>"
                + "<span style='color:#eef1f4; font-size:12px'>" + escape(subtitle) + "</span></html>");
        lblName.setFont(UITheme.FONT_BOLD);
        lblName.setForeground(UITheme.BRAND);
        card.add(lblName, BorderLayout.CENTER);
        return card;
    }

    /** Small amber heading between the sidebar buttons, e.g. "MASTER DATA". */
    static void section(JPanel sidebar, String title) {
        JLabel label = new JLabel(title);
        label.setFont(UITheme.FONT_SECTION);
        label.setForeground(UITheme.SIDEBAR_SECTION);
        label.setBorder(new EmptyBorder(12, 8, 4, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(label);
    }

    /** Database connection on the left, who is logged in on the right. */
    static JComponent statusBar(String loggedInText) {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(UITheme.SIDEBAR);
        status.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, UITheme.CHROME_BORDER), new EmptyBorder(5, 16, 5, 16)));
        JLabel db = new JLabel("<html><span style='color:#4ac868'>&#9679;</span>&nbsp; Connected to Apache Derby"
                + " &nbsp;|&nbsp; " + DatabaseConnection.getUrl() + "</html>");
        db.setFont(UITheme.FONT_SMALL);
        db.setForeground(UITheme.CHROME_MUTED);
        status.add(db, BorderLayout.WEST);
        JLabel who = new JLabel(loggedInText);
        who.setFont(UITheme.FONT_SMALL);
        who.setForeground(UITheme.CHROME_MUTED);
        status.add(who, BorderLayout.EAST);
        return status;
    }

    /** The menu bar stays dark like the header, so its menu titles are written in light text. */
    static void styleMenuBar(JMenuBar bar) {
        for (int i = 0; i < bar.getMenuCount(); i++) {
            bar.getMenu(i).setForeground(UITheme.CHROME_TEXT);
            bar.getMenu(i).setOpaque(false);   // the dark menu bar shows through (the drop-down lists stay white)
        }
    }

    static String initialsOf(String fullName) {
        StringBuilder initials = new StringBuilder();
        for (String part : fullName.trim().split("\\s+")) {
            if (!part.isEmpty() && Character.isLetterOrDigit(part.charAt(0)) && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.length() == 0 ? "?" : initials.toString();
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
