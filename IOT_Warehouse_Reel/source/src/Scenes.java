import static java.lang.Math.PI;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import ui.components.ModuleIcon;

/** The choreography: every scene of the reel, driven by the Score timeline. */
final class Scenes {

    static final int W = Assets.W, H = Assets.H;
    static final double CX = W / 2.0, CY = H / 2.0;
    static final Color AMBER = Assets.AMBER, TEXT = Assets.TEXT, MUTED = Assets.MUTED, DIM = Assets.DIM;

    /** Per-thread scratch images. */
    static final class Ctx {
        final BufferedImage canvas = new BufferedImage(1442, 900, BufferedImage.TYPE_INT_ARGB_PRE);
        final BufferedImage glint = new BufferedImage(1089, 501, BufferedImage.TYPE_INT_ARGB_PRE);
        final BufferedImage strip = new BufferedImage(1640, 40, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    // ================================================================== frame

    static void render(Graphics2D g, double t, Ctx ctx) {
        Assets.hq(g);
        g.drawImage(Assets.background, 0, 0, null);
        backgroundLife(g, t);
        AffineTransform base = g.getTransform();
        double[] sh = shake(t);
        g.translate(sh[0], sh[1]);

        if (t < 4.0) intro(g, t);
        if (t >= 3.95 && t < 7.92) logoScene(g, t, ctx);
        if (t >= 7.92 && t < 12.1) dashboard(g, t);
        if (t >= 11.82 && t < 15.82) moves(g, t, ctx);
        if (t >= 15.6 && t < 18.0) breakdown(g, t);
        if (t >= 18.0 && t < 22.06) features(g, t);
        if (t >= 21.85 && t < 26.0) assembly(g, t, ctx);
        if (t >= 25.94) outro(g, t, ctx);

        g.setTransform(base);
        stripeWipe(g, t);
        for (double ti : Score.IMPACTS) impact(g, t, ti);
        if (t > 29.15) {
            g.setColor(Fx.alpha(Color.BLACK, Fx.smooth(Fx.prog(t, 29.15, 0.85))));
            g.fillRect(0, 0, W, H);
        }
    }

    static double[] shake(double t) {
        double a = 0;
        for (double ti : Score.IMPACTS) if (t >= ti) a += 18 * Math.exp(-(t - ti) / 0.16);
        if (Score.kickSection(t)) a += 1.4 * Score.kickPulse(t, 0.08);
        return new double[]{a * Fx.noise(t * 46, 3), a * Fx.noise(t * 46, 9)};
    }

    // ================================================================== background life

    static void backgroundLife(Graphics2D g, double t) {
        Composite oc = g.getComposite();
        // dot grid drifting slowly (parallax floor)
        double dotsA = t < 4 ? 0.55 * Fx.prog(t, 0.3, 1.2) : t > 26 ? 0.25 : 0.4;
        if (t >= 15.6 && t < 18) dotsA = 0.5;
        g.setComposite(Fx.opacity(dotsA));
        int ox = (int) Math.floor((t * 9) % 48), oy = (int) Math.floor((t * 4) % 48);
        g.drawImage(Assets.dots, -ox, -oy, null);
        // breathing amber light from the centre, pulsing with the kick
        double kick = Score.kickSection(t) ? Score.kickPulse(t, 0.22) : 0;
        double glow = 0.05 + 0.07 * kick;
        if (t >= 4 && t < 8) glow += 0.06;
        if (t >= 15.6 && t < 18) glow += 0.1 * Fx.prog(t, 16, 2);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Fx.clamp01(glow)));
        g.drawImage(Assets.glowAmber, (int) CX - 1000, (int) CY - 640, 2000, 1280, null);
        g.setComposite(oc);
        // dust / bokeh particles with depth
        for (int i = 0; i < 80; i++) {
            double z = 0.15 + 0.85 * Fx.hash(i);
            double x = ((Fx.hash(i + 101) * (W + 200) + t * (6 + 26 * z)) % (W + 200)) - 100;
            double y = ((Fx.hash(i + 202) * (H + 200) - t * (10 + 30 * z)) % (H + 200));
            if (y < 0) y += H + 200;
            y -= 100;
            double tw = 0.55 + 0.45 * Math.sin(t * (1.3 + z) + i);
            double r = 1.2 + 3.2 * z * z;
            g.setColor(Fx.alpha(Assets.AMBER_LIGHT, (0.06 + 0.32 * z) * tw));
            g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        }
        for (int i = 0; i < 9; i++) {
            double x = (Fx.hash(i + 900) * W + t * 12 * (i % 2 == 0 ? 1 : -1)) % W;
            double y = Fx.hash(i + 950) * H;
            double s = 120 + 220 * Fx.hash(i + 990);
            g.setComposite(Fx.opacity(0.05 + 0.03 * Math.sin(t * 0.7 + i)));
            g.drawImage(Assets.glowAmber, (int) (x - s / 2), (int) (y - s / 2), (int) s, (int) s, null);
        }
        g.setComposite(oc);
    }

    // ================================================================== 1. INTRO (0 - 4)

    static void intro(Graphics2D g, double t) {
        hudCorners(g, t);
        // terminal boot text, bottom-left
        double ta = 1 - Fx.prog(t, 3.2, 0.4);
        String l1 = "> booting I.O.T WAREHOUSE", l2 = "> 3 warehouses  |  25 products  |  5,223 units on hand";
        int n1 = (int) Fx.clamp((t - 0.2) * 42, 0, l1.length());
        int n2 = (int) Fx.clamp((t - 0.95) * 52, 0, l2.length());
        boolean cursor = ((int) (t * 4)) % 2 == 0;
        Font mono = Assets.mono(22);
        if (n1 > 0) Fx.text(g, mono, l1.substring(0, n1) + (n2 == 0 && cursor ? "_" : ""), 130, 940, 0, Fx.alpha(AMBER, 0.85 * ta));
        if (n2 > 0) Fx.text(g, mono, l2.substring(0, n2) + (cursor ? "_" : ""), 130, 975, 0, Fx.alpha(MUTED, 0.85 * ta));

        // the first horizon line
        double lp = Fx.outExpo(Fx.prog(t, 0.08, 1.0));
        double la = 1 - Fx.prog(t, 1.15, 0.5);
        if (la > 0) {
            double lw = 1300 * lp;
            g.setColor(Fx.alpha(AMBER, la));
            g.fill(new Rectangle2D.Double(CX - lw / 2, CY - 1.5, lw, 3));
            g.setColor(Fx.alpha(Color.WHITE, la * 0.9));
            g.fill(new Rectangle2D.Double(CX - lw * 0.3, CY - 0.5, lw * 0.6, 1));
            Fx.image(g, Assets.glowWhite, CX, CY, lw / 256.0 * 0.5, 0.12, 0, la * 0.6);
        }

        // isometric crates popping in on the hi-hats, then sucked into the centre
        AffineTransform old = g.getTransform();
        double z = 1 + 0.10 * Fx.inOutCubic(Fx.prog(t, 1.0, 2.6));
        g.translate(CX, CY);
        g.rotate(-0.025 + 0.03 * Fx.inOutCubic(Fx.prog(t, 1.0, 2.6)));
        g.scale(z, z);
        g.translate(-CX, -CY);
        List<int[]> boxes = crateLayout();
        double s = 64, dx = s * Math.cos(PI / 6), dy = s * Math.sin(PI / 6);
        double ox = CX, oy = 400;
        for (int k = 0; k < boxes.size(); k++) {
            int[] b = boxes.get(k);
            double tk = Score.INTRO_BOXES[Math.min(k, Score.INTRO_BOXES.length - 1)];
            double p = Fx.prog(t, tk, 0.3);
            if (p <= 0) continue;
            double bx = ox + (b[0] - b[1]) * dx;
            double by = oy + (b[0] + b[1]) * dy - b[2] * s;
            double pop = Fx.outBack(p, 2.2);
            double drop = -70 * (1 - Fx.outCubic(p));
            double flash = Math.exp(-Math.max(0, t - tk - 0.12) / 0.14) * (t > tk + 0.05 ? 1 : 0);
            // implosion into the centre
            double dist = Math.hypot(bx - CX, by - CY);
            double e = Fx.inExpo(Fx.prog(t, 3.3 + dist / 3000.0, 0.55));
            double ang = e * 2.2;
            double rx = (bx - CX) * Math.cos(ang) - (by - CY) * Math.sin(ang);
            double ry = (bx - CX) * Math.sin(ang) + (by - CY) * Math.cos(ang);
            double px = CX + rx * (1 - e), py = CY + ry * (1 - e) + drop;
            double scale = pop * (1 - 0.9 * e);
            double alphaV = Math.min(1, p * 4) * (1 - Fx.prog(t, 3.8, 0.075));
            crate(g, px, py, s, scale, e * 4, flash, alphaV);
        }
        g.setTransform(old);

        // energy core growing with the riser, then the silence before the drop
        if (t > 2.6 && t < 3.875) {
            double c = Fx.inCubic(Fx.prog(t, 2.6, 1.27));
            Fx.image(g, Assets.glowAmber, CX, CY, 0.4 + 1.9 * c, 0.4 + 1.9 * c, 0, 0.25 + 0.65 * c);
            Fx.image(g, Assets.glowWhite, CX, CY, 0.3 + 1.2 * c, 0.3 + 1.2 * c, 0, 0.2 + 0.8 * c);
        }
        if (t >= 3.875 && t < 4.0) {
            double q = Fx.prog(t, 3.875, 0.125);
            Fx.image(g, Assets.glowWhite, CX, CY, 0.12 + 0.3 * q, 0.12 + 0.3 * q, 0, 0.9);
            g.setColor(Fx.alpha(Color.WHITE, 0.6 * q));
            g.fill(new Rectangle2D.Double(CX - 500 * q, CY - 1, 1000 * q, 2));
        }
    }

    static List<int[]> crateLayout() {
        List<int[]> out = new ArrayList<>();
        for (int sum = 0; sum <= 10; sum++) {
            for (int i = 0; i <= 5; i++) {
                int j = sum - i;
                if (j < 0 || j > 5) continue;
                out.add(new int[]{i, j, 0});
                if ((i == 2 || i == 3) && (j == 2 || j == 3)) out.add(new int[]{i, j, 1});
            }
        }
        // stacked boxes must be drawn after the ones below them
        return out;
    }

