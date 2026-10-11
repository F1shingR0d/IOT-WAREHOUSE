package ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.JTextComponent;
import ui.components.DarkButtonUI;
import ui.components.DarkComboBoxUI;
import ui.components.DarkMetalTheme;
import ui.components.DarkScrollBarUI;
import ui.components.DarkTabbedPaneUI;
import ui.components.InputBorder;
import ui.components.ToggleIcon;

/**
 * One place for the colors, fonts and common widgets of the program, so that
 * every screen looks consistent (AWT Color, Font and Dimension are defined here).
 *
 * The program uses a dark theme in the I.O.T Warehouse brand colors: amber
 * text and accents on near-black screens, with large, readable fonts.
 */
public final class UITheme {

    // ---------------------------------------------------------------- brand colors
    public static final Color BRAND = new Color(251, 171, 6);            // logo amber
    public static final Color BRAND_LIGHT = new Color(255, 196, 64);
    public static final Color BRAND_BLACK = new Color(11, 15, 17);       // screen background

    public static final Color PRIMARY = BRAND;
    public static final Color PRIMARY_DARK = new Color(214, 140, 0);
    /** Dark amber tint for hover and highlight backgrounds. */
    public static final Color PRIMARY_LIGHT = new Color(58, 44, 16);
    /** Amber used for text (readable on the dark backgrounds). */
    public static final Color PRIMARY_TEXT = BRAND;

    // ---------------------------------------------------------------- status colors (light, for dark screens)
    public static final Color SUCCESS = new Color(74, 200, 104);
    public static final Color DANGER = new Color(248, 96, 86);
    public static final Color WARNING = new Color(251, 146, 60);
    public static final Color CHARCOAL = new Color(150, 166, 186);
    public static final Color INFO = new Color(56, 189, 248);
    public static final Color NEUTRAL = new Color(150, 158, 168);

    // ---------------------------------------------------------------- surfaces
    public static final Color BACKGROUND = BRAND_BLACK;
    public static final Color SIDEBAR = new Color(8, 11, 14);
    public static final Color SIDEBAR_HOVER = new Color(26, 32, 40);
    public static final Color SIDEBAR_TEXT = new Color(236, 239, 242);
    public static final Color SIDEBAR_SECTION = BRAND;
    public static final Color CARD = new Color(15, 20, 25);
    /** Thin amber-brown line around the cards. */
    public static final Color CARD_BORDER = new Color(112, 82, 36);
    public static final Color BORDER = new Color(40, 47, 56);
    public static final Color INPUT = new Color(19, 25, 32);
    public static final Color INPUT_BORDER = new Color(60, 70, 83);
    public static final Color INPUT_DISABLED = new Color(25, 31, 38);
    public static final Color BUTTON_DARK = new Color(22, 28, 36);
    public static final Color BUTTON_BORDER = new Color(66, 78, 92);

    // ---------------------------------------------------------------- text
    public static final Color TEXT = new Color(238, 241, 244);
    public static final Color TEXT_MUTED = new Color(165, 174, 184);
    public static final Color TEXT_DIM = new Color(112, 121, 132);

    // ---------------------------------------------------------------- tables
    public static final Color TABLE_HEADER = new Color(83, 61, 28);
    public static final Color TABLE_HEADER_TEXT = new Color(252, 243, 226);
    public static final Color TABLE_ROW = new Color(12, 18, 22);
    public static final Color TABLE_STRIPE = new Color(20, 26, 33);
    public static final Color TABLE_GRID = new Color(30, 37, 45);
    public static final Color TABLE_SELECTION = new Color(104, 76, 22);

    // ---------------------------------------------------------------- fonts (large and readable)
    public static final String FONT_NAME = chooseFontName();
    public static final Font FONT = new Font(FONT_NAME, Font.PLAIN, 16);
    public static final Font FONT_BOLD = new Font(FONT_NAME, Font.BOLD, 16);
    public static final Font FONT_SMALL = new Font(FONT_NAME, Font.PLAIN, 15);
    public static final Font FONT_SMALL_BOLD = new Font(FONT_NAME, Font.BOLD, 15);
    public static final Font FONT_TABLE = new Font(FONT_NAME, Font.PLAIN, 15);
    public static final Font FONT_TABLE_BOLD = new Font(FONT_NAME, Font.BOLD, 15);
    public static final Font FONT_NAV = new Font(FONT_NAME, Font.PLAIN, 17);
    public static final Font FONT_NAV_BOLD = new Font(FONT_NAME, Font.BOLD, 17);
    public static final Font FONT_SECTION = new Font(FONT_NAME, Font.BOLD, 14);
    public static final Font FONT_HEADING = new Font(FONT_NAME, Font.BOLD, 21);
    public static final Font FONT_TITLE = new Font(FONT_NAME, Font.BOLD, 40);
    public static final Font FONT_STAT = new Font(FONT_NAME, Font.BOLD, 30);

