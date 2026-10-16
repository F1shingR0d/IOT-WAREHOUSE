package ui;

import dao.CustomerAccountDAO;
import dao.UserDAO;
import database.DatabaseConnection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import model.CustomerAccount;
import model.Status;
import model.User;
import ui.components.FlatButton;
import ui.components.InputBorder;
import ui.components.ModuleIcon;
import util.ErrorHandler;
import util.LengthFilter;
import util.Session;

/**
 * The login screen: Staff / Customer toggle, username, password,
 * [LOGIN] [CLEAR] [EXIT] and a link to register a new customer account.
 * Staff are checked against the USERS table, customers against
 * CUSTOMER_ACCOUNTS.
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String STAFF_HINT = "Sign in with your staff username and password.";
    private static final String CUSTOMER_HINT = "Customers: sign in to request stock from the warehouse.";

    private final UserDAO userDAO = new UserDAO();
    private final CustomerAccountDAO customerAccountDAO = new CustomerAccountDAO();
    private final JRadioButton rbStaff = new JRadioButton("Staff");
    private final JRadioButton rbCustomer = new JRadioButton("Customer");
    private final JLabel lblSubtitle = UITheme.mutedLabel(STAFF_HINT);
    private final JTextField txtUsername = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JCheckBox chkShowPassword = new JCheckBox("Show password");
    private final FlatButton btnLogin = new FlatButton("LOGIN", UITheme.PRIMARY, ModuleIcon.Type.LOGIN);
    private final FlatButton btnClear = FlatButton.outline("CLEAR", ModuleIcon.Type.CLEAR);
    private final FlatButton btnExit = FlatButton.outline("EXIT", ModuleIcon.Type.CLOSE);
    private final JLabel lnkRegister = new JLabel();
    private final JLabel lblMessage = new JLabel(" ");
    private final char echoChar;

    public LoginFrame() {
        super(UITheme.APP_NAME + " - Login");
        setIconImages(UITheme.appIcons());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                txtUsername.requestFocusInWindow();
            }
        });
        echoChar = txtPassword.getEchoChar();

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(new BrandPanel());
        root.add(createFormPanel());
        setContentPane(root);
        setSize(1040, 740);
        setResizable(false);
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(btnLogin);   // Enter in the password field logs in
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.CARD);
        form.setBorder(new EmptyBorder(40, 56, 32, 56));

        JLabel title = new JLabel("SYSTEM LOGIN");
        title.setFont(UITheme.FONT_TITLE.deriveFont(34f));
        title.setForeground(UITheme.BRAND);
        lblSubtitle.setFont(UITheme.FONT);

        // Staff / Customer toggle (JRadioButton in a ButtonGroup)
        ButtonGroup loginType = new ButtonGroup();
        loginType.add(rbStaff);
        loginType.add(rbCustomer);
        rbStaff.setSelected(true);
        rbStaff.setOpaque(false);
        rbCustomer.setOpaque(false);
        rbStaff.setFont(UITheme.FONT_BOLD);
        rbCustomer.setFont(UITheme.FONT_BOLD);
        JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typeRow.setOpaque(false);
        typeRow.add(UITheme.accentLabel("Login as:"));
        typeRow.add(Box.createHorizontalStrut(16));
        typeRow.add(rbStaff);
        typeRow.add(Box.createHorizontalStrut(18));
        typeRow.add(rbCustomer);

        lnkRegister.setText(linkText(false));
        lnkRegister.setFont(UITheme.FONT);
        lnkRegister.setForeground(UITheme.TEXT_MUTED);
        lnkRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lnkRegister.setHorizontalAlignment(JLabel.CENTER);
        lnkRegister.setToolTipText("Create a Customer Portal account");

        LengthFilter.apply(txtUsername, 50);
        LengthFilter.apply(txtPassword, 64);
        txtUsername.setPreferredSize(new Dimension(300, 44));
        txtPassword.setPreferredSize(new Dimension(300, 44));
        UITheme.setPlaceholder(txtUsername, "Enter your username");
        UITheme.setPlaceholder(txtPassword, "Enter your password");
        txtUsername.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.USER, 18, UITheme.TEXT_MUTED));
        txtPassword.putClientProperty(InputBorder.LEADING_ICON,
                new ModuleIcon(ModuleIcon.Type.KEY, 18, UITheme.TEXT_MUTED));
        chkShowPassword.setOpaque(false);
        chkShowPassword.setFont(UITheme.FONT_SMALL);
        lblMessage.setFont(UITheme.FONT_SMALL_BOLD);
        lblMessage.setForeground(UITheme.DANGER);

        JPanel mainButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        mainButtons.setOpaque(false);
        mainButtons.add(btnLogin);
        mainButtons.add(btnClear);

        JLabel footer = UITheme.mutedLabel("<html><span style='color:#4ac868'>&#9679;</span> Connected to Apache Derby"
                + "<br>" + DatabaseConnection.getUrl() + "</html>");
        footer.setMinimumSize(footer.getPreferredSize());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(title, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 18, 0);
        form.add(lblSubtitle, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 16, 0);
        form.add(typeRow, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(UITheme.label("Username"), c);
        c.gridy++;
        c.insets = new Insets(0, 0, 14, 0);
        form.add(txtUsername, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(UITheme.label("Password"), c);
        c.gridy++;
        c.insets = new Insets(0, 0, 6, 0);
        form.add(txtPassword, c);
        c.gridy++;
        form.add(chkShowPassword, c);
        c.gridy++;
        c.insets = new Insets(4, 0, 10, 0);
        form.add(lblMessage, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 10, 0);
        c.ipady = 6;
        form.add(mainButtons, c);
        c.gridy++;
        form.add(btnExit, c);
        c.gridy++;
        c.ipady = 0;
        c.insets = new Insets(4, 0, 0, 0);
        form.add(lnkRegister, c);
        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.SOUTHWEST;
        c.insets = new Insets(10, 0, 0, 0);
        form.add(footer, c);

        // ---------- events
        btnLogin.addActionListener(e -> login());
        btnClear.addActionListener(e -> clearForm());
        btnExit.addActionListener(e -> exitApplication());
        chkShowPassword.addItemListener(e ->
                txtPassword.setEchoChar(chkShowPassword.isSelected() ? (char) 0 : echoChar));
        rbStaff.addItemListener(e -> loginTypeChanged());
        rbCustomer.addItemListener(e -> loginTypeChanged());
        lnkRegister.addMouseListener(new MouseAdapter() {   // MouseListener: a clickable text link
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegistration();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                lnkRegister.setText(linkText(true));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lnkRegister.setText(linkText(false));
            }
        });
        txtUsername.addKeyListener(new KeyAdapter() {     // KeyListener: Enter moves to the password
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    txtPassword.requestFocusInWindow();
                }
            }
        });
        return form;
    }

    private void clearForm() {
        txtUsername.setText("");
        txtPassword.setText("");
        chkShowPassword.setSelected(false);
        lblMessage.setText(" ");
        txtUsername.requestFocusInWindow();
    }

    /** Validates the fields, then checks the credentials in a background thread. */
    private void login() {
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getPassword();
        if (username.isEmpty()) {
            lblMessage.setText("Please enter your username.");
            txtUsername.requestFocusInWindow();
            return;
        }
        if (password.length == 0) {
            lblMessage.setText("Please enter your password.");
            txtPassword.requestFocusInWindow();
            return;
        }
        setBusy(true);
        boolean customer = rbCustomer.isSelected();
        new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() throws Exception {
                if (!customer) {
                    return userDAO.authenticate(username, password);
                }
                CustomerAccount account = customerAccountDAO.authenticate(username, password);
                if (account != null && account.isActive() && Status.ACTIVE.equals(account.getCustomerStatus())) {
                    customerAccountDAO.recordLogin(account.getAccountId());
                }
                return account;
            }

            @Override
            protected void done() {
                Arrays.fill(password, '\0');
                setBusy(false);
                try {
                    Object result = get();
                    if (result == null) {
                        lblMessage.setText("Invalid username or password.");
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Invalid username or password.\nPlease try again."
                                + (customer ? "" : "\n\nCustomers: select \"Customer\" above before logging in."),
                                "Login Failed", JOptionPane.ERROR_MESSAGE);
                        txtPassword.setText("");
                        txtPassword.requestFocusInWindow();
                    } else if (customer) {
                        customerLoggedIn((CustomerAccount) result);
                    } else {
                        staffLoggedIn((User) result);
                    }
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof SQLException) {
                        ErrorHandler.showDatabaseError(LoginFrame.this, (SQLException) e.getCause(), "log in");
                    } else {
                        ErrorHandler.showUnexpectedError(LoginFrame.this, e.getCause());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void staffLoggedIn(User user) {
        if (!user.isActive()) {
            lblMessage.setText("This account is inactive.");
            JOptionPane.showMessageDialog(this, "Your account is INACTIVE.\nPlease contact the system administrator.",
                    "Login Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Session.start(user);
        dispose();
        new DashboardFrame().setVisible(true);
    }

    /** A customer may only enter when the account was approved and the customer record is active. */
    private void customerLoggedIn(CustomerAccount account) {
        if (account.isPending()) {
            lblMessage.setText("This account is waiting for approval.");
            JOptionPane.showMessageDialog(this, "Your registration is still waiting for approval by the warehouse.\n"
                    + "You can log in as soon as it is approved.", "Account Not Yet Approved",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!account.isActive() || !Status.ACTIVE.equals(account.getCustomerStatus())) {
            lblMessage.setText("This account is inactive.");
            JOptionPane.showMessageDialog(this, "Your customer account is INACTIVE.\nPlease contact the warehouse.",
                    "Login Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Session.startCustomer(account);
        dispose();
        new CustomerDashboardFrame().setVisible(true);
    }

    private void loginTypeChanged() {
        lblSubtitle.setText(rbCustomer.isSelected() ? CUSTOMER_HINT : STAFF_HINT);
        lblMessage.setText(" ");
    }

    /** Opens the registration form; afterwards the new username is filled in. */
    private void openRegistration() {
        RegisterDialog dialog = new RegisterDialog(this);
        dialog.setVisible(true);
        String username = dialog.getRegisteredUsername();
        if (username != null) {
            rbCustomer.setSelected(true);
            txtUsername.setText(username);
            txtPassword.setText("");
            lblMessage.setText(" ");
        }
    }

    private static String linkText(boolean hover) {
        String color = hover ? "#ffc440" : "#fbab06";
        return "<html>New customer?&nbsp; <span style='color:" + color + "'>"
                + (hover ? "<u>Register an account</u>" : "Register an account") + "</span></html>";
    }

    private void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnClear.setEnabled(!busy);
        btnLogin.setText(busy ? "Signing in..." : "LOGIN");
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        if (busy) {
            lblMessage.setText(" ");
        }
    }

    private void exitApplication() {
        if (Dialogs.confirm(this, "Exit " + UITheme.APP_NAME + "?", "Exit")) {
            dispose();
            DatabaseConnection.shutdown();
            System.exit(0);
        }
    }

    /** Left half of the login window: the I.O.T Warehouse logo on black, drawn with Java 2D. */
    static final class BrandPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        BrandPanel() {
            super(new BorderLayout());
            setBorder(new EmptyBorder(36, 40, 30, 36));
            JPanel top = new JPanel();
            top.setOpaque(false);
            top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));
            top.add(leftAligned(UITheme.logoLabel(170)));
            top.add(javax.swing.Box.createVerticalStrut(18));
            JLabel name = new JLabel(UITheme.APP_SUBTITLE);
            name.setFont(UITheme.FONT_TITLE.deriveFont(25f));
            name.setForeground(UITheme.BRAND);
            top.add(leftAligned(name));
            top.add(javax.swing.Box.createVerticalStrut(12));
            JLabel text = new JLabel("<html>Keep track of warehouses, products and every<br>"
                    + "stock movement: receiving, releasing and<br>stock transfers.</html>");
            text.setFont(UITheme.FONT.deriveFont(16f));
            text.setForeground(UITheme.TEXT);
            top.add(leftAligned(text));
            top.add(javax.swing.Box.createVerticalStrut(16));
            String bullet = "<span style='color:#fbab06'>&#9632;</span>&nbsp; ";
            JLabel features = new JLabel("<html>" + bullet + "Live stock per warehouse<br>"
                    + bullet + "Receiving, releasing and transfers<br>"
                    + bullet + "Low-stock alerts and reports<br>"
                    + bullet + "Role-based access<br>"
                    + bullet + "Customer Portal for stock requests</html>");
            features.setFont(UITheme.FONT.deriveFont(16f));
            features.setForeground(Color.WHITE);
            top.add(leftAligned(features));
            add(top, BorderLayout.NORTH);

            JLabel tech = new JLabel("Java Swing  |  AWT  |  JDBC  |  Apache Derby");
            tech.setFont(UITheme.FONT_SMALL);
            tech.setForeground(UITheme.TEXT_MUTED);
            add(tech, BorderLayout.SOUTH);
        }

        private static JLabel leftAligned(JLabel label) {
            label.setAlignmentX(LEFT_ALIGNMENT);
            return label;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(UITheme.BACKGROUND);
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(251, 171, 6, 22));   // decorative amber rings
            g.setStroke(new java.awt.BasicStroke(26f));
            g.drawOval(w - 190, h - 210, 330, 330);
            g.setColor(UITheme.BRAND);
            g.fillRect(w - 6, 0, 6, h);               // amber edge next to the form
            g.dispose();
        }
    }
}