    /** An isometric crate in the colors of the cube inside the logo's "O". */
    static void crate(Graphics2D g, double x, double y, double s, double scale, double rot, double flash, double a) {
        if (scale <= 0.01 || a <= 0.01) return;
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        if (rot != 0) g.rotate(rot);
        g.scale(scale, scale);
        double dx = s * Math.cos(PI / 6), dy = s * Math.sin(PI / 6);
        // (0,0) = bottom centre, top centre at (0,-s)
        Path2D top = poly(0, -s - dy, dx, -s, 0, -s + dy, -dx, -s);
        Path2D left = poly(-dx, -s, 0, -s + dy, 0, dy, -dx, 0);
        Path2D right = poly(0, -s + dy, dx, -s, dx, 0, 0, dy);
        Color topC = Fx.mix(new Color(246, 188, 62), Color.WHITE, flash * 0.7);
        g.setColor(Fx.alpha(topC, a)); g.fill(top);
        g.setColor(Fx.alpha(Fx.mix(new Color(232, 152, 6), Color.WHITE, flash * 0.35), a)); g.fill(left);
        g.setColor(Fx.alpha(Fx.mix(new Color(188, 116, 0), Color.WHITE, flash * 0.3), a)); g.fill(right);
        // packing tape across the top and down the front
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.setColor(Fx.alpha(new Color(214, 140, 10), a));
        g.draw(new Line2D.Double(-dx / 2, -s - dy / 2, dx / 2, -s + dy / 2));
        g.draw(new Line2D.Double(dx / 2, -s + dy / 2, dx / 2, dy / 2));
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(Fx.alpha(new Color(110, 64, 0), a * 0.9));
        g.draw(top); g.draw(left); g.draw(right);
        g.setTransform(old);
    }

    static Path2D poly(double... p) {
        Path2D path = new Path2D.Double();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.closePath();
        return path;
    }

    static void hudCorners(Graphics2D g, double t) {
        double a = Fx.prog(t, 0.2, 0.5) * (1 - Fx.prog(t, 3.5, 0.3));
        if (a <= 0) return;
        double grow = Fx.outExpo(Fx.prog(t, 0.2, 0.8)) * 60;
        g.setStroke(new BasicStroke(2f));
        g.setColor(Fx.alpha(AMBER, 0.7 * a));
        double m = 90;
        double[][] corners = {{m, m, 1, 1}, {W - m, m, -1, 1}, {m, H - m, 1, -1}, {W - m, H - m, -1, -1}};
        for (double[] c : corners) {
            g.draw(new Line2D.Double(c[0], c[1], c[0] + c[2] * grow, c[1]));
            g.draw(new Line2D.Double(c[0], c[1], c[0], c[1] + c[3] * grow));
        }
        Fx.text(g, Assets.mono(16), "IOT.WMS // v1.0", W - m - 170, m + 30, 1, Fx.alpha(MUTED, 0.7 * a));
        Fx.text(g, Assets.mono(16), String.format("T+%05.2f", frameTime(t)), m + 14, m + 30, 1, Fx.alpha(MUTED, 0.7 * a));
    }

    // ================================================================== 2. LOGO (4 - 8)

    static final double LCX = 544.5, LCY = 250.5;

    static void logoScene(Graphics2D g, double t, Ctx ctx) {
        double move = Fx.inOutExpo(Fx.prog(t, 5.7, 0.55));
        double cx = CX, cy = Fx.lerp(505, 292, move);
        double s = Fx.lerp(0.86, 0.47, move);
        if (t >= 4.4) s *= 1 + 0.018 * Score.kickPulse(t, 0.16);

        // warm light behind the mark
        double kick = Score.kickPulse(t, 0.2);
        Fx.image(g, Assets.glowAmber, cx, cy, 3.4 * s, 1.8 * s, 0, 0.22 + 0.14 * kick);

        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.scale(s, s);
        g.translate(-LCX, -LCY);
        drawLogoPieces(g, t, 4.0);
        // light sweep across the assembled mark
        if (t > 5.0 && t < 5.75) glint(g, t, 5.0, 0.7, ctx);
        g.setTransform(old);

        // tagline: kinetic type on the beats
        if (t >= 5.95) {
            Font f1 = Assets.black(84), f3 = Assets.black(108);
            double lineOut = Fx.inCubic(Fx.prog(t, 7.62, 0.3));
            AffineTransform o2 = g.getTransform();
            g.translate(0, -60 * lineOut);
            String a = "EVERY BOX.", b = "EVERY WAREHOUSE.", c = "IN REAL TIME.";
            Fx.riseText(g, f1, a, CX - Fx.width(g, f1, a, 3) / 2, 600, 3, TEXT, t, 6.0, 0.014, 0.5);
            Fx.riseText(g, f1, b, CX - Fx.width(g, f1, b, 3) / 2, 700, 3, TEXT, t, 6.5, 0.014, 0.5);
            // punch on "IN REAL TIME."
            double punch = 1 + 0.16 * Math.exp(-Math.max(0, t - 7.0) / 0.09) * (t >= 7.0 ? 1 : 0);
            g.translate(CX, 830);
            g.scale(punch, punch);
            g.translate(-CX, -830);
            double cw = Fx.width(g, f3, c, 4);
            Fx.riseText(g, f3, c, CX - cw / 2, 840, 4, AMBER, t, 7.0, 0.012, 0.4);
            double ul = Fx.outExpo(Fx.prog(t, 7.08, 0.35));
            if (ul > 0) {
                g.setColor(AMBER);
                g.fill(Fx.rr(CX - cw / 2, 862, cw * ul, 9, 9));
            }
            g.setTransform(o2);
        }
    }

    /**
     * Draws the 20 logo layers with their entrance animations (logo coordinates).
     * t0 = moment of the drop.
     */
    static void drawLogoPieces(Graphics2D g, double t, double t0) {
        // swooshes: drawn on along the orbit, with a hot comet head
        swoosh(g, t, "swoosh_left", t0 + 0.28, 0.36, Math.toRadians(192), Math.toRadians(94));
        swoosh(g, t, "swoosh_right", t0 + 0.55, 0.36, Math.toRadians(12), Math.toRadians(-66));

        // the O: two halves slam together
        double eo = Fx.spring(t - t0, 17, 7);
        double ao = Fx.clamp01((t - t0) * 9);
        piece(g, "O_top", 0, -400 * (1 - eo), 1, 1, 0, ao);
        piece(g, "O_bottom", 0, 400 * (1 - eo), 1, 1, 0, ao);

        // I and T fall in with squash & stretch
        dropLetter(g, t, "I", t0 + 0.06);
        dropLetter(g, t, "T", t0 + 0.14);

        // dots pop
        dotPop(g, t, "dot1", t0 + 0.30);
        dotPop(g, t, "dot2", t0 + 0.36);

        // the cube assembles from its three faces, then punches
        double tc = t0 + 0.2;
        double ec = Fx.spring(t - tc, 15, 6.5), ac = Fx.clamp01((t - tc) * 7);
        double punch = 1 + 0.28 * Math.exp(-Math.max(0, t - (tc + 0.42)) / 0.09) * (t >= tc + 0.42 ? 1 : 0);
        AffineTransform old = g.getTransform();
        g.translate(520, 217);
        g.scale(punch, punch);
        g.translate(-520, -217);
        piece(g, "cube_top", 0, -300 * (1 - ec), 1, 1, -0.9 * (1 - ec), ac);
        piece(g, "cube_left", -280 * (1 - ec), 210 * (1 - ec), 1, 1, 0.7 * (1 - ec), ac);
        piece(g, "cube_right", 280 * (1 - ec), 210 * (1 - ec), 1, 1, -0.7 * (1 - ec), ac);
        g.setTransform(old);

        // "warehouse": letters rise and the tracking tightens
        for (int i = 0; i < 9; i++) {
            String name = "w" + i + "_" + "warehouse".charAt(i);
            Assets.Piece p = Assets.LOGO.get(name);
            double tt = t0 + 0.55 + 0.042 * i;
            double e = Fx.outBack(Fx.prog(t, tt, 0.45), 2.0);
            double spread = (p.cx() - LCX) * 0.7 * (1 - Fx.outExpo(Fx.prog(t, tt, 0.7)));
            piece(g, name, spread, 90 * (1 - e), 1, 1, 0, Fx.clamp01((t - tt) * 5));
        }
    }

    static void piece(Graphics2D g, String name, double dx, double dy, double sx, double sy, double rot, double a) {
        Assets.Piece p = Assets.LOGO.get(name);
        Fx.image(g, p.img, p.cx() + dx, p.cy() + dy, sx, sy, rot, a);
    }

    static void dropLetter(Graphics2D g, double t, String name, double tt) {
        Assets.Piece p = Assets.LOGO.get(name);
        double fall = 0.17;
        double dy = t < tt + fall ? -720 * (1 - Fx.inCubic(Fx.prog(t, tt, fall))) : 0;
        double tau = t - (tt + fall);
        double sq = tau > 0 ? Math.exp(-9 * tau) * Math.cos(30 * tau) : 0;
        double sy = 1 - 0.24 * sq, sx = 1 + 0.16 * sq;
        if (t < tt) return;
        // pivot at the bottom of the letter
        double bx = p.cx(), by = p.y + p.img.getHeight();
        AffineTransform old = g.getTransform();
        g.translate(bx, by + dy);
        g.scale(sx, sy);
        g.translate(-bx, -by);
        Fx.image(g, p.img, p.cx(), p.cy(), 1, 1, 0, Fx.clamp01((t - tt) * 10));
        g.setTransform(old);
    }

    static void dotPop(Graphics2D g, double t, String name, double tt) {
        double e = Fx.outBack(Fx.prog(t, tt, 0.32), 3.0);
        if (e <= 0) return;
        piece(g, name, 0, 0, e, e, 0, 1);
    }

    /** Reveals a swoosh by sweeping a wedge around the orbit centre. */
    static void swoosh(Graphics2D g, double t, String name, double tt, double dur, double from, double to) {
        double p = Fx.inOutCubic(Fx.prog(t, tt, dur));
        if (p <= 0) return;
        double ocx = 545, ocy = 210;
        double a = Fx.lerp(from, to, p);
        Shape clip = g.getClip();
        Path2D wedge = new Path2D.Double();
        wedge.moveTo(ocx, ocy);
        int steps = 48;
        for (int i = 0; i <= steps; i++) {
            double ang = Fx.lerp(from + Math.signum(from - to) * 0.05, a, i / (double) steps);
            wedge.lineTo(ocx + Math.cos(ang) * 1500, ocy + Math.sin(ang) * 1500);
        }
        wedge.closePath();
        if (p < 1) g.clip(wedge);
        Assets.Piece pc = Assets.LOGO.get(name);
        g.drawImage(pc.img, (int) pc.x, (int) pc.y, null);
        g.setClip(clip);
        // comet head on the ellipse
        if (p < 1) {
            double rx = 523, ry = 188;
            double r = 1 / Math.sqrt(Math.pow(Math.cos(a) / rx, 2) + Math.pow(Math.sin(a) / ry, 2));
            double hx = ocx + Math.cos(a) * r, hy = ocy + Math.sin(a) * r;
            Fx.image(g, Assets.glowWhite, hx, hy, 0.55, 0.55, 0, 0.95 * (1 - p * 0.3));
            Fx.image(g, Assets.glowAmber, hx, hy, 0.9, 0.9, 0, 0.6);
        }
    }

