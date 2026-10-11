import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import ui.components.ModuleIcon;

/** Loads and pre-renders everything the reel draws (read-only after setup, so threads can share it). */
final class Assets {

    static final int W = 1920, H = 1080;

    // brand palette (same values as the app's UITheme)
    static final Color BG = new Color(11, 15, 17);
    static final Color CARD = new Color(15, 20, 25);
    static final Color CARD_BORDER = new Color(112, 82, 36);
    static final Color INPUT = new Color(19, 25, 32);
    static final Color AMBER = new Color(251, 171, 6);
    static final Color AMBER_LIGHT = new Color(255, 199, 66);
    static final Color ORANGE = new Color(247, 148, 29);
    static final Color AMBER_DARK = new Color(205, 130, 0);
    static final Color TEXT = new Color(238, 241, 244);
    static final Color MUTED = new Color(165, 174, 184);
    static final Color DIM = new Color(112, 121, 132);
    static final Color GREEN = new Color(74, 200, 104);
    static final Color RED = new Color(248, 96, 86);
    static final Color WARN = new Color(251, 146, 60);
    static final Color SLATE = new Color(150, 166, 186);
    static final Color CYAN = new Color(56, 189, 248);
    static final Color TABLE_HEAD = new Color(83, 61, 28);
    static final Color ROW_A = new Color(12, 18, 22);
    static final Color ROW_B = new Color(20, 26, 33);
    static final Color GRID = new Color(30, 37, 45);

    // fonts
    static Font black(float size) { return BLACK.deriveFont(size); }
    static Font semibold(float size) { return SEMIBOLD.deriveFont(size); }
    static Font regular(float size) { return REGULAR.deriveFont(size); }
    static Font bold(float size) { return REGULAR.deriveFont(Font.BOLD, size); }
    static Font light(float size) { return LIGHT.deriveFont(size); }
    static Font mono(float size) { return MONO.deriveFont(size); }

    static Font BLACK, SEMIBOLD, REGULAR, LIGHT, MONO;

    /** One cut-out layer of the logo, positioned in logo coordinates (1089 x 501). */
    static final class Piece {
        final BufferedImage img;
        final double x, y;

        Piece(BufferedImage img, double x, double y) {
            this.img = img;
            this.x = x;
            this.y = y;
        }

        double cx() { return x + img.getWidth() / 2.0; }
        double cy() { return y + img.getHeight() / 2.0; }
    }

    static final Map<String, Piece> LOGO = new LinkedHashMap<>();
    static BufferedImage logoFull;
    static final double LOGO_W = 1089, LOGO_H = 501;

    static BufferedImage shot;            // the real Products screen (dark theme)
    static BufferedImage background, dots, glowAmber, glowWhite, windowShadow;

    static final Map<ModuleIcon.Type, BufferedImage> ICON_AMBER = new EnumMap<>(ModuleIcon.Type.class);
    static final Map<ModuleIcon.Type, BufferedImage> ICON_BLACK = new EnumMap<>(ModuleIcon.Type.class);
    static final Map<ModuleIcon.Type, BufferedImage> ICON_MUTED = new EnumMap<>(ModuleIcon.Type.class);
    static final int ICON = 128;