    public static final String APP_NAME = "I.O.T Warehouse";
    public static final String APP_SUBTITLE = "Warehouse Management System";

    private UITheme() {
    }

    private static String chooseFontName() {
        List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        return installed.contains("Segoe UI") ? "Segoe UI" : Font.SANS_SERIF;
    }

    // ---------------------------------------------------------------- look and feel

    /**
     * Installs the dark theme: the cross-platform Metal look and feel with a
     * dark color theme, our own painted combo boxes, scroll bars, tabs, buttons,
     * check boxes and radio buttons, and the same large font everywhere.
     */
    public static void install() {
        try {
            MetalLookAndFeel.setCurrentTheme(new DarkMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception e) {
            // the default look and feel is used instead
        }
        FontUIResource font = new FontUIResource(FONT);
        for (Object key : Collections.list(UIManager.getLookAndFeelDefaults().keys())) {
            if (key instanceof String && ((String) key).toLowerCase().endsWith("font")) {
                UIManager.put(key, font);
            }
        }
        UIManager.put("ToolTip.font", new FontUIResource(FONT_SMALL));
        UIManager.put("TableHeader.font", new FontUIResource(FONT_TABLE_BOLD));
        UIManager.put("Table.font", new FontUIResource(FONT_TABLE));

        // our own painted components (javax.swing.plaf delegates)
        UIManager.put("ComboBoxUI", DarkComboBoxUI.class.getName());
        UIManager.put("ScrollBarUI", DarkScrollBarUI.class.getName());
        UIManager.put("TabbedPaneUI", DarkTabbedPaneUI.class.getName());
        UIManager.put("ButtonUI", DarkButtonUI.class.getName());
        UIManager.put("CheckBox.icon", ToggleIcon.checkBox());
        UIManager.put("RadioButton.icon", ToggleIcon.radioButton());

        put("Panel.background", BACKGROUND);
        put("Panel.foreground", TEXT);
        put("Label.foreground", TEXT);
        put("Label.disabledForeground", TEXT_DIM);
        put("OptionPane.background", CARD);
        UIManager.put("OptionPane.border", new EmptyBorder(16, 18, 14, 18));
        UIManager.put("OptionPane.buttonAreaBorder", new EmptyBorder(16, 0, 0, 0));
        put("OptionPane.messageForeground", TEXT);
        put("Viewport.background", CARD);
        put("ScrollPane.background", CARD);
        UIManager.put("ScrollBar.width", 12);

        for (String field : new String[]{"TextField", "PasswordField", "FormattedTextField"}) {
            put(field + ".background", INPUT);
            put(field + ".foreground", TEXT);
            put(field + ".caretForeground", TEXT);
            put(field + ".selectionBackground", BRAND);
            put(field + ".selectionForeground", Color.BLACK);
            put(field + ".inactiveBackground", INPUT_DISABLED);
            put(field + ".inactiveForeground", TEXT_MUTED);
            put(field + ".disabledBackground", INPUT_DISABLED);
            UIManager.put(field + ".border", new InputBorder());
        }
        put("TextArea.background", INPUT);
        put("TextArea.foreground", TEXT);
        put("TextArea.caretForeground", TEXT);
        put("TextArea.selectionBackground", BRAND);
        put("TextArea.selectionForeground", Color.BLACK);
        put("TextArea.inactiveBackground", INPUT_DISABLED);
        UIManager.put("TextArea.margin", new InsetsUIResource(6, 8, 6, 8));

        put("ComboBox.background", INPUT);
        put("ComboBox.foreground", TEXT);
        put("ComboBox.selectionBackground", BRAND);
        put("ComboBox.selectionForeground", Color.BLACK);
        put("ComboBox.disabledBackground", INPUT_DISABLED);
        put("ComboBox.disabledForeground", TEXT_DIM);
        put("List.background", INPUT);
        put("List.foreground", TEXT);
        put("List.selectionBackground", BRAND);
        put("List.selectionForeground", Color.BLACK);

        put("Table.background", TABLE_ROW);
        put("Table.foreground", TEXT);
        put("Table.gridColor", TABLE_GRID);
        put("Table.selectionBackground", TABLE_SELECTION);
        put("Table.selectionForeground", Color.WHITE);
        put("TableHeader.background", TABLE_HEADER);
        put("TableHeader.foreground", TABLE_HEADER_TEXT);

        put("TabbedPane.background", BACKGROUND);
        put("TabbedPane.foreground", TEXT_MUTED);
        UIManager.put("TabbedPane.contentOpaque", Boolean.FALSE);

        put("CheckBox.foreground", TEXT);
        put("CheckBox.background", CARD);
        put("RadioButton.foreground", TEXT);
        put("RadioButton.background", CARD);
        put("Button.foreground", TEXT);
        put("Button.background", BUTTON_DARK);

        put("MenuBar.background", SIDEBAR);
        put("MenuBar.foreground", TEXT);
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        for (String menu : new String[]{"Menu", "MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem"}) {
            put(menu + ".background", CARD);
            put(menu + ".foreground", TEXT);
            put(menu + ".selectionBackground", BRAND);
            put(menu + ".selectionForeground", Color.BLACK);
            put(menu + ".acceleratorForeground", TEXT_MUTED);
            put(menu + ".acceleratorSelectionForeground", Color.BLACK);
            put(menu + ".disabledForeground", TEXT_DIM);
        }
        put("PopupMenu.background", CARD);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(CARD_BORDER));
        put("Separator.foreground", BORDER);
        put("Separator.background", CARD);
        put("ToolTip.background", new Color(28, 35, 44));
        put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER), new EmptyBorder(4, 8, 4, 8)));
        put("ProgressBar.foreground", BRAND);
        put("ProgressBar.background", new Color(30, 37, 45));
        put("ProgressBar.selectionBackground", Color.BLACK);
        put("ProgressBar.selectionForeground", Color.BLACK);
        UIManager.put("ProgressBar.border", BorderFactory.createEmptyBorder());

        // the input borders turn amber while a field has the keyboard focus
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", e -> {
            repaintField(e.getOldValue());
            repaintField(e.getNewValue());
        });
    }

    private static void put(String key, Color color) {
        UIManager.put(key, new ColorUIResource(color));
    }

    private static void repaintField(Object component) {
        if (component instanceof JTextComponent) {
            JComponent field = (JComponent) component;
            field.repaint();
            Container parent = field.getParent();
            if (parent instanceof JViewport && parent.getParent() != null) {
                parent.getParent().repaint();   // a text area: its scroll pane draws the border
            } else if (parent != null) {
                parent.repaint();               // e.g. the date field with its calendar button
            }
        }
    }

    // ---------------------------------------------------------------- labels

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    /** Amber card title such as "Product Details". */
    public static JLabel headingLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_HEADING);
        label.setForeground(BRAND);
        return label;
    }

    /** Amber bold label, e.g. "Search:". */
    public static JLabel accentLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(BRAND);
        return label;
    }

    /**
     * The title area at the top of every screen: a big amber title, a short
     * description and optional buttons on the right.
     */
    public static JPanel pageHeader(String title, String subtitle, JComponent... actions) {
        JLabel lblSubtitle = new JLabel(subtitle);
        return pageHeader(title, lblSubtitle, actions);
    }

    /** Same as above, with a subtitle label that the screen can change later. */
    public static JPanel pageHeader(String title, JLabel lblSubtitle, JComponent... actions) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_TITLE);
        lblTitle.setForeground(BRAND);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblSubtitle.setFont(FONT.deriveFont(17f));
        lblSubtitle.setForeground(TEXT);
        lblSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(lblTitle);
        texts.add(lblSubtitle);
        header.add(texts, BorderLayout.CENTER);

        if (actions.length > 0) {
            JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 14));
            buttons.setOpaque(false);
            for (JComponent action : actions) {
                buttons.add(action);
            }
            header.add(buttons, BorderLayout.EAST);
        }
        return header;
    }

    /**
     * Scroll pane in the theme colors: tables get a thin dark border, text
     * areas get the same rounded border as the other input fields.
     */
    public static JScrollPane scroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        boolean textArea = view instanceof JTextComponent;
        scroll.setBorder(textArea ? new InputBorder(new Insets(3, 3, 3, 3)) : BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(textArea ? INPUT : TABLE_ROW);
        scroll.setBackground(CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /** Scroll pane without a border (used around forms). */
    public static JScrollPane plainScroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    public static Border padding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    /** Gray hint text shown inside an empty text field, e.g. "Search product...". */
    public static void setPlaceholder(JTextComponent field, String placeholder) {
        field.putClientProperty(InputBorder.PLACEHOLDER, placeholder);
        field.repaint();
    }

    /**
     * Combo box renderer that shows a hint such as "Select a warehouse" while
     * nothing is selected.
     */
    public static <T> ListCellRenderer<? super T> placeholderRenderer(String placeholder) {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(new EmptyBorder(3, 8, 3, 8));
                if (value == null) {
                    setText(placeholder);
                    if (!isSelected) {
                        setForeground(TEXT_MUTED);
                    }
                }
                return this;
            }
        };
    }

    // ---------------------------------------------------------------- app icon (drawn with Java 2D)

    /** The program icon in several sizes for window title bars and the taskbar. */
    public static List<Image> appIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 20, 24, 32, 40, 48, 64, 128}) {
            icons.add(createAppIcon(size));
        }
        return icons;
    }

    public static ImageIcon appIcon(int size) {
        return new ImageIcon(createAppIcon(size));
    }

    /**
     * The brand mark of I.O.T Warehouse drawn with Java 2D: the amber "O" of
     * the logo (a ring split by a horizontal line) on a black rounded square.
     */
    public static BufferedImage createAppIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        float s = size;
        g.setColor(Color.BLACK);
        g.fill(new RoundRectangle2D.Float(0, 0, s, s, s * 0.28f, s * 0.28f));

        float ring = s * 0.64f;
        float thickness = Math.max(2f, s * 0.15f);
        float x = (s - ring) / 2f;
        g.setColor(BRAND);
        g.setStroke(new BasicStroke(thickness));
        g.draw(new Ellipse2D.Float(x + thickness / 2f, x + thickness / 2f, ring - thickness, ring - thickness));
        // the black line through the middle of the "O"
        g.setColor(Color.BLACK);
        float gap = Math.max(1f, s * 0.06f);
        g.fill(new Rectangle2D.Float(0, (s - gap) / 2f, s, gap));
        g.dispose();
        return image;
    }

    // ---------------------------------------------------------------- logo

    private static BufferedImage logoImage;

    /**
     * The I.O.T Warehouse logo (transparent background) scaled to the given
     * height. Returns null if the image file is missing.
     */
    public static ImageIcon logo(int height) {
        BufferedImage source = loadLogo();
        if (source == null) {
            return null;
        }
        int width = Math.round(source.getWidth() * (height / (float) source.getHeight()));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return new ImageIcon(scaled);
    }

    /** A label showing the logo, or the name in amber text if the image cannot be loaded. */
    public static JLabel logoLabel(int height) {
        ImageIcon icon = logo(height);
        if (icon != null) {
            return new JLabel(icon);
        }
        JLabel text = new JLabel(APP_NAME);
        text.setFont(new Font(FONT_NAME, Font.BOLD, Math.max(16, height / 2)));
        text.setForeground(BRAND);
        return text;
    }

    private static synchronized BufferedImage loadLogo() {
        if (logoImage == null) {
            try (java.io.InputStream in = UITheme.class.getResourceAsStream("resources/iot_logo.png")) {
                if (in != null) {
                    logoImage = javax.imageio.ImageIO.read(in);
                }
            } catch (java.io.IOException e) {
                logoImage = null;
            }
        }
        return logoImage;
    }

    // ---------------------------------------------------------------- color helpers

    /** Black or white, whichever is easier to read on the given background. */
    public static Color textOn(Color background) {
        double luminance = (0.2126 * background.getRed() + 0.7152 * background.getGreen()
                + 0.0722 * background.getBlue()) / 255;
        return luminance > 0.55 ? Color.BLACK : Color.WHITE;
    }
}
