import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** Easing curves, springs and drawing helpers for the motion graphics. */
final class Fx {

    private Fx() {
    }

    // ------------------------------------------------------------------ math

    static double clamp01(double x) { return x < 0 ? 0 : x > 1 ? 1 : x; }

    static double clamp(double x, double a, double b) { return x < a ? a : x > b ? b : x; }

    static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    /** 0..1 progress of t through [start, start + dur]. */
    static double prog(double t, double start, double dur) { return clamp01((t - start) / dur); }

    static double smooth(double x) { x = clamp01(x); return x * x * (3 - 2 * x); }

    static double outCubic(double x) { x = clamp01(x); return 1 - Math.pow(1 - x, 3); }

    static double inCubic(double x) { x = clamp01(x); return x * x * x; }

    static double inOutCubic(double x) { x = clamp01(x); return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2; }

    static double outQuint(double x) { x = clamp01(x); return 1 - Math.pow(1 - x, 5); }

    static double outExpo(double x) { x = clamp01(x); return x >= 1 ? 1 : 1 - Math.pow(2, -10 * x); }

    static double inExpo(double x) { x = clamp01(x); return x <= 0 ? 0 : Math.pow(2, 10 * x - 10); }

    static double inOutExpo(double x) {
        x = clamp01(x);
        if (x == 0 || x == 1) return x;
        return x < 0.5 ? Math.pow(2, 20 * x - 10) / 2 : (2 - Math.pow(2, -20 * x + 10)) / 2;
    }

    static double outBack(double x) { return outBack(x, 1.9); }

    static double outBack(double x, double s) {
        x = clamp01(x);
        double c3 = s + 1;
        return 1 + c3 * Math.pow(x - 1, 3) + s * Math.pow(x - 1, 2);
    }

    /**
     * Damped spring from 0 to 1 (overshoots and settles). dt in seconds since
     * the start, freq in rad/s, decay = how fast the wobble dies.
     */
    static double spring(double dt, double freq, double decay) {
        if (dt <= 0) return 0;
        return 1 - Math.exp(-decay * dt) * Math.cos(freq * dt);
    }

    /** A decaying wobble around 0 (for squash & stretch). */
    static double wobble(double dt, double freq, double decay) {
        if (dt <= 0) return 0;
        return Math.exp(-decay * dt) * Math.sin(freq * dt);
    }

