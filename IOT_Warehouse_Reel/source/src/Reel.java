import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

/**
 * Renders the I.O.T Warehouse reel: 1920x1080, 60 fps, with sub-frame motion
 * blur, bloom, chromatic aberration on hits, vignette and film grain.
 *
 * Usage: Reel <reelDir> <appDir> frames <outDir> <fromFrame> <toFrame>
 *        Reel <reelDir> <appDir> stills <outDir> <t1> <t2> ...
 */
public class Reel {

    static final int W = 1920, H = 1080, FPS = 60, FRAMES = 1800;
    static float[] vignette;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        File reelDir = new File(args[0]), appDir = new File(args[1]);
        String mode = args[2];
        File out = new File(args[3]);
        out.mkdirs();
        Assets.load(reelDir, appDir);
        vignette = new float[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                double dx = (x - W / 2.0) / (W / 2.0), dy = (y - H / 2.0) / (H / 2.0);
                double r = Math.sqrt(dx * dx * 0.8 + dy * dy);
                vignette[y * W + x] = (float) (1 - 0.42 * Fx.smooth((r - 0.55) / 0.85));
            }
        }
        if (mode.equals("stills")) {
            Worker w = new Worker();
            for (int i = 4; i < args.length; i++) {
                double t = Double.parseDouble(args[i]);
                int[] px = w.frame(t, (int) Math.round(t * FPS));
                BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
                img.setRGB(0, 0, W, H, px, 0, W);
                ImageIO.write(img, "png", new File(out, String.format("still_%05.2f.png", t)));
                System.out.println("still " + t);
            }
            return;
        }
        int from = Integer.parseInt(args[4]), to = Integer.parseInt(args[5]);
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        ThreadLocal<Worker> workers = ThreadLocal.withInitial(Worker::new);
        AtomicInteger done = new AtomicInteger();
        long start = System.currentTimeMillis();
        java.util.List<Future<?>> jobs = new java.util.ArrayList<>();
        for (int f = from; f < to; f++) {
            final int frame = f;
            jobs.add(pool.submit(() -> {
                try {
                    Worker w = workers.get();
                    int[] px = w.frame(frame / (double) FPS, frame);
                    w.writeJpeg(px, new File(out, String.format("f%05d.jpg", frame)));
                    int n = done.incrementAndGet();
                    if (n % 60 == 0) {
                        double sec = (System.currentTimeMillis() - start) / 1000.0;
                        System.out.printf("%d/%d frames  %.0fs%n", n, to - from, sec);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }));
        }
        for (Future<?> j : jobs) j.get();
        pool.shutdown();
        System.out.printf("done %d frames in %.0fs%n", to - from, (System.currentTimeMillis() - start) / 1000.0);
    }

    /** Number of motion-blur sub-frames: more during fast moves and hits. */
    static int subframes(double t) {
        if ((t > 25.5 && t < 26.0) || (t > 11.65 && t < 12.05) || (t > 7.75 && t < 8.1)) return 10;
        if ((t > 3.3 && t < 4.3) || (t > 7.55 && t < 8.3) || (t > 11.6 && t < 12.3) || (t > 17.5 && t < 18.3)
                || (t > 21.7 && t < 22.1) || (t > 25.2 && t < 26.3)) return 6;
        return 4;
    }

    static double shutter(double t) {
        if ((t > 11.6 && t < 12.3) || (t > 25.2 && t < 26.05) || (t > 7.55 && t < 8.3)) return 0.8 / FPS;
        return 0.5 / FPS;
    }

    static double chroma(double t) {
        double a = 0.9;
        for (double ti : Score.IMPACTS) if (t >= ti) a += 10 * Math.exp(-(t - ti) / 0.12);
        if (t > 11.7 && t < 12.1) a += 5 * Math.sin(Math.PI * Fx.prog(t, 11.7, 0.4));
        if (t > 25.4 && t < 26.0) a += 5 * Fx.prog(t, 25.4, 0.6);
        if (t >= 15.0 && t < 15.25) a += 6 * (1 - Fx.prog(t, 15.0, 0.25));
        return a;
    }

    static double bloom(double t) {
        double b = 0.55;
        for (double ti : Score.IMPACTS) if (t >= ti) b += 0.5 * Math.exp(-(t - ti) / 0.3);
        if (t >= 4 && t < 8) b += 0.15;
        if (t >= 26) b += 0.15;
        return b;
    }

    static final class Worker {
        final BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        final int[] cpx = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
        final int[] acc = new int[W * H * 3];
        final int[] out = new int[W * H];
        final int[] tmp = new int[W * H];
        final Scenes.Ctx ctx = new Scenes.Ctx();
        final int sw = W / 4, sh = H / 4, sw2 = W / 8, sh2 = H / 8;
        final float[] br = new float[sw * sh], bg = new float[sw * sh], bb = new float[sw * sh];
        final float[] br2 = new float[sw2 * sh2], bg2 = new float[sw2 * sh2], bb2 = new float[sw2 * sh2];
        final ImageWriter jpeg;

        Worker() {
            Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
            jpeg = it.next();
        }

        int[] frame(double t, int frameIndex) {
            int s = subframes(t);
            double span = shutter(t);
            java.util.Arrays.fill(acc, 0);
            for (int k = 0; k < s; k++) {
                double ts = t + ((k + 0.5) / s - 0.5) * span;
                Graphics2D g = canvas.createGraphics();
                Scenes.render(g, Math.max(0, ts), ctx);
                g.dispose();
                for (int i = 0, j = 0; i < cpx.length; i++, j += 3) {
                    int c = cpx[i];
                    acc[j] += (c >> 16) & 255;
                    acc[j + 1] += (c >> 8) & 255;
                    acc[j + 2] += c & 255;
                }
            }
            for (int i = 0, j = 0; i < out.length; i++, j += 3) {
                out[i] = ((acc[j] / s) << 16) | ((acc[j + 1] / s) << 8) | (acc[j + 2] / s);
            }
            post(t, frameIndex);
            return out;
        }

        void post(double t, int frameIndex) {
            // ---- bloom: bright parts, blurred at two sizes, added back
            final float th = 0.58f;
            java.util.Arrays.fill(br, 0); java.util.Arrays.fill(bg, 0); java.util.Arrays.fill(bb, 0);
            for (int y = 0; y < sh; y++) {
                for (int x = 0; x < sw; x++) {
                    float r = 0, gg = 0, b = 0;
                    for (int yy = 0; yy < 4; yy += 2) {
                        for (int xx = 0; xx < 4; xx += 2) {
                            int c = out[(y * 4 + yy) * W + x * 4 + xx];
                            float cr = ((c >> 16) & 255) / 255f, cg = ((c >> 8) & 255) / 255f, cb = (c & 255) / 255f;
                            float lum = 0.2126f * cr + 0.7152f * cg + 0.0722f * cb;
                            float k = Math.max(0, lum - th) / (1 - th);
                            r += cr * k; gg += cg * k; b += cb * k;
                        }
                    }
                    int i = y * sw + x;
                    br[i] = r / 4; bg[i] = gg / 4; bb[i] = b / 4;
                }
            }
            for (int y = 0; y < sh2; y++) {
                for (int x = 0; x < sw2; x++) {
                    int a = (y * 2) * sw + x * 2;
                    int i = y * sw2 + x;
                    br2[i] = (br[a] + br[a + 1] + br[a + sw] + br[a + sw + 1]) / 4;
                    bg2[i] = (bg[a] + bg[a + 1] + bg[a + sw] + bg[a + sw + 1]) / 4;
                    bb2[i] = (bb[a] + bb[a + 1] + bb[a + sw] + bb[a + sw + 1]) / 4;
                }
            }
            for (int p = 0; p < 3; p++) {
                blur(br, sw, sh, 3); blur(bg, sw, sh, 3); blur(bb, sw, sh, 3);
                blur(br2, sw2, sh2, 4); blur(bg2, sw2, sh2, 4); blur(bb2, sw2, sh2, 4);
            }
            float k1 = (float) (bloom(t) * 1.1), k2 = (float) (bloom(t) * 0.9);
            double chroma = chroma(t);
            float kc = (float) (chroma / (W / 2.0));
            for (int y = 0; y < H; y++) {
                float sy = (y + 0.5f) / 4f - 0.5f, sy2 = (y + 0.5f) / 8f - 0.5f;
                for (int x = 0; x < W; x++) {
                    float sx = (x + 0.5f) / 4f - 0.5f, sx2 = (x + 0.5f) / 8f - 0.5f;
                    int i = y * W + x;
                    int c = out[i];
                    float r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                    r += 255 * (k1 * sample(br, sw, sh, sx, sy) + k2 * sample(br2, sw2, sh2, sx2, sy2));
                    g += 255 * (k1 * sample(bg, sw, sh, sx, sy) + k2 * sample(bg2, sw2, sh2, sx2, sy2));
                    b += 255 * (k1 * sample(bb, sw, sh, sx, sy) + k2 * sample(bb2, sw2, sh2, sx2, sy2));
                    tmp[i] = (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
                }
            }
            // ---- chromatic aberration (radial), vignette, grain
            long seed = frameIndex * 0x9E3779B97F4A7C15L + 1;
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    int i = y * W + x;
                    int c = tmp[i];
                    int r, g = (c >> 8) & 255, b;
                    if (chroma > 0.3) {
                        float dx = (x - W / 2f) * kc, dy = (y - H / 2f) * kc;
                        int rx = clampI((int) (x + dx), W), ry = clampI((int) (y + dy), H);
                        int bx = clampI((int) (x - dx), W), by = clampI((int) (y - dy), H);
                        r = (tmp[ry * W + rx] >> 16) & 255;
                        b = tmp[by * W + bx] & 255;
                    } else {
                        r = (c >> 16) & 255;
                        b = c & 255;
                    }
                    float v = vignette[i];
                    seed ^= seed << 13; seed ^= seed >>> 7; seed ^= seed << 17;
                    float n = ((seed & 1023) / 1023f - 0.5f) * 7f;
                    out[i] = (clamp(r * v + n) << 16) | (clamp(g * v + n) << 8) | clamp(b * v + n);
                }
            }
        }

        void writeJpeg(int[] px, File f) throws Exception {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, W, H, px, 0, W);
            if (f.exists()) f.delete();
            try (FileImageOutputStream ios = new FileImageOutputStream(f)) {
                jpeg.setOutput(ios);
                ImageWriteParam p = jpeg.getDefaultWriteParam();
                p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                p.setCompressionQuality(0.96f);
                jpeg.write(null, new IIOImage(img, null, null), p);
            }
        }
    }

    static float sample(float[] a, int w, int h, float x, float y) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > w - 1.001f) x = w - 1.001f;
        if (y > h - 1.001f) y = h - 1.001f;
        int x0 = (int) x, y0 = (int) y;
        float fx = x - x0, fy = y - y0;
        int i = y0 * w + x0;
        float top = a[i] + (a[i + 1] - a[i]) * fx;
        float bot = a[i + w] + (a[i + w + 1] - a[i + w]) * fx;
        return top + (bot - top) * fy;
    }

    static void blur(float[] a, int w, int h, int r) {
        float[] t = new float[a.length];
        float inv = 1f / (2 * r + 1);
        for (int y = 0; y < h; y++) {
            float sum = 0;
            int row = y * w;
            for (int x = -r; x <= r; x++) sum += a[row + Math.max(0, Math.min(w - 1, x))];
            for (int x = 0; x < w; x++) {
                t[row + x] = sum * inv;
                sum += a[row + Math.min(w - 1, x + r + 1)] - a[row + Math.max(0, x - r)];
            }
        }
        for (int x = 0; x < w; x++) {
            float sum = 0;
            for (int y = -r; y <= r; y++) sum += t[Math.max(0, Math.min(h - 1, y)) * w + x];
            for (int y = 0; y < h; y++) {
                a[y * w + x] = sum * inv;
                sum += t[Math.min(h - 1, y + r + 1) * w + x] - t[Math.max(0, y - r) * w + x];
            }
        }
    }

    static int clamp(float v) { return v < 0 ? 0 : v > 255 ? 255 : (int) v; }

    static int clampI(int v, int max) { return v < 0 ? 0 : v >= max ? max - 1 : v; }
}