    static void load(File reelDir, File appDir) throws Exception {
        BLACK = new Font("Segoe UI Black", Font.PLAIN, 10);
        SEMIBOLD = new Font("Segoe UI Semibold", Font.PLAIN, 10);
        REGULAR = new Font("Segoe UI", Font.PLAIN, 10);
        LIGHT = new Font("Segoe UI Light", Font.PLAIN, 10);
        MONO = new Font("Consolas", Font.PLAIN, 10);

        File logoDir = new File(reelDir, "assets/logo");
        List<String> lines = Files.readAllLines(new File(logoDir, "pieces.txt").toPath());
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] p = line.trim().split("\\s+");
            LOGO.put(p[0], new Piece(argb(ImageIO.read(new File(logoDir, p[0] + ".png"))), Double.parseDouble(p[1]), Double.parseDouble(p[2])));
        }
        logoFull = argb(ImageIO.read(new File(appDir, "src/ui/resources/iot_logo.png")));
        shot = argb(ImageIO.read(new File(appDir, "docs/screenshots/14_products.png")));

        // product icons, painted by the app's own ModuleIcon class
        for (ModuleIcon.Type type : ModuleIcon.Type.values()) {
            ICON_AMBER.put(type, icon(type, AMBER));
            ICON_BLACK.put(type, icon(type, Color.BLACK));
            ICON_MUTED.put(type, icon(type, MUTED));
        }

        background = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = background.createGraphics();
        g.setPaint(new RadialGradientPaint(W / 2f, H * 0.46f, W * 0.62f, new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(24, 31, 37), new Color(14, 19, 22), new Color(7, 9, 11)}));
        g.fillRect(0, 0, W, H);
        g.dispose();

        dots = new BufferedImage(W + 96, H + 96, BufferedImage.TYPE_INT_ARGB);
        g = dots.createGraphics();
        hq(g);
        for (int y = 0; y < dots.getHeight(); y += 48) {
            for (int x = 0; x < dots.getWidth(); x += 48) {
                g.setColor(new Color(60, 70, 84, 120));
                g.fillOval(x - 1, y - 1, 3, 3);
            }
        }
        g.dispose();

        glowAmber = radial(512, new Color(251, 160, 20), 1.0);
        glowWhite = radial(256, new Color(255, 240, 210), 1.0);
        windowShadow = shadow(1442, 900, 60);
    }

    static BufferedImage argb(BufferedImage src) {
        BufferedImage b = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = b.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return b;
    }

    static BufferedImage icon(ModuleIcon.Type type, Color color) {
        BufferedImage b = new BufferedImage(ICON, ICON, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = b.createGraphics();
        hq(g);
        new ModuleIcon(type, ICON - 16, color).paintIcon(null, g, 8, 8);
        g.dispose();
        return b;
    }

    /** Soft radial glow sprite (alpha falls off smoothly). */
    static BufferedImage radial(int size, Color c, double strength) {
        BufferedImage b = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = (x + 0.5) / size * 2 - 1, dy = (y + 0.5) / size * 2 - 1;
                double r = Math.sqrt(dx * dx + dy * dy);
                double a = r >= 1 ? 0 : Math.pow(1 - r, 2.2) * strength;
                int ai = (int) Math.round(255 * Math.min(1, a));
                int rr = c.getRed() * ai / 255, gg = c.getGreen() * ai / 255, bb = c.getBlue() * ai / 255;
                b.setRGB(x, y, (ai << 24) | (rr << 16) | (gg << 8) | bb);
            }
        }
        return b;
    }

    /** Blurred drop shadow of a w x h rectangle, with margin m around it. */
    static BufferedImage shadow(int w, int h, int m) {
        int sw = (w + 2 * m) / 4, sh = (h + 2 * m) / 4;
        float[] a = new float[sw * sh];
        for (int y = 0; y < sh; y++) {
            for (int x = 0; x < sw; x++) {
                int px = x * 4, py = y * 4;
                a[y * sw + x] = (px >= m && px < m + w && py >= m && py < m + h) ? 1f : 0f;
            }
        }
        for (int pass = 0; pass < 3; pass++) a = boxBlur(a, sw, sh, 6);
        BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB_PRE);
        for (int i = 0; i < a.length; i++) {
            int ai = (int) (Math.min(1, a[i]) * 200);
            small.setRGB(i % sw, i / sw, ai << 24);
        }
        BufferedImage big = new BufferedImage(w + 2 * m, h + 2 * m, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = big.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(small, 0, 0, big.getWidth(), big.getHeight(), null);
        g.dispose();
        return big;
    }

    static float[] boxBlur(float[] src, int w, int h, int r) {
        float[] tmp = new float[src.length], out = new float[src.length];
        for (int y = 0; y < h; y++) {
            float sum = 0;
            for (int x = -r; x <= r; x++) sum += src[y * w + Math.max(0, Math.min(w - 1, x))];
            for (int x = 0; x < w; x++) {
                tmp[y * w + x] = sum / (2 * r + 1);
                sum += src[y * w + Math.min(w - 1, x + r + 1)] - src[y * w + Math.max(0, x - r)];
            }
        }
        for (int x = 0; x < w; x++) {
            float sum = 0;
            for (int y = -r; y <= r; y++) sum += tmp[Math.max(0, Math.min(h - 1, y)) * w + x];
            for (int y = 0; y < h; y++) {
                out[y * w + x] = sum / (2 * r + 1);
                sum += tmp[Math.min(h - 1, y + r + 1) * w + x] - tmp[Math.max(0, y - r) * w + x];
            }
        }
        return out;
    }

    static void hq(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private Assets() {
    }
}