    /** Deterministic pseudo-random 0..1 from an integer. */
    static double hash(int n) {
        long x = n * 0x9E3779B97F4A7C15L;
        x ^= (x >>> 31);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 29);
        return ((x >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
    }

    /** Smooth 1D value noise in -1..1. */
    static double noise(double x, int seed) {
        int i = (int) Math.floor(x);
        double f = x - i, u = f * f * (3 - 2 * f);
        return lerp(hash(i * 31 + seed) * 2 - 1, hash((i + 1) * 31 + seed) * 2 - 1, u);
    }

    // ------------------------------------------------------------------ color

    static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(255 * clamp01(a) * c.getAlpha() / 255.0));
    }

    static Color mix(Color a, Color b, double t) {
        t = clamp01(t);
        return new Color((int) lerp(a.getRed(), b.getRed(), t), (int) lerp(a.getGreen(), b.getGreen(), t),
                (int) lerp(a.getBlue(), b.getBlue(), t), (int) lerp(a.getAlpha(), b.getAlpha(), t));
    }

    static Composite opacity(double a) {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp01(a));
    }

    // ------------------------------------------------------------------ text

    static double advance(Graphics2D g, Font f, String s) {
        FontRenderContext frc = g.getFontRenderContext();
        return f.getStringBounds(s, frc).getWidth();
    }

    /** Width of a string drawn with extra letter spacing. */
    static double width(Graphics2D g, Font f, String s, double tracking) {
        double w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += advance(g, f, s.substring(i, i + 1));
            if (i < s.length() - 1) w += tracking;
        }
        return w;
    }

    /** Draws text with letter spacing; (x, y) = left baseline. */
    static void text(Graphics2D g, Font f, String s, double x, double y, double tracking, Color c) {
        g.setFont(f);
        g.setColor(c);
        if (tracking == 0) {
            g.drawString(s, (float) x, (float) y);
            return;
        }
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            g.drawString(ch, (float) x, (float) y);
            x += advance(g, f, ch) + tracking;
        }
    }

    static void textCentered(Graphics2D g, Font f, String s, double cx, double y, double tracking, Color c) {
        text(g, f, s, cx - width(g, f, s, tracking) / 2, y, tracking, c);
    }

    static void textRight(Graphics2D g, Font f, String s, double rx, double y, Color c) {
        text(g, f, s, rx - advance(g, f, s), y, 0, c);
    }

    /**
     * Kinetic line: each letter rises from below a mask with a small stagger.
     * (x, y) = left baseline; t0 = start time.
     */
    static void riseText(Graphics2D g, Font f, String s, double x, double y, double tracking, Color c,
                         double t, double t0, double stagger, double dur) {
        g.setFont(f);
        java.awt.FontMetrics fm = g.getFontMetrics(f);
        Shape oldClip = g.getClip();
        double w = width(g, f, s, tracking);
        g.clip(new java.awt.geom.Rectangle2D.Double(x - 20, y - fm.getAscent() - 10, w + 60, fm.getAscent() + fm.getDescent() + 20));
        double cx = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            double p = outExpo(prog(t, t0 + i * stagger, dur));
            if (p > 0) {
                double dy = (1 - p) * (fm.getAscent() + fm.getDescent() + 10);
                g.setColor(alpha(c, Math.min(1, p * 1.6)));
                g.drawString(ch, (float) cx, (float) (y + dy));
            }
            cx += advance(g, f, ch) + tracking;
        }
        g.setClip(oldClip);
    }

    // ------------------------------------------------------------------ shapes

    static RoundRectangle2D rr(double x, double y, double w, double h, double r) {
        return new RoundRectangle2D.Double(x, y, w, h, r, r);
    }

    static void card(Graphics2D g, double x, double y, double w, double h, double r, Color fill, Color border, double borderW) {
        RoundRectangle2D s = rr(x, y, w, h, r);
        g.setColor(fill);
        g.fill(s);
        if (border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke((float) borderW));
            g.draw(s);
        }
    }

    /** A rounded "pill" badge with centered text. Returns its width. */
    static double pill(Graphics2D g, Font f, String s, double x, double cy, Color fill, Color text, double padX, double h) {
        double w = advance(g, f, s) + padX * 2;
        g.setColor(fill);
        g.fill(rr(x, cy - h / 2, w, h, h));
        java.awt.FontMetrics fm = g.getFontMetrics(f);
        Fx.text(g, f, s, x + padX, cy - (fm.getAscent() + fm.getDescent()) / 2.0 + fm.getAscent(), 0, text);
        return w;
    }

    // ------------------------------------------------------------------ images

    /** Draws an image centered at (cx, cy) with scale, rotation (radians) and opacity. */
    static void image(Graphics2D g, BufferedImage img, double cx, double cy, double sx, double sy, double rot, double a) {
        if (a <= 0.001 || sx == 0 || sy == 0) return;
        AffineTransform old = g.getTransform();
        Composite oc = g.getComposite();
        g.translate(cx, cy);
        if (rot != 0) g.rotate(rot);
        g.scale(sx, sy);
        if (a < 1) g.setComposite(opacity(a));
        g.drawImage(img, -img.getWidth() / 2, -img.getHeight() / 2, null);
        g.setComposite(oc);
        g.setTransform(old);
    }

    /**
     * Draws an image on a 3D-rotated plane (perspective projection), by
     * splitting it into a grid of small affine-mapped cells. rotX tilts the top
     * away, rotY turns it sideways (radians). (cx, cy) = screen centre,
     * scale = size at zero rotation.
     */
    static void perspective(Graphics2D g, BufferedImage img, double cx, double cy, double scale,
                            double rotX, double rotY, double alphaV, int cells) {
        int iw = img.getWidth(), ih = img.getHeight();
        double focal = 1800;
        double cosX = Math.cos(rotX), sinX = Math.sin(rotX), cosY = Math.cos(rotY), sinY = Math.sin(rotY);
        int nx = cells, ny = Math.max(4, (int) Math.round(cells * ih / (double) iw));
        double[][] px = new double[ny + 1][nx + 1], py = new double[ny + 1][nx + 1];
        for (int j = 0; j <= ny; j++) {
            for (int i = 0; i <= nx; i++) {
                double x = (i / (double) nx - 0.5) * iw * scale;
                double y = (j / (double) ny - 0.5) * ih * scale;
                // rotate around Y then X
                double x1 = x * cosY, z1 = -x * sinY;
                double y2 = y * cosX - z1 * sinX, z2 = y * sinX + z1 * cosX;
                double k = focal / (focal + z2);
                px[j][i] = cx + x1 * k;
                py[j][i] = cy + y2 * k;
            }
        }
        Composite oc = g.getComposite();
        if (alphaV < 1) g.setComposite(opacity(alphaV));
        AffineTransform old = g.getTransform();
        double cw = iw / (double) nx, ch = ih / (double) ny;
        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                double x0 = px[j][i], y0 = py[j][i];
                double ax = (px[j][i + 1] - x0) / cw, ay = (py[j][i + 1] - y0) / cw;
                double bx = (px[j + 1][i] - x0) / ch, by = (py[j + 1][i] - y0) / ch;
                AffineTransform at = new AffineTransform(ax, ay, bx, by, x0, y0);
                g.setTransform(old);
                g.transform(at);
                int sx = (int) Math.floor(i * cw), sy = (int) Math.floor(j * ch);
                int ex = (int) Math.min(iw, Math.ceil((i + 1) * cw) + 1), ey = (int) Math.min(ih, Math.ceil((j + 1) * ch) + 1);
                g.drawImage(img, 0, 0, ex - sx, ey - sy, sx, sy, ex, ey, null);
            }
        }
        g.setTransform(old);
        g.setComposite(oc);
    }

    /** Screen position of an image point after the same projection as perspective(). */
    static double[] project(double u, double v, int iw, int ih, double cx, double cy, double scale, double rotX, double rotY) {
        double focal = 1800;
        double x = (u / iw - 0.5) * iw * scale, y = (v / ih - 0.5) * ih * scale;
        double x1 = x * Math.cos(rotY), z1 = -x * Math.sin(rotY);
        double y2 = y * Math.cos(rotX) - z1 * Math.sin(rotX), z2 = y * Math.sin(rotX) + z1 * Math.cos(rotX);
        double k = focal / (focal + z2);
        return new double[]{cx + x1 * k, cy + y2 * k};
    }
}