    /** A bright diagonal band sweeping across the logo (masked to the logo's shape). */
    static void glint(Graphics2D g, double t, double t0, double dur, Ctx ctx) {
        double p = Fx.inOutCubic(Fx.prog(t, t0, dur));
        BufferedImage img = ctx.glint;
        Graphics2D gg = img.createGraphics();
        gg.setComposite(AlphaComposite.Clear);
        gg.fillRect(0, 0, img.getWidth(), img.getHeight());
        gg.setComposite(AlphaComposite.SrcOver);
        gg.drawImage(Assets.logoFull, 0, 0, null);
        gg.setComposite(AlphaComposite.SrcIn);
        double x = Fx.lerp(-300, 1400, p);
        gg.setPaint(new LinearGradientPaint((float) (x - 140), 0f, (float) (x + 140), 120f, new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(255, 255, 255, 0), new Color(255, 250, 235, 235), new Color(255, 255, 255, 0)}));
        gg.fillRect(0, 0, img.getWidth(), img.getHeight());
        gg.dispose();
        Composite oc = g.getComposite();
        g.setComposite(Fx.opacity(0.9));
        g.drawImage(img, 0, 0, null);
        g.setComposite(oc);
    }

    // ================================================================== transitions

    static void stripeWipe(Graphics2D g, double t) {
        if (t < 7.6 || t > 8.25) return;
        double p = Fx.inOutCubic(Fx.prog(t, 7.62, 0.6));
        double pos = 2410 + 5600 * (p - 0.5);
        double skew = 0.45;
        AffineTransform old = g.getTransform();
        g.shear(-skew, 0);
        g.translate(skew * CY, 0);
        double body = 2900;
        g.setColor(new Color(226, 146, 0));
        g.fill(new Rectangle2D.Double(pos - body, -200, body, H + 400));
        // leading and trailing brand stripes (like the sidebar decoration)
        double[] lead = {60, 70, 160, 40, 240, 22};
        for (int i = 0; i < lead.length; i += 2) {
            g.setColor(Fx.alpha(AMBER, 0.95 - i * 0.12));
            g.fill(new Rectangle2D.Double(pos + lead[i], -200, lead[i + 1], H + 400));
        }
        g.setColor(Assets.AMBER_DARK);
        g.fill(new Rectangle2D.Double(pos - body - 50, -200, 34, H + 400));
        g.setColor(Fx.alpha(AMBER, 0.8));
        g.fill(new Rectangle2D.Double(pos - body - 120, -200, 22, H + 400));
        g.setTransform(old);
    }

    static void impact(Graphics2D g, double t, double ti) {
        if (t < ti || t > ti + 1.3) return;
        double d = t - ti;
        double cy = ti == 4.0 ? 505 : ti == 26.0 ? 470 : CY;
        // flash
        double flash = Math.exp(-d / 0.07);
        g.setColor(Fx.alpha(new Color(255, 236, 200), 0.85 * flash));
        g.fillRect(0, 0, W, H);
        // shockwave rings
        for (int k = 0; k < 2; k++) {
            double p = Fx.prog(d, 0.07 * k, 0.9);
            if (p <= 0 || p >= 1) continue;
            double r = 60 + 1500 * Fx.outExpo(p);
            g.setStroke(new BasicStroke((float) ((k == 0 ? 26 : 10) * (1 - p) + 1.5)));
            g.setColor(Fx.alpha(k == 0 ? AMBER : Color.WHITE, 0.75 * (1 - p)));
            g.draw(new Ellipse2D.Double(CX - r, cy - r * 0.92, r * 2, r * 1.84));
        }
        // sparks
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 56; i++) {
            double ang = Fx.hash(i * 7 + (int) ti) * 2 * PI;
            double sp = 500 + 1300 * Fx.hash(i * 13 + (int) ti);
            double dist = sp * (1 - Math.exp(-d * 3.2)) / 3.2 * 3.2;
            double len = 30 + 110 * Math.exp(-d * 4);
            double a = Math.max(0, 1 - d / (0.5 + 0.6 * Fx.hash(i + 77)));
            if (a <= 0) continue;
            double x1 = CX + Math.cos(ang) * (dist + 40), y1 = cy + Math.sin(ang) * (dist + 40) * 0.8;
            double x0 = CX + Math.cos(ang) * Math.max(0, dist + 40 - len), y0 = cy + Math.sin(ang) * Math.max(0, dist + 40 - len) * 0.8;
            g.setColor(Fx.alpha(i % 3 == 0 ? Color.WHITE : Assets.AMBER_LIGHT, a));
            g.draw(new Line2D.Double(x0, y0, x1, y1));
        }
    }

    // ================================================================== shared UI bits

    static void sectionHeader(Graphics2D g, double t, double t0, String num, String title, String subtitle) {
        double bar = Fx.outExpo(Fx.prog(t, t0, 0.4));
        g.setColor(AMBER);
        g.fill(new Rectangle2D.Double(140, 120 + 44 * (1 - bar), 6, 44 * bar));
        Font fNum = Assets.black(34), fTitle = Assets.semibold(30);
        Fx.riseText(g, fNum, num, 166, 156, 0, AMBER, t, t0 + 0.05, 0.02, 0.4);
        Fx.riseText(g, fTitle, title, 166 + Fx.advance(g, fNum, num) + 18, 155, 7, TEXT, t, t0 + 0.08, 0.012, 0.45);
        if (subtitle != null) {
            double a = Fx.prog(t, t0 + 0.25, 0.4);
            Fx.text(g, Assets.regular(20), subtitle, 168, 192 + 8 * (1 - Fx.outCubic(a)), 0.5, Fx.alpha(MUTED, a));
        }
    }

    static void ripple(Graphics2D g, double x, double y, double t, double t0, double maxR, Color c) {
        ripple(g, x, y, t, t0, maxR, c, 1.0);
    }

    static void ripple(Graphics2D g, double x, double y, double t, double t0, double maxR, Color c, double flat) {
        double p = Fx.prog(t, t0, 0.55);
        if (p <= 0 || p >= 1) return;
        double r = maxR * Fx.outExpo(p);
        g.setStroke(new BasicStroke((float) (4 * (1 - p) + 1)));
        g.setColor(Fx.alpha(c, 0.8 * (1 - p)));
        g.draw(new Ellipse2D.Double(x - r, y - r * flat, r * 2, r * 2 * flat));
    }

    static void icon(Graphics2D g, ModuleIcon.Type type, double cx, double cy, double size, double a, boolean black) {
        BufferedImage img = black ? Assets.ICON_BLACK.get(type) : Assets.ICON_AMBER.get(type);
        double sc = size / (Assets.ICON - 16.0);
        Fx.image(g, img, cx, cy, sc, sc, 0, a);
    }

    /** The frame's own time (all motion-blur sub-frames agree), for counters and other changing text. */
    static double frameTime(double t) { return Math.round(t * 60) / 60.0; }

    static String money(double v) { return "₱" + new DecimalFormat("#,##0.00").format(v); }

    static String num(double v) { return new DecimalFormat("#,##0").format(v); }

    // ================================================================== 3. DASHBOARD (8 - 12)

    static void dashboard(Graphics2D g, double t) {
        AffineTransform old = g.getTransform();
        double settle = Fx.outExpo(Fx.prog(t, 7.92, 0.6));
        double z = 1.07 - 0.07 * settle + 0.03 * Fx.inOutCubic(Fx.prog(t, 8.5, 3.2));
        double camX = -2700 * Fx.inExpo(Fx.prog(t, 11.69, 0.185));
        g.translate(CX + camX, CY);
        g.scale(z, z);
        g.rotate(Math.toRadians(-0.5 * Fx.inOutCubic(Fx.prog(t, 8.5, 3.2))));
        g.translate(-CX, -CY);

        sectionHeader(g, t, 7.95, "01", "LIVE DASHBOARD", "Real-time stock across every warehouse");

        String[] titles = {"ACTIVE WAREHOUSES", "ACTIVE PRODUCTS", "UNITS ON HAND", "STOCK VALUE"};
        String[] caps = {"Storage locations in use", "Items in the catalog", "In all warehouses", "Valued at standard unit cost"};
        double[] values = {3, 24, 5223, 1339310.00};
        Color[] accents = {AMBER, Assets.SLATE, Assets.CYAN, Assets.GREEN};
        ModuleIcon.Type[] icons = {ModuleIcon.Type.WAREHOUSES, ModuleIcon.Type.PRODUCTS, ModuleIcon.Type.INVENTORY, ModuleIcon.Type.REPORTS};
        for (int i = 0; i < 4; i++) {
            double ti = Score.CARD_POPS[i];
            double x = 140 + i * 415, y = 240, w = 395, h = 172;
            if (t < ti - 0.16) continue;
            double fall = Fx.inCubic(Fx.prog(t, ti - 0.16, 0.16));
            double dy = -240 * (1 - fall);
            double tau = t - ti;
            double sq = tau > 0 ? Math.exp(-10 * tau) * Math.cos(28 * tau) : 0;
            AffineTransform o = g.getTransform();
            g.translate(x + w / 2, y + h + dy);
            g.scale(1 + 0.07 * sq, 1 - 0.11 * sq);
            g.translate(-(x + w / 2), -(y + h));
            double glow = tau > 0 ? Math.exp(-tau / 0.25) : 0;
            Fx.card(g, x, y, w, h, 16, Assets.CARD, Fx.mix(Assets.CARD_BORDER, AMBER, glow), 1.5 + 1.5 * glow);
            g.setColor(accents[i]);
            g.fill(Fx.rr(x, y + 18, 5, h - 36, 5));
            Fx.text(g, Assets.bold(18), titles[i], x + 26, y + 44, 1.2, MUTED);
            double cv = values[i] * Fx.outExpo(Fx.prog(frameTime(t), ti, 0.95));
            String val = i == 3 ? money(cv) : num(Math.round(cv));
            Fx.text(g, Assets.bold(46), val, x + 24, y + 106, 0, TEXT);
            Fx.text(g, Assets.regular(19), caps[i], x + 26, y + 148, 0, MUTED);
            icon(g, icons[i], x + w - 46, y + 46, 34, 0.9, false);
            g.setTransform(o);
            // landing dust
            ripple(g, x + w / 2, y + h + 4, t, ti, 230, Fx.alpha(AMBER, 0.7), 0.12);
        }

        // stock vs capacity chart
        double ca = Fx.outExpo(Fx.prog(t, 9.7, 0.5));
        if (ca > 0) {
            double x = 140, y = 445 + 50 * (1 - ca), w = 810, h = 470;
            Composite oc = g.getComposite();
            g.setComposite(Fx.opacity(ca));
            Fx.card(g, x, y, w, h, 16, Assets.CARD, Assets.CARD_BORDER, 1.5);
            Fx.text(g, Assets.bold(26), "Stock vs Capacity by Warehouse", x + 30, y + 52, 0, AMBER);
            String[] names = {"Main Warehouse", "North Distribution Center", "South Distribution Center"};
            double[] pct = {72.7, 51.6, 54.2};
            String[] units = {"2,036 of 2,800 units", "1,290 of 2,500 units", "1,897 of 3,500 units"};
            for (int k = 0; k < 3; k++) {
                double ty = y + 125 + k * 112;
                double gp = Fx.prog(t, Score.BAR_GROW[k], 0.7);
                double grow = gp <= 0 ? 0 : Fx.spring(t - Score.BAR_GROW[k], 15, 6);
                Color bc = pct[k] >= 70 ? Assets.WARN : AMBER;
                Fx.text(g, Assets.bold(21), names[k], x + 30, ty, 0, TEXT);
                Fx.textRight(g, Assets.bold(21), String.format("%.1f%%", pct[k] * Math.min(1, Fx.outExpo(Fx.prog(frameTime(t), Score.BAR_GROW[k], 0.7)))), x + w - 30, ty, bc);
                double tw = w - 60;
                g.setColor(Assets.GRID);
                g.fill(Fx.rr(x + 30, ty + 14, tw, 20, 20));
                double fw = Math.max(0, tw * pct[k] / 100 * grow);
                if (fw > 2) {
                    g.setPaint(new GradientPaint((float) (x + 30), 0f, Fx.mix(bc, Color.WHITE, 0.1), (float) (x + 30 + fw), 0f, bc));
                    g.fill(Fx.rr(x + 30, ty + 14, fw, 20, 20));
                    Fx.image(g, Assets.glowWhite, x + 30 + fw - 6, ty + 24, 0.18, 0.18, 0, 0.6 * Math.exp(-Math.max(0, t - Score.BAR_GROW[k]) / 0.4));
                }
                Fx.text(g, Assets.regular(17), units[k], x + 30, ty + 60, 0, MUTED);
            }
            g.setComposite(oc);
        }

        // low stock alerts
        double aa = Fx.outExpo(Fx.prog(t, 10.2, 0.5));
        if (aa > 0) {
            double x = 970, y = 445 + 50 * (1 - aa), w = 810, h = 470;
            Composite oc = g.getComposite();
            g.setComposite(Fx.opacity(aa));
            Fx.card(g, x, y, w, h, 16, Assets.CARD, Assets.CARD_BORDER, 1.5);
            Fx.text(g, Assets.bold(26), "Low Stock Alerts", x + 30, y + 52, 0, AMBER);
            // bell with a red counter badge
            double bx = x + w - 60, by = y + 42;
            double ping = 0;
            for (double tp : Score.ALERT_PINGS) if (t >= tp) ping = Math.max(ping, Math.exp(-(t - tp) / 0.12));
            drawBell(g, bx, by, 1 + 0.25 * ping, Math.sin((t - 11) * 40) * ping * 0.25);
            g.setColor(Assets.RED);
            g.fill(new Ellipse2D.Double(bx + 8 - 3 * ping, by - 24 - 3 * ping, 26 + 6 * ping, 26 + 6 * ping));
            Fx.textCentered(g, Assets.bold(17), "4", bx + 21, by - 5, 0, Color.WHITE);
            for (double tp : Score.ALERT_PINGS) ripple(g, bx, by, t, tp, 70, Assets.RED);

            String[][] rows = {{"OFF-2003", "Heavy-Duty Stapler", "8 / 15"}, {"ELE-1001", "LED Monitor 24-inch", "8 / 10"},
                {"FNB-4003", "Canned Tuna 155g (Case of 48)", "10 / 15"}, {"PKG-6003", "Stretch Film 18-inch", "12 / 20"}};
            for (int k = 0; k < 4; k++) {
                double tr = 10.5 + 0.0625 * k;
                double rp = Fx.outExpo(Fx.prog(t, tr, 0.45));
                if (rp <= 0) continue;
                double ry = y + 96 + k * 88, rx = x + 24 + 160 * (1 - rp);
                Composite o2 = g.getComposite();
                g.setComposite(Fx.opacity(rp * aa));
                g.setColor(k % 2 == 0 ? Assets.ROW_A : Assets.ROW_B);
                g.fill(Fx.rr(rx, ry, w - 48, 70, 12));
                Fx.text(g, Assets.mono(19), rows[k][0], rx + 18, ry + 43, 0, MUTED);
                Fx.text(g, Assets.semibold(21), rows[k][1], rx + 150, ry + 44, 0, TEXT);
                Fx.textRight(g, Assets.bold(20), rows[k][2], rx + w - 190, ry + 44, TEXT);
                double bp = Fx.outBack(Fx.prog(t, tr + 0.12, 0.3), 2.5);
                if (bp > 0) {
                    AffineTransform o3 = g.getTransform();
                    g.translate(rx + w - 110, ry + 35);
                    g.scale(bp, bp);
                    Fx.pill(g, Assets.black(15), "LOW", -30, 0, Assets.WARN, Color.BLACK, 14, 30);
                    g.setTransform(o3);
                }
                g.setComposite(o2);
            }
            g.setComposite(oc);
        }
        g.setTransform(old);
    }

    static void drawBell(Graphics2D g, double x, double y, double s, double rot) {
        AffineTransform o = g.getTransform();
        g.translate(x, y);
        g.rotate(rot);
        g.scale(s, s);
        Path2D bell = new Path2D.Double();
        bell.moveTo(-16, 10);
        bell.curveTo(-12, 6, -13, -4, -12, -8);
        bell.curveTo(-10, -20, 10, -20, 12, -8);
        bell.curveTo(13, -4, 12, 6, 16, 10);
        bell.closePath();
        g.setColor(AMBER);
        g.fill(bell);
        g.fill(new Ellipse2D.Double(-4, 11, 8, 7));
        g.setTransform(o);
    }

    // ================================================================== 4. STOCK IN MOTION (12 - 16)

    static void moves(Graphics2D g, double t, Ctx ctx) {
        AffineTransform old = g.getTransform();
        double camX = 2700 * (1 - Fx.outExpo(Fx.prog(t, 11.875, 0.4)));
        double out = Fx.inCubic(Fx.prog(t, 15.45, 0.35));
        double z = 1 - 0.14 * out;
        g.translate(CX + camX, CY);
        g.scale(z, z);
        g.translate(-CX, -CY);
        Composite oc = g.getComposite();
        if (out > 0) g.setComposite(Fx.opacity(1 - out));

        sectionHeader(g, t, 11.95, "02", "STOCK IN MOTION", "Receiving, releasing and transfers update every warehouse instantly");

        String[] titles = {"RECEIVING", "RELEASING", "TRANSFERS"};
        String[] subs = {"Stock in from suppliers", "Stock out to customers", "WH-001 → WH-002"};
        Color[] colors = {Assets.GREEN, Assets.WARN, AMBER};
        for (int i = 0; i < 3; i++) {
            double ti = Score.MOVE_TILES[i];
            if (t < ti) continue;
            double e = Fx.spring(t - ti, 16, 7);
            double x = 140 + i * 570, y = 232, w = 540, h = 300;
            AffineTransform o = g.getTransform();
            g.translate(x + w / 2, y + h / 2);
            g.rotate(Math.toRadians((i % 2 == 0 ? -7 : 7) * (1 - e)));
            double sc = 0.55 + 0.45 * e;
            g.scale(sc, sc);
            g.translate(-(x + w / 2), -(y + h / 2));
            Composite o2 = g.getComposite();
            g.setComposite(Fx.opacity(Fx.clamp01((t - ti) * 6) * (1 - out)));
            double glow = Math.exp(-(t - ti) / 0.3);
            Fx.card(g, x, y, w, h, 18, Assets.CARD, Fx.mix(Assets.CARD_BORDER, AMBER, glow), 1.5 + glow * 2);
            moveIcon(g, i, x + 120, y + 140, 150, t, ti);
            Fx.text(g, Assets.black(34), titles[i], x + 222, y + 118, 2, TEXT);
            Fx.text(g, Assets.regular(19), subs[i], x + 224, y + 152, 0, MUTED);
            // counter chip ticking on every beat
            double cp = Fx.outBack(Fx.prog(t, ti + 0.3, 0.35), 2.4);
            if (cp > 0) {
                int k = (int) Math.floor((frameTime(t) - ti) / Score.BEAT) + 1;
                double bump = 1 + 0.12 * Math.exp(-((t - ti) % Score.BEAT) / 0.07);
                String label = i == 0 ? "+" + (20 * k) + " UNITS" : i == 1 ? "−" + (3 * k) + " UNITS" : (5 * k) + " UNITS MOVED";
                AffineTransform o3 = g.getTransform();
                g.translate(x + 224, y + 212);
                g.scale(cp * bump, cp * bump);
                Fx.pill(g, Assets.black(19), label, 0, 0, Fx.alpha(colors[i], 0.18), colors[i], 16, 42);
                g.setTransform(o3);
            }
            g.setComposite(o2);
            g.setTransform(o);
        }

        // transaction history assembling row by row
        double ha = Fx.outExpo(Fx.prog(t, 13.3, 0.45));
        if (ha > 0) {
            double x = 140, y = 568 + 40 * (1 - ha), w = 1640, h = 420;
            Composite o2 = g.getComposite();
            g.setComposite(Fx.opacity(ha * (1 - out)));
            Fx.card(g, x, y, w, h, 16, Assets.CARD, Assets.CARD_BORDER, 1.5);
            g.setColor(Assets.TABLE_HEAD);
            g.fill(Fx.rr(x + 20, y + 20, w - 40, 46, 10));
            String[] heads = {"Reference No.", "Type", "Warehouse", "Supplier / Customer", "Units", "Status"};
            double[] cols = {44, 360, 540, 950, 1330, 1440};
            for (int c = 0; c < heads.length; c++) {
                if (c == 4) Fx.textRight(g, Assets.bold(19), heads[c], x + cols[c] + 40, y + 51, new Color(252, 243, 226));
                else Fx.text(g, Assets.bold(19), heads[c], x + cols[c], y + 51, 0, new Color(252, 243, 226));
            }
            String[][] rows = {
                {"RCV-20261010-001", "Receiving", "South Distribution Center", "CleanPro Industrial Supply", "320"},
                {"REL-20261010-001", "Release", "North Distribution Center", "Northpoint Retail Mart", "150"},
                {"TRF-20261010-001", "Transfer", "Main Warehouse", "To North Distribution Center", "30"},
                {"RCV-20261008-001", "Receiving", "Main Warehouse", "BuildRight Hardware Trading", "150"},
                {"REL-20261009-001", "Release", "Main Warehouse", "Metro Office Supplies", "72"},
                {"RCV-20261005-001", "Receiving", "Main Warehouse", "PrimeOffice Supply Co.", "40"},
                {"REL-20261007-001", "Release", "South Distribution Center", "Northpoint Retail Mart", "55"},
                {"TRF-20261005-001", "Transfer", "Main Warehouse", "To South Distribution Center", "25"},
            };
            for (int k = 0; k < rows.length; k++) {
                double tr = Score.ROW_SLIDES[k];
                double rp = Fx.outExpo(Fx.prog(t, tr, 0.38));
                if (rp <= 0) continue;
                double ry = y + 74 + k * 42, rx = x + 20 + 300 * (1 - rp);
                boolean voided = k == 5 && t >= Score.VOID_GLITCH;
                double gl = k == 5 ? Fx.prog(t, Score.VOID_GLITCH, 0.24) : 1;
                boolean glitching = k == 5 && gl > 0 && gl < 1;
                Composite o3 = g.getComposite();
                g.setComposite(Fx.opacity(rp * ha * (1 - out)));
                if (glitching) {
                    drawRowGlitched(g, ctx, rows[k], rx, ry, w - 40, k, t, tr);
                } else {
                    drawHistoryRow(g, rows[k], rx, ry, w - 40, k, voided, t, tr, cols, voided ? Math.exp(-(t - Score.VOID_GLITCH - 0.24) / 0.3) : 0);
                }
                g.setComposite(o3);
            }
            g.setComposite(o2);
        }
        g.setComposite(oc);
        g.setTransform(old);
    }

    static void drawHistoryRow(Graphics2D g, String[] r, double x, double y, double w, int k, boolean voided,
                               double t, double tr, double[] cols, double redFlash) {
        Color bg = k % 2 == 0 ? Assets.ROW_A : Assets.ROW_B;
        if (redFlash > 0) bg = Fx.mix(bg, new Color(90, 20, 20), redFlash);
        g.setColor(bg);
        g.fill(Fx.rr(x, y, w, 40, 8));
        double bx = x - 20;
        Fx.text(g, Assets.mono(19), r[0], bx + cols[0], y + 27, 0, TEXT);
        Color tc = r[1].equals("Receiving") ? Assets.GREEN : r[1].equals("Release") ? Assets.WARN : Assets.SLATE;
        Fx.text(g, Assets.bold(19), r[1], bx + cols[1], y + 27, 0, tc);
        Fx.text(g, Assets.regular(19), r[2], bx + cols[2], y + 27, 0, TEXT);
        Fx.text(g, Assets.regular(19), r[3], bx + cols[3], y + 27, 0, MUTED);
        Fx.textRight(g, Assets.bold(19), r[4], bx + cols[4] + 40, y + 27, TEXT);
        double bp = Fx.outBack(Fx.prog(t, tr + 0.1, 0.3), 2.6);
        if (bp > 0) {
            AffineTransform o = g.getTransform();
            g.translate(bx + cols[5] + 50, y + 20);
            g.scale(bp, bp);
            Fx.pill(g, Assets.black(14), voided ? "VOIDED" : "POSTED", -50, 0, voided ? Assets.RED : Assets.GREEN,
                    voided ? Color.WHITE : Color.BLACK, 14, 28);
            g.setTransform(o);
        }
    }

    /** RGB-split, sliced glitch while a row turns VOIDED. */
    static void drawRowGlitched(Graphics2D g, Ctx ctx, String[] r, double x, double y, double w, int k, double t, double tr) {
        BufferedImage img = ctx.strip;
        Graphics2D gg = img.createGraphics();
        Assets.hq(gg);
        gg.setComposite(AlphaComposite.Clear);
        gg.fillRect(0, 0, img.getWidth(), img.getHeight());
        gg.setComposite(AlphaComposite.SrcOver);
        boolean flip = ((int) (t * 30)) % 2 == 0;
        double[] cols = {44, 360, 540, 950, 1330, 1440};
        drawHistoryRow(gg, r, 0, 0, w, k, flip, t, tr - 1, cols, 0.6);
        gg.dispose();
        Composite oc = g.getComposite();
        int seed = (int) (t * 60);
        for (int s = 0; s < 4; s++) {
            int sy = s * 10, sh = 10;
            double off = (Fx.hash(seed * 4 + s) - 0.5) * 60;
            g.drawImage(img, (int) (x + off), (int) (y + sy), (int) (x + off + img.getWidth()), (int) (y + sy + sh),
                    0, sy, img.getWidth(), sy + sh, null);
        }
        g.setComposite(Fx.opacity(0.45));
        g.setColor(new Color(255, 40, 60, 90));
        g.fill(new Rectangle2D.Double(x - 8, y, w, 40));
        g.setColor(new Color(40, 220, 255, 70));
        g.fill(new Rectangle2D.Double(x + 8, y + 4, w, 32));
        g.setComposite(oc);
    }

    /** Big animated product icons (same geometry as the app's ModuleIcon, 16 x 16 grid). */
    static void moveIcon(Graphics2D g, int kind, double cx, double cy, double size, double t, double ti) {
        AffineTransform o = g.getTransform();
        double s = size / 16.0;
        g.translate(cx - size / 2, cy - size / 2);
        g.scale(s, s);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double ph = ((t - ti) % Score.BEAT) / Score.BEAT;     // 0..1 within each beat
        double since = t - ti;
        Color c = AMBER;
        if (kind == 0) {
            // tray bounces when the arrow lands
            double land = Math.exp(-ph * 10) * (since > Score.BEAT ? 1 : 0);
            AffineTransform o2 = g.getTransform();
            g.translate(8, 14.5);
            g.scale(1 + 0.08 * land, 1 - 0.18 * land);
            g.translate(-8, -14.5);
            g.setColor(c);
            Path2D tray = new Path2D.Double();
            tray.moveTo(1.5, 9.5); tray.lineTo(1.5, 14.5); tray.lineTo(14.5, 14.5); tray.lineTo(14.5, 9.5);
            g.draw(tray);
            g.setTransform(o2);
            double fall = Fx.inCubic(Fx.clamp01(ph / 0.55));
            double ay = -7 * (1 - fall);
            Shape clip = g.getClip();
            g.clip(new Rectangle2D.Double(-2, -2, 20, 15));
            g.setColor(c);
            g.draw(new Line2D.Double(8, 1.5 + ay, 8, 9.5 + ay));
            Path2D head = new Path2D.Double();
            head.moveTo(5, 6.5 + ay); head.lineTo(8, 9.5 + ay); head.lineTo(11, 6.5 + ay);
            g.draw(head);
            g.setClip(clip);
        } else if (kind == 1) {
            g.setColor(c);
            Path2D tray = new Path2D.Double();
            tray.moveTo(1.5, 9.5); tray.lineTo(1.5, 14.5); tray.lineTo(14.5, 14.5); tray.lineTo(14.5, 9.5);
            g.draw(tray);
            double rise = Fx.outCubic(ph);
            double ay = -9 * rise;
            Shape clip = g.getClip();
            g.clip(new Rectangle2D.Double(-2, -4, 20, 15));
            Composite oc = g.getComposite();
            g.setComposite(Fx.opacity(1 - Fx.prog(ph, 0.6, 0.4)));
            g.draw(new Line2D.Double(8, 9.5 + ay, 8, 1.5 + ay));
            Path2D head = new Path2D.Double();
            head.moveTo(5, 4.5 + ay); head.lineTo(8, 1.5 + ay); head.lineTo(11, 4.5 + ay);
            g.draw(head);
            g.setComposite(oc);
            // the next arrow already coming up from below
            g.setComposite(Fx.opacity(Fx.prog(ph, 0.5, 0.5)));
            double ay2 = 9 - 9 * Fx.outCubic(Fx.prog(ph, 0.5, 0.5));
            g.draw(new Line2D.Double(8, 9.5 + ay2, 8, 1.5 + ay2));
            Path2D head2 = new Path2D.Double();
            head2.moveTo(5, 4.5 + ay2); head2.lineTo(8, 1.5 + ay2); head2.lineTo(11, 4.5 + ay2);
            g.draw(head2);
            g.setComposite(oc);
            g.setClip(clip);
        } else {
            double sh = Fx.inOutCubic(ph) * 3;
            g.setColor(c);
            Shape clip = g.getClip();
            g.clip(new Rectangle2D.Double(0, 0, 16, 16));
            for (int rep = -1; rep <= 1; rep++) {
                double off = sh + rep * 16;
                g.draw(new Line2D.Double(1.5 + off, 5, 14 + off, 5));
                Path2D h1 = new Path2D.Double();
                h1.moveTo(11 + off, 2); h1.lineTo(14 + off, 5); h1.lineTo(11 + off, 8);
                g.draw(h1);
                g.draw(new Line2D.Double(14.5 - off, 11, 2 - off, 11));
                Path2D h2 = new Path2D.Double();
                h2.moveTo(5 - off, 8); h2.lineTo(2 - off, 11); h2.lineTo(5 - off, 14);
                g.draw(h2);
            }
            g.setClip(clip);
        }
        g.setTransform(o);
    }

    // ================================================================== 5. BREAKDOWN (16 - 18)

    static final ModuleIcon.Type[] MODULES = {ModuleIcon.Type.DASHBOARD, ModuleIcon.Type.INVENTORY, ModuleIcon.Type.RECEIVING,
        ModuleIcon.Type.RELEASING, ModuleIcon.Type.TRANSFER, ModuleIcon.Type.PRODUCTS, ModuleIcon.Type.CATEGORIES,
        ModuleIcon.Type.WAREHOUSES, ModuleIcon.Type.SUPPLIERS, ModuleIcon.Type.CUSTOMERS, ModuleIcon.Type.REPORTS, ModuleIcon.Type.USERS};
    static final String[] MODULE_NAMES = {"Dashboard", "Inventory", "Receiving", "Releasing", "Transfers", "Products",
        "Categories", "Warehouses", "Suppliers", "Customers", "Reports", "Users"};

    static void breakdown(Graphics2D g, double t) {
        double cy = 520;
        double rot = 0.55 * (t - 15.6) + 1.6 * Math.pow(Math.max(0, t - 16.8), 2.2);
        double implode = Fx.inExpo(Fx.prog(t, 17.6, 0.36));
        double rx = 760 * (1 - implode), ry = 255 * (1 - implode);
        // depth-sorted orbit
        Integer[] order = new Integer[12];
        double[] zs = new double[12];
        for (int i = 0; i < 12; i++) {
            order[i] = i;
            zs[i] = Math.sin(2 * PI * i / 12 + rot);
        }
        java.util.Arrays.sort(order, (a, b) -> Double.compare(zs[a], zs[b]));
        // the back half is drawn first, then the title, then the front half
        for (int pass = 0; pass < 2; pass++) {
            for (int idx : order) {
                if ((pass == 0) != (zs[idx] < 0)) continue;
                double tin = 15.8 + 0.03 * idx;
                double e = Fx.outBack(Fx.prog(t, tin, 0.5), 1.6);
                if (e <= 0) continue;
                double ang = 2 * PI * idx / 12 + rot;
                double x = CX + Math.cos(ang) * rx * e, y = cy + 45 + Math.sin(ang) * ry * e;
                double depth = (zs[idx] + 1) / 2;
                double sc = (0.62 + 0.5 * depth) * (1 - 0.6 * implode);
                double a = (0.35 + 0.65 * depth) * Math.min(1, e * 2);
                double hl = 0;
                for (int k = 0; k < Score.ORBIT_TICKS.length; k++) {
                    if (k % 12 == idx || (k + 8) % 12 == idx) {
                        double tk = Score.ORBIT_TICKS[k];
                        if (t >= tk) hl = Math.max(hl, Math.exp(-(t - tk) / 0.22));
                    }
                }
                AffineTransform o = g.getTransform();
                g.translate(x, y);
                g.scale(sc, sc);
                Composite oc = g.getComposite();
                g.setComposite(Fx.opacity(a));
                Fx.card(g, -52, -52, 104, 104, 22, Fx.mix(Assets.CARD, AMBER, hl), Fx.mix(Assets.CARD_BORDER, AMBER, hl), 2);
                icon(g, MODULES[idx], 0, 0, 56, 1, hl > 0.5);
                Fx.textCentered(g, Assets.semibold(17), MODULE_NAMES[idx], 0, 84, 0.5, Fx.alpha(TEXT, 0.9));
                g.setComposite(oc);
                g.setTransform(o);
            }
            if (pass == 0) breakdownTitle(g, t, cy, implode);
        }
        if (t > 17.0) {
            double c = Fx.inCubic(Fx.prog(t, 17.0, 0.97));
            Fx.image(g, Assets.glowAmber, CX, cy, 0.5 + 2.5 * c, 0.5 + 2.5 * c, 0, 0.5 * c);
            Fx.image(g, Assets.glowWhite, CX, cy, 0.2 + 1.5 * implode, 0.2 + 1.5 * implode, 0, implode);
        }
    }

    static void breakdownTitle(Graphics2D g, double t, double cy, double implode) {
        AffineTransform o = g.getTransform();
        double grow = 1 + 0.08 * Fx.inCubic(Fx.prog(t, 16.9, 0.75));
        double sc = grow * (1 - implode);
        if (sc <= 0.01) return;
        g.translate(CX, cy);
        g.scale(sc, sc);
        g.translate(-CX, -cy);
        Font small = Assets.semibold(28);
        double sa = Fx.prog(t, 16.15, 0.4);
        Fx.textCentered(g, small, "12 MODULES", CX, cy - 70, 14 + 10 * (1 - Fx.outExpo(sa)), Fx.alpha(AMBER, sa));
        Font big = Assets.black(132);
        String one = "ONE ", sys = "SYSTEM";
        double total = Fx.width(g, big, one + sys + ".", 2);
        double x0 = CX - total / 2;
        Fx.riseText(g, big, one, x0, cy + 70, 2, TEXT, t, 16.5, 0.03, 0.45);
        double x1 = x0 + Fx.width(g, big, one, 2) + 2;
        Fx.riseText(g, big, sys, x1, cy + 70, 2, TEXT, t, 16.75, 0.03, 0.45);
        double x2 = x1 + Fx.width(g, big, sys, 2) + 2;
        Fx.riseText(g, big, ".", x2, cy + 70, 0, AMBER, t, 17.0, 0, 0.3);
        g.setTransform(o);
    }

    // ================================================================== 6. FEATURES (18 - 22)

    static void features(Graphics2D g, double t) {
        if (t < 19.45) search(g, t);
        if (t >= 19.3 && t < 20.66) roles(g, t);
        if (t >= 20.38 && t < 21.45) reports(g, t);
        if (t >= 21.18) tech(g, t);
    }

    static final String[][] PRODUCTS = {
        {"FNB-4002", "3-in-1 Instant Coffee (Pack of 30)", "Food & Beverages", "110"},
        {"OFF-2001", "A4 Bond Paper 70gsm", "Office Supplies", "850"},
        {"CLN-5002", "Bleach 1 Gallon", "Cleaning Supplies", "50"},
        {"HRD-3002", "Claw Hammer 16oz", "Hardware & Tools", "105"},
        {"ELE-1004", "HDMI Cable 2m", "Electronics", "200"},
        {"PKG-6001", "Corrugated Box Medium", "Packaging Materials", "1,000"},
    };

    static void search(Graphics2D g, double t) {
        AffineTransform old = g.getTransform();
        double out = Fx.inCubic(Fx.prog(t, 19.25, 0.2));
        double z = 1 + 0.25 * out;
        g.translate(CX, CY);
        g.scale(z, z);
        g.translate(-CX, -CY);
        Composite oc = g.getComposite();
        g.setComposite(Fx.opacity(1 - out));
        sectionHeader(g, t, 18.02, "03", "FIND ANYTHING", "Search by SKU, name, category, reference or date");

        double fe = Fx.spring(t - 18.0, 18, 8);
        double fx = 460, fy = 250, fw = 1000, fh = 92;
        AffineTransform o = g.getTransform();
        g.translate(CX, fy + fh / 2);
        double fs = 1.25 - 0.25 * fe;
        g.scale(fs, fs);
        g.translate(-CX, -(fy + fh / 2));
        Fx.card(g, fx, fy, fw, fh, 22, Assets.INPUT, AMBER, 3);
        Fx.image(g, Assets.ICON_MUTED.get(ModuleIcon.Type.SEARCH), fx + 52, fy + fh / 2, 0.36, 0.36, 0, 1);
        String q = "coffee";
        int typed = 0;
        for (double k : Score.TYPE_KEYS) if (t >= k) typed++;
        String shown = q.substring(0, typed);
        Font ff = Assets.regular(42);
        if (typed == 0) Fx.text(g, ff, "Search product...", fx + 92, fy + 61, 0, DIM);
        else Fx.text(g, ff, shown, fx + 92, fy + 61, 0, TEXT);
        double caretX = fx + 92 + (typed == 0 ? 0 : Fx.advance(g, ff, shown)) + 4;
        boolean typing = typed < q.length();
        if (typing || ((int) (t * 3)) % 2 == 0) {
            g.setColor(AMBER);
            g.fill(new Rectangle2D.Double(caretX, fy + 22, 3, 48));
        }
        g.setTransform(o);

        // results filter live as the letters arrive
        double ra = Fx.outExpo(Fx.prog(t, 18.08, 0.4));
        double ry = 380;
        for (int i = 0; i < PRODUCTS.length; i++) {
            String name = PRODUCTS[i][1].toLowerCase();
            double vis = 1;
            for (int k = 1; k <= q.length(); k++) {
                if (!name.contains(q.substring(0, k))) {
                    double tk = Score.TYPE_KEYS[k - 1];
                    vis = Math.min(vis, 1 - Fx.outExpo(Fx.prog(t, tk, 0.22)));
                    break;
                }
            }
            double h = 78 * vis;
            if (h < 1) continue;
            double rowIn = Fx.outExpo(Fx.prog(t, 18.08 + 0.04 * i, 0.4));
            Composite o2 = g.getComposite();
            g.setComposite(Fx.opacity(ra * vis * rowIn * (1 - out)));
            double rx = 460 + 120 * (1 - rowIn);
            boolean hit = i == 0 && t >= Score.SEARCH_HIT;
            double hitGlow = hit ? Math.exp(-(t - Score.SEARCH_HIT) / 0.35) : 0;
            Fx.card(g, rx, ry, 1000, h - 10, 14, Fx.mix(i % 2 == 0 ? Assets.ROW_A : Assets.ROW_B, new Color(60, 44, 12), hit ? 0.6 : 0),
                    hit ? Fx.mix(AMBER, Color.WHITE, hitGlow * 0.5) : Assets.GRID, hit ? 2.5 : 1);
            Shape clip = g.getClip();
            g.clip(new Rectangle2D.Double(rx, ry, 1000, h - 10));
            Fx.text(g, Assets.mono(20), PRODUCTS[i][0], rx + 24, ry + 44, 0, MUTED);
            drawHighlighted(g, Assets.semibold(24), PRODUCTS[i][1], q.substring(0, Math.max(0, typed)), rx + 170, ry + 45);
            Fx.text(g, Assets.regular(19), PRODUCTS[i][2], rx + 640, ry + 44, 0, MUTED);
            Fx.textRight(g, Assets.bold(21), PRODUCTS[i][3], rx + 960, ry + 44, TEXT);
            g.setClip(clip);
            g.setComposite(o2);
            ry += h;
            if (hit) {
                double cp = Fx.outBack(Fx.prog(t, Score.SEARCH_HIT, 0.35), 2.8);
                AffineTransform o3 = g.getTransform();
                g.translate(rx + 1000 + 40, ry - 50);
                g.scale(cp, cp);
                g.setColor(Assets.GREEN);
                g.fill(new Ellipse2D.Double(-24, -24, 48, 48));
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D tick = new Path2D.Double();
                tick.moveTo(-10, 0); tick.lineTo(-3, 8); tick.lineTo(11, -8);
                g.draw(tick);
                g.setTransform(o3);
                ripple(g, rx + 1040, ry - 50, t, Score.SEARCH_HIT, 90, Assets.GREEN);
            }
        }
        g.setComposite(oc);
        g.setTransform(old);
    }

    static void drawHighlighted(Graphics2D g, Font f, String text, String q, double x, double y) {
        int idx = q.isEmpty() ? -1 : text.toLowerCase().indexOf(q);
        if (idx < 0) {
            Fx.text(g, f, text, x, y, 0, TEXT);
            return;
        }
        String a = text.substring(0, idx), b = text.substring(idx, idx + q.length()), c = text.substring(idx + q.length());
        Fx.text(g, f, a, x, y, 0, TEXT);
        double xb = x + Fx.advance(g, f, a);
        double wb = Fx.advance(g, f, b);
        g.setColor(Fx.alpha(AMBER, 0.25));
        g.fill(Fx.rr(xb - 2, y - 26, wb + 4, 34, 6));
        Fx.text(g, f, b, xb, y, 0, AMBER);
        Fx.text(g, f, c, xb + wb, y, 0, TEXT);
    }

    static void roles(Graphics2D g, double t) {
        sectionHeaderFade(g, t, 19.32, "04", "ROLE-BASED ACCESS", "Every user sees exactly what their role allows",
                1 - Fx.prog(t, 20.3, 0.15));
        String[] names = {"ADMINISTRATOR", "MANAGER", "STAFF"};
        String[] initials = {"SA", "MS", "JD"};
        String[][] perms = {
            {"+Manage user accounts", "+Delete records", "+Void transactions", "+Reports & CSV export"},
            {"+Edit master data", "+Void transactions", "+Reports & CSV export", "-Manage user accounts"},
            {"+Receiving & releasing", "+Stock transfers", "+View inventory", "-Reports & CSV export"},
        };
        for (int i = 0; i < 3; i++) {
            double ti = Score.ROLE_FLIPS[i];
            double flipIn = Fx.prog(t, ti - 0.12, 0.42);
            if (flipIn <= 0) continue;
            double ang = (1 - Fx.outBack(flipIn, 1.4)) * PI / 2;
            double flipOut = Fx.inCubic(Fx.prog(t, 20.3 + 0.05 * i, 0.2));
            double sx = Math.max(0.0, Math.cos(ang)) * (1 - flipOut);
            if (sx < 0.01) continue;
            double x = 180 + i * 540, y = 250, w = 480, h = 560;
            AffineTransform o = g.getTransform();
            g.translate(x + w / 2, y + h / 2);
            g.scale(sx, 1 - 0.06 * (1 - sx));
            g.translate(-(x + w / 2), -(y + h / 2));
            double shade = 1 - sx;
            Fx.card(g, x, y, w, h, 22, Fx.mix(Assets.CARD, Color.BLACK, shade * 0.5), Fx.mix(Assets.CARD_BORDER, AMBER, Math.exp(-Math.max(0, t - ti) / 0.3)), 2);
            // avatar like the app's sidebar user card
            g.setColor(AMBER);
            g.fill(new Ellipse2D.Double(x + w / 2 - 48, y + 46, 96, 96));
            Fx.textCentered(g, Assets.black(36), initials[i], x + w / 2, y + 107, 0, Color.BLACK);
            Fx.textCentered(g, Assets.black(32), names[i], x + w / 2, y + 196, 3, TEXT);
            for (int p = 0; p < 4; p++) {
                double la = Fx.outExpo(Fx.prog(t, ti + 0.18 + 0.06 * p, 0.3));
                if (la <= 0) continue;
                boolean ok = perms[i][p].startsWith("+");
                double ly = y + 270 + p * 66;
                double lx = x + 50 + 40 * (1 - la);
                Composite oc = g.getComposite();
                g.setComposite(Fx.opacity(la * (1 - flipOut)));
                g.setColor(ok ? Assets.GREEN : new Color(120, 40, 40));
                g.fill(new Ellipse2D.Double(lx, ly - 22, 32, 32));
                g.setColor(ok ? Color.BLACK : Assets.RED);
                g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (ok) {
                    Path2D tick = new Path2D.Double();
                    tick.moveTo(lx + 9, ly - 6); tick.lineTo(lx + 14, ly); tick.lineTo(lx + 24, ly - 12);
                    g.draw(tick);
                } else {
                    g.draw(new Line2D.Double(lx + 10, ly - 12, lx + 22, ly));
                    g.draw(new Line2D.Double(lx + 22, ly - 12, lx + 10, ly));
                }
                Fx.text(g, Assets.semibold(22), perms[i][p].substring(1), lx + 50, ly + 2, 0, ok ? TEXT : DIM);
                g.setComposite(oc);
            }
            g.setTransform(o);
        }
    }

    static final String[] REPORTS = {"Inventory Status", "Low Stock / Reorder List", "Stock Movement History", "Receiving Report",
        "Releasing Report", "Stock Transfer Report", "Warehouse Summary", "Monthly In/Out Summary", "Product List",
        "Supplier List", "Customer List", "User List"};

    static void reports(Graphics2D g, double t) {
        double out = Fx.inCubic(Fx.prog(t, 21.18, 0.25));
        sectionHeaderFade(g, t, 20.42, "05", "12 REPORTS", "Print them or export to CSV in one click", 1 - out);
        AffineTransform old = g.getTransform();
        double z = 1 - 0.3 * out;
        g.translate(CX, 560);
        g.scale(z, z);
        g.translate(-CX, -560);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                int i = r * 4 + c;
                int order = 0;
                // diagonal wave order
                int[] diag = {0, 1, 3, 6, 2, 4, 7, 9, 5, 8, 10, 11};
                order = diag[i];
                double ti = Score.REPORT_TILES[order];
                double e = Fx.outBack(Fx.prog(t, ti, 0.35), 2.2);
                if (e <= 0) continue;
                double x = 140 + c * 415, y = 250 + r * 190, w = 395, h = 168;
                AffineTransform o = g.getTransform();
                g.translate(x + w / 2, y + h / 2);
                g.scale(0.6 + 0.4 * e, 0.6 + 0.4 * e);
                g.translate(-(x + w / 2), -(y + h / 2));
                Composite oc = g.getComposite();
                g.setComposite(Fx.opacity(Fx.clamp01((t - ti) * 6) * (1 - out * 0.85)));
                double glow = Math.exp(-Math.max(0, t - ti) / 0.25);
                Fx.card(g, x, y, w, h, 16, Assets.CARD, Fx.mix(Assets.CARD_BORDER, AMBER, glow), 1.5 + glow);
                icon(g, ModuleIcon.Type.REPORTS, x + 52, y + 56, 40, 1, false);
                Fx.text(g, Assets.bold(23), REPORTS[i], x + 30, y + 118, 0, TEXT);
                Fx.text(g, Assets.mono(15), "PRINT  ·  CSV", x + 31, y + 146, 1.5, MUTED);
                g.setComposite(oc);
                g.setTransform(o);
            }
        }
        g.setTransform(old);
    }

    static void sectionHeaderFade(Graphics2D g, double t, double t0, String n, String title, String sub, double a) {
        if (a <= 0) return;
        Composite oc = g.getComposite();
        g.setComposite(Fx.opacity(a));
        sectionHeader(g, t, t0, n, title, sub);
        g.setComposite(oc);
    }

    static void tech(Graphics2D g, double t) {
        String[] chips = {"JAVA SWING", "AWT", "JDBC", "APACHE DERBY"};
        double[][] pos = {{CX - 300, 470}, {CX + 330, 470}, {CX - 360, 640}, {CX + 250, 640}};
        double out = Fx.inExpo(Fx.prog(t, 21.8, 0.2));
        double la = Fx.prog(t, 21.2, 0.3) * (1 - out);
        Fx.textCentered(g, Assets.semibold(28), "BUILT WITH", CX, 360, 14, Fx.alpha(AMBER, la));
        for (int i = 0; i < 4; i++) {
            double ti = Score.TECH_CHIPS[i];
            if (t < ti) continue;
            double e = Fx.spring(t - ti, 20, 9);
            double sc = 1.9 - 0.9 * e;
            double rot = Math.toRadians((i % 2 == 0 ? -5 : 5) * (1 - e));
            double dirX = pos[i][0] - CX, dirY = pos[i][1] - 555;
            double px = pos[i][0] + dirX * 3 * out, py = pos[i][1] + dirY * 3 * out;
            AffineTransform o = g.getTransform();
            g.translate(px, py);
            g.rotate(rot);
            g.scale(sc, sc);
            Font f = Assets.black(58);
            double w = Fx.advance(g, f, chips[i]) + 80;
            Composite oc = g.getComposite();
            g.setComposite(Fx.opacity(Fx.clamp01((t - ti) * 10) * (1 - out)));
            double glow = Math.exp(-(t - ti) / 0.2);
            Fx.card(g, -w / 2, -60, w, 120, 120, Fx.mix(Assets.CARD, new Color(70, 50, 12), glow), Fx.mix(AMBER, Color.WHITE, glow * 0.6), 3.5);
            Fx.textCentered(g, f, chips[i], 0, 21, 0, i % 2 == 0 ? TEXT : AMBER);
            g.setComposite(oc);
            g.setTransform(o);
            ripple(g, pos[i][0], pos[i][1], t, ti, 180, AMBER);
        }
    }

    // ================================================================== 7. ASSEMBLY (22 - 26)

    /** A cut-out of the real screenshot: source rectangle in screenshot pixels. */
    static void cut(Graphics2D g, int x0, int y0, int x1, int y1, double dx, double dy, double sc, double a) {
        if (a <= 0.001) return;
        BufferedImage src = Assets.shot;
        Composite oc = g.getComposite();
        if (a < 1) g.setComposite(Fx.opacity(a));
        double cx = (x0 + x1) / 2.0 + dx, cy = (y0 + y1) / 2.0 - 33 + dy;
        AffineTransform o = g.getTransform();
        g.translate(cx, cy);
        g.scale(sc, sc);
        g.translate(-(x1 - x0) / 2.0, -(y1 - y0) / 2.0);
        g.drawImage(src, 0, 0, x1 - x0, y1 - y0, x0, y0, x1, y1, null);
        g.setTransform(o);
        g.setComposite(oc);
    }

    static double slideIn(double t, double t0, double dur) { return 1 - Fx.outExpo(Fx.prog(t, t0, dur)); }

    static void assembly(Graphics2D g, double t, Ctx ctx) {
        double[] a = Score.ASSEMBLE;
        BufferedImage canvas = ctx.canvas;
        Graphics2D cg = canvas.createGraphics();
        Assets.hq(cg);
        cg.setColor(Assets.BG);
        cg.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // menu bar + header drop down
        cut(cg, 0, 33, 1442, 61, 0, -40 * slideIn(t, a[0], 0.5), 1, Fx.prog(t, a[0], 0.15));
        double hs = Fx.spring(t - (a[0] + 0.05), 16, 7);
        cut(cg, 0, 61, 1442, 142, 0, -110 * (1 - hs), 1, Fx.prog(t, a[0] + 0.05, 0.12));
        // sidebar background then every item, one by one
        double sb = slideIn(t, a[1], 0.45);
        cg.setColor(new Color(8, 11, 14));
        cg.fill(new Rectangle2D.Double(-270 * sb, 109, 262, 760));
        cg.setColor(new Color(70, 52, 22));
        cg.fill(new Rectangle2D.Double(262 - 270 * sb, 109, 1, 760));
        int[][] side = {{146, 216}, {224, 248}, {249, 295}, {295, 341}, {352, 378}, {378, 424}, {424, 470}, {470, 516},
            {526, 551}, {551, 597}, {597, 643}, {643, 689}, {689, 735}, {735, 781}, {792, 817}, {817, 863}, {874, 900}};
        for (int k = 0; k < side.length; k++) {
            double tk = a[1] + 0.06 + 0.028 * k;
            cut(cg, 0, side[k][0], 258, side[k][1], -220 * slideIn(t, tk, 0.4), 0, 1, Fx.prog(t, tk, 0.15));
        }
        // page title
        cut(cg, 270, 150, 1000, 248, 0, 40 * slideIn(t, a[2], 0.45), 1, Fx.prog(t, a[2], 0.2));
        // form card
        double fc = Fx.outBack(Fx.prog(t, a[3], 0.4), 1.6);
        if (fc > 0) {
            AffineTransform o = cg.getTransform();
            cg.translate(487, 535);
            cg.scale(0.85 + 0.15 * fc, 0.85 + 0.15 * fc);
            cg.translate(-487, -535);
            Composite oc = cg.getComposite();
            cg.setComposite(Fx.opacity(Fx.clamp01(Fx.prog(t, a[3], 0.15))));
            Fx.card(cg, 287.5, 223.5, 399, 624, 16, Assets.CARD, Assets.CARD_BORDER, 1.2);
            cg.setComposite(oc);
            cg.setTransform(o);
        }
        cut(cg, 296, 266, 680, 330, 0, 20 * slideIn(t, a[3] + 0.08, 0.4), 1, Fx.prog(t, a[3] + 0.08, 0.2));
        int[] formRows = {332, 374, 416, 458, 500, 542, 584, 626, 664, 756};
        for (int k = 0; k < formRows.length - 1; k++) {
            double tk = a[4] + 0.035 * k;
            cut(cg, 296, formRows[k], 680, formRows[k + 1], -70 * slideIn(t, tk, 0.4), 0, 1, Fx.prog(t, tk, 0.15));
        }
        // list card
        double lc = Fx.outBack(Fx.prog(t, a[5], 0.4), 1.6);
        if (lc > 0) {
            AffineTransform o = cg.getTransform();
            cg.translate(1060, 535);
            cg.scale(0.85 + 0.15 * lc, 0.85 + 0.15 * lc);
            cg.translate(-1060, -535);
            Composite oc = cg.getComposite();
            cg.setComposite(Fx.opacity(Fx.prog(t, a[5], 0.15)));
            Fx.card(cg, 703.5, 223.5, 713, 624, 16, Assets.CARD, Assets.CARD_BORDER, 1.2);
            cg.setComposite(oc);
            cg.setTransform(o);
        }
        cut(cg, 712, 266, 1408, 362, 0, -40 * slideIn(t, a[6], 0.45), 1, Fx.prog(t, a[6], 0.2));
        cut(cg, 721, 366, 1399, 407, 0, -24 * slideIn(t, a[7], 0.4), 1, Fx.prog(t, a[7], 0.15));
        for (int k = 0; k < 12; k++) {
            double tk = a[8] + 0.028 * k;
            int y0 = 407 + 34 * k;
            cut(cg, 721, y0, 1399, y0 + 34, 160 * slideIn(t, tk, 0.4), 0, 1, Fx.prog(t, tk, 0.15));
        }
        cut(cg, 712, 815, 1408, 875, 0, 20 * slideIn(t, a[9], 0.4), 1, Fx.prog(t, a[9], 0.2));
        // buttons pop last
        for (int k = 0; k < 2; k++) {
            double tk = a[9] + 0.07 * k;
            double bp = Fx.outBack(Fx.prog(t, tk, 0.35), 2.4);
            int y0 = k == 0 ? 766 : 820;
            cut(cg, 296, y0, 680, y0 + 50, 0, 0, bp, Math.min(1, bp * 2));
        }
        cut(cg, 0, 901, 1442, 934, 0, 40 * slideIn(t, a[9] + 0.1, 0.4), 1, Fx.prog(t, a[9] + 0.1, 0.2));
        // light sweep over the finished screen
        if (t > Score.SWEEP && t < Score.SWEEP + 0.7) {
            double p = Fx.inOutCubic(Fx.prog(t, Score.SWEEP, 0.6));
            double x = Fx.lerp(-500, 1900, p);
            cg.setPaint(new LinearGradientPaint((float) (x - 200), 0f, (float) (x + 200), 300f, new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(255, 255, 255, 0), new Color(255, 240, 210, 70), new Color(255, 255, 255, 0)}));
            cg.fillRect(0, 0, 1442, 900);
        }
        cg.setColor(Fx.alpha(Assets.CARD_BORDER, Fx.prog(t, 21.9, 0.3)));
        cg.setStroke(new BasicStroke(2f));
        cg.drawRect(1, 1, 1440, 898);
        cg.dispose();

        // 3D plane that settles flat, then the punch-in to the logo
        double settle = Fx.outCubic(Fx.prog(t, 21.85, 2.7));
        double rotX = -0.62 * (1 - settle);
        double rotY = -0.32 * (1 - settle) + 0.015 * Math.sin((t - 22) * 1.4) * (1 - Fx.prog(t, 24.4, 0.6));
        double scale = 1.05 * (0.82 + 0.18 * Fx.outExpo(Fx.prog(t, 21.85, 1.2)));
        double alphaIn = Fx.prog(t, 21.85, 0.25);
        double cx = CX, cy = 548;
        AffineTransform old = g.getTransform();
        if (t > 25.3) {
            double zp = Fx.inExpo(Fx.prog(t, 25.32, 0.68));
            double pan = Fx.inOutCubic(Fx.prog(t, 25.3, 0.55));
            double[] p = Fx.project(92, 67, 1442, 900, cx, cy, scale, rotX, rotY);
            double z = Math.exp(Math.log(18) * zp);
            // the camera pans to the header logo first, then the zoom rushes in
            g.translate((CX - p[0]) * pan, (470 - p[1]) * pan);
            g.translate(p[0], p[1]);
            g.scale(z, z);
            g.translate(-p[0], -p[1]);
        }
        // soft shadow under the window
        Fx.perspective(g, Assets.windowShadow, cx, cy + 30, scale * 1442 / 1442.0 * (1442.0 / 1442), rotX, rotY, 0.85 * alphaIn, 6);
        Fx.perspective(g, canvas, cx, cy, scale, rotX, rotY, alphaIn, 28);
        g.setTransform(old);
        if (t > 25.82) {
            g.setColor(Fx.alpha(new Color(255, 236, 200), Fx.inCubic(Fx.prog(t, 25.82, 0.18))));
            g.fillRect(0, 0, W, H);
        }
    }

    // ================================================================== 8. OUTRO (26 - 30)

    static void outro(Graphics2D g, double t, Ctx ctx) {
        double t0 = 26.0;
        double e = Fx.spring(t - t0, 13, 5.5);
        double s = 0.8 * (1.55 - 0.55 * e) * (1 + 0.035 * Fx.inOutCubic(Fx.prog(t, 26.6, 3.2)));
        double cx = CX, cy = 455 + 6 * Math.sin((t - 26) * 1.6);
        Fx.image(g, Assets.glowAmber, cx, cy, 3.6 * s, 2.0 * s, 0, 0.28);

        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.scale(s, s);
        g.rotate(Math.toRadians(-3 * (1 - e)));
        g.translate(-LCX, -LCY);
        // orbit swooshes swing in from opposite sides
        double sw = Fx.spring(t - t0, 11, 5);
        orbitPiece(g, "swoosh_left", -0.55 * (1 - sw), Fx.clamp01((t - t0) * 6));
        orbitPiece(g, "swoosh_right", 0.55 * (1 - sw), Fx.clamp01((t - t0) * 6));
        for (String name : Assets.LOGO.keySet()) {
            if (name.startsWith("swoosh")) continue;
            Assets.Piece p = Assets.LOGO.get(name);
            boolean letter = name.startsWith("w");
            double tt = letter ? t0 + 0.25 + 0.03 * (name.charAt(1) - '0') : t0;
            double a = Fx.clamp01((t - tt) * 6);
            double dy = letter ? 50 * (1 - Fx.outBack(Fx.prog(t, tt, 0.5), 2)) : 0;
            Fx.image(g, p.img, p.cx(), p.cy() + dy, 1, 1, 0, a);
        }
        if (t > 27.3 && t < 28.1) glint(g, t, 27.3, 0.75, ctx);
        g.setTransform(old);

        // wordmark line: tracking tightens as it fades in
        double wa = Fx.prog(t, 26.45, 0.6);
        double track = 12 + 34 * (1 - Fx.outExpo(Fx.prog(t, 26.45, 1.4)));
        Font f = Assets.semibold(36);
        String wm = "WAREHOUSE MANAGEMENT SYSTEM";
        double ww = Fx.width(g, f, wm, track);
        Fx.textCentered(g, f, wm, CX, 760, track, Fx.alpha(TEXT, wa));
        double ln = Fx.outExpo(Fx.prog(t, 26.8, 0.8));
        g.setColor(Fx.alpha(AMBER, wa));
        g.fill(new Rectangle2D.Double(CX - ww / 2 - 30 - 160 * ln, 746, 160 * ln, 3));
        g.fill(new Rectangle2D.Double(CX + ww / 2 + 30, 746, 160 * ln, 3));
        double sa = Fx.prog(t, 27.1, 0.6);
        Fx.textCentered(g, Assets.regular(24), "Warehouses  ·  Products  ·  Receiving  ·  Releasing  ·  Stock Transfers",
                CX, 815 + 12 * (1 - Fx.outCubic(sa)), 1, Fx.alpha(MUTED, sa));
        double ba = Fx.prog(t, 27.6, 0.6);
        Fx.textCentered(g, Assets.mono(18), "JAVA SWING  ·  AWT  ·  JDBC  ·  APACHE DERBY", CX, 1000, 3, Fx.alpha(DIM, ba));
    }

    static void orbitPiece(Graphics2D g, String name, double rot, double a) {
        Assets.Piece p = Assets.LOGO.get(name);
        AffineTransform o = g.getTransform();
        g.translate(545, 210);
        g.rotate(rot);
        g.translate(-545, -210);
        Fx.image(g, p.img, p.cx(), p.cy(), 1, 1, 0, a);
        g.setTransform(o);
    }

    private Scenes() {
    }
}
