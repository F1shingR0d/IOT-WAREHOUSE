import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Synthesizes the 30-second soundtrack (120 BPM, A minor, electronic) from
 * scratch: drums, sub bass, sidechained supersaw chords, pluck arpeggio, FM
 * bell lead, risers, impacts, whooshes and UI sound effects that follow the
 * same Score as the picture. Writes a 48 kHz / 16-bit stereo WAV.
 */
public class Music {

    static final int SR = 48000;
    static final int N = (int) Math.round(Score.LENGTH * SR);
    static final double[] L = new double[N], R = new double[N];
    static final double[] revL = new double[N], revR = new double[N];
    static final double[] dlyL = new double[N], dlyR = new double[N];
    static final double[] side = new double[N];
    static final Random RND = new Random(20261011);

    // chord per bar: bass root first, then the pad voicing (MIDI notes)
    static final int[][] CHORDS = {
        {33, 52, 57, 60, 64}, // 0  Am
        {29, 53, 57, 60, 65}, // 1  F
        {33, 52, 57, 60, 64}, // 2  Am  (drop A)
        {29, 53, 57, 60, 65}, // 3  F
        {36, 52, 55, 60, 64}, // 4  C
        {31, 50, 55, 59, 62}, // 5  G
        {33, 52, 57, 60, 64}, // 6  Am
        {29, 53, 57, 60, 65}, // 7  F
        {31, 50, 55, 59, 62}, // 8  G   (breakdown)
        {33, 52, 57, 60, 64}, // 9  Am  (drop B)
        {29, 53, 57, 60, 65}, // 10 F
        {36, 52, 55, 60, 64}, // 11 C
        {31, 50, 55, 59, 62}, // 12 G
        {33, 45, 52, 57, 60, 64}, // 13 Am (final)
        {33, 45, 52, 57, 60, 64}, // 14 Am (tail)
    };

    public static void main(String[] args) throws IOException {
        buildSidechain();
        drums();
        bass();
        pads();
        arp();
        bell();
        effects();
        uiSounds();
        mixdown();
        writeWav(new File(args[0]));
        System.out.println("music written: " + args[0]);
    }

    // ------------------------------------------------------------------ helpers

    static int s(double t) { return (int) Math.round(t * SR); }

    static double hz(double midi) { return 440.0 * Math.pow(2, (midi - 69) / 12.0); }

    static double noise() { return RND.nextDouble() * 2 - 1; }

    /** Adds a mono buffer at time t with gain and pan (-1 left .. 1 right). */
    static void put(double[] buf, double t, double gain, double pan, double reverb) {
        int start = s(t);
        double gl = Math.cos((pan + 1) * Math.PI / 4) * Math.sqrt(2);
        double gr = Math.sin((pan + 1) * Math.PI / 4) * Math.sqrt(2);
        for (int i = 0; i < buf.length; i++) {
            int k = start + i;
            if (k < 0 || k >= N) continue;
            double v = buf[i] * gain;
            L[k] += v * gl;
            R[k] += v * gr;
            revL[k] += v * gl * reverb;
            revR[k] += v * gr * reverb;
        }
    }

    static void putStereo(double[] bl, double[] br, double t, double gain, double reverb, double delay) {
        int start = s(t);
        for (int i = 0; i < bl.length; i++) {
            int k = start + i;
            if (k < 0 || k >= N) continue;
            L[k] += bl[i] * gain;
            R[k] += br[i] * gain;
            revL[k] += bl[i] * gain * reverb;
            revR[k] += br[i] * gain * reverb;
            dlyL[k] += bl[i] * gain * delay;
            dlyR[k] += br[i] * gain * delay;
        }
    }

    static double polyBlep(double t, double dt) {
        if (t < dt) { t /= dt; return t + t - t * t - 1; }
        if (t > 1 - dt) { t = (t - 1) / dt; return t * t + t + t + 1; }
        return 0;
    }

    /** RBJ biquad filter (transposed direct form II). */
    static final class Biquad {
        double b0, b1, b2, a1, a2, z1, z2;

        static Biquad lowpass(double f, double q) { Biquad b = new Biquad(); b.setLowpass(f, q); return b; }
        static Biquad highpass(double f, double q) { Biquad b = new Biquad(); b.setHighpass(f, q); return b; }
        static Biquad bandpass(double f, double q) { Biquad b = new Biquad(); b.setBandpass(f, q); return b; }

        void setLowpass(double f, double q) {
            double w = 2 * Math.PI * Math.min(f, SR * 0.45) / SR, c = Math.cos(w), a = Math.sin(w) / (2 * q), n = 1 + a;
            b0 = (1 - c) / 2 / n; b1 = (1 - c) / n; b2 = b0; a1 = -2 * c / n; a2 = (1 - a) / n;
        }

        void setHighpass(double f, double q) {
            double w = 2 * Math.PI * Math.min(f, SR * 0.45) / SR, c = Math.cos(w), a = Math.sin(w) / (2 * q), n = 1 + a;
            b0 = (1 + c) / 2 / n; b1 = -(1 + c) / n; b2 = b0; a1 = -2 * c / n; a2 = (1 - a) / n;
        }

        void setBandpass(double f, double q) {
            double w = 2 * Math.PI * Math.min(f, SR * 0.45) / SR, c = Math.cos(w), a = Math.sin(w) / (2 * q), n = 1 + a;
            b0 = a / n; b1 = 0; b2 = -a / n; a1 = -2 * c / n; a2 = (1 - a) / n;
        }

        double process(double x) {
            double y = b0 * x + z1;
            z1 = b1 * x - a1 * y + z2;
            z2 = b2 * x - a2 * y;
            return y;
        }
    }

    // ------------------------------------------------------------------ sidechain

    static void buildSidechain() {
        double[] kicks = Score.kicks();
        java.util.Arrays.fill(side, 1.0);
        for (int i = 0; i < N; i++) {
            double t = i / (double) SR, last = -10;
            for (double k : kicks) if (k <= t) last = k;
            double dt = t - last;
            if (dt < 0 || dt > 0.45) continue;
            double env = dt < 0.004 ? dt / 0.004 : Math.exp(-(dt - 0.004) / 0.10);
            side[i] = 1 - 0.78 * env;
        }
    }

    // ------------------------------------------------------------------ drums

    static double[] kick() {
        int n = s(0.55);
        double[] b = new double[n];
        double ph = 0;
        Random r = new Random(1);
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double f = 46 + 125 * Math.exp(-t / 0.032) + 60 * Math.exp(-t / 0.004);
            ph += 2 * Math.PI * f / SR;
            double env = Math.exp(-t / 0.32) * Math.min(1, t / 0.0015);
            double click = t < 0.005 ? (r.nextDouble() * 2 - 1) * (1 - t / 0.005) * 0.5 : 0;
            b[i] = Math.tanh((Math.sin(ph) * env + click) * 2.2) * 0.92;
        }
        return b;
    }

    static double[] clap(long seed) {
        Random r = new Random(seed);
        int n = s(0.45);
        double[] b = new double[n];
        Biquad bp = Biquad.bandpass(1250, 0.8);
        Biquad hp = Biquad.highpass(500, 0.7);
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR, env = 0;
            for (int k = 0; k < 4; k++) {
                double tk = k * 0.0105;
                if (t >= tk) env = Math.max(env, Math.exp(-(t - tk) / 0.0065));
            }
            if (t > 0.032) env = Math.max(env, 0.6 * Math.exp(-(t - 0.032) / 0.13));
            b[i] = hp.process(bp.process((r.nextDouble() * 2 - 1) * env)) * 3.0;
        }
        return b;
    }

    static double[] snare(long seed) {
        Random r = new Random(seed);
        int n = s(0.22);
        double[] b = new double[n];
        Biquad bp = Biquad.bandpass(1900, 0.6);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            ph += 2 * Math.PI * (185 + 60 * Math.exp(-t / 0.01)) / SR;
            b[i] = bp.process((r.nextDouble() * 2 - 1)) * Math.exp(-t / 0.075) * 1.9 + Math.sin(ph) * Math.exp(-t / 0.045) * 0.6;
        }
        return b;
    }

    static double[] hat(double decay, long seed) {
        Random r = new Random(seed);
        int n = s(Math.min(0.6, decay * 6));
        double[] b = new double[n];
        Biquad hp = Biquad.highpass(7200, 0.7);
        Biquad bp = Biquad.bandpass(10500, 1.2);
        double[] ratios = {205.3, 304.4, 369.6, 522.7, 540.0, 800.0};
        double[] ph = new double[6];
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR, metal = 0;
            for (int k = 0; k < 6; k++) {
                ph[k] += ratios[k] * 2.0 / SR;
                metal += (ph[k] % 1.0) < 0.5 ? 1 : -1;
            }
            double x = (r.nextDouble() * 2 - 1) * 0.7 + metal / 6 * 0.5;
            b[i] = hp.process(bp.process(x)) * Math.exp(-t / decay) * 2.4;
        }
        return b;
    }

    static void drums() {
        double[] kick = kick();
        for (double t : Score.kicks()) put(kick, t, t == 26.0 ? 0.85 : 0.72, 0, 0.04);

        double[][] claps = {clap(11), clap(12), clap(13)};
        double[] hc = hat(0.028, 21), hc2 = hat(0.022, 22), ho = hat(0.17, 23);
        int ci = 0;
        for (int bar = 0; bar < 15; bar++) {
            double b0 = bar * Score.BAR;
            boolean drop = Score.kickSection(b0 + 0.01);
            for (int sixteenth = 0; sixteenth < 16; sixteenth++) {
                double t = b0 + sixteenth * Score.BEAT / 4;
                // claps on beats 2 and 4
                if (drop && (sixteenth == 4 || sixteenth == 12)) put(claps[ci++ % 3], t, 0.62, 0, 0.24);
                // closed hats: 16ths, accent on the off-beat 8th; quieter in the intro
                boolean introHats = t >= 1.0 && t < 3.875;
                boolean breakHats = t >= 16.0 && t < 18.0;
                if (drop || introHats || breakHats) {
                    double vel = (sixteenth % 4 == 2) ? 1.0 : (sixteenth % 2 == 1 ? 0.55 : 0.7);
                    if (introHats) vel *= 0.35 + 0.4 * (t - 1.0) / 2.9;
                    if (breakHats) vel *= 0.6;
                    double pan = (sixteenth % 2 == 0) ? -0.25 : 0.25;
                    if (!(drop && sixteenth % 4 == 2)) put(sixteenth % 2 == 0 ? hc : hc2, t, 0.2 * vel, pan, 0.02);
                }
                // open hat on the off-beat 8th in the drops
                if (drop && sixteenth % 4 == 2) put(ho, t, 0.14, 0.15, 0.05);
            }
        }
        // snare rolls into the drops (8ths -> 16ths -> 32nds, rising)
        roll(3.0, 4.0 - 0.125);
        roll(17.0, 18.0 - 0.0625);
    }

    static void roll(double from, double to) {
        double t = from;
        int i = 0;
        while (t < to) {
            double p = (t - from) / (to - from);
            double step = p < 0.5 ? Score.BEAT / 2 : p < 0.8 ? Score.BEAT / 4 : Score.BEAT / 8;
            put(snare(100 + i), t, 0.12 + 0.28 * p, (i % 2 == 0) ? -0.15 : 0.15, 0.25);
            t += step;
            i++;
        }
    }

    // ------------------------------------------------------------------ bass

    static void bass() {
        // intro drone: sub swell on A then F
        for (int bar = 0; bar < 2; bar++) {
            double f = hz(CHORDS[bar][0]);
            int a = s(bar * 2.0), n = s(2.0);
            double ph = 0;
            for (int i = 0; i < n; i++) {
                double t = i / (double) SR + bar * 2.0;
                ph += 2 * Math.PI * f / SR;
                double env = bar == 0 ? Math.min(1, t / 1.6) : 1.0;
                if (t > 3.8) env *= Math.max(0, (3.9 - t) / 0.1);
                double v = Math.sin(ph) * 0.22 * env;
                L[a + i] += v; R[a + i] += v;
            }
        }
        // drops: sub on the root, saw bass bouncing in 8ths
        for (int bar = 2; bar < 13; bar++) {
            if (bar == 8) continue;
            double b0 = bar * Score.BAR;
            int root = CHORDS[bar][0];
            for (int e = 0; e < 8; e++) {
                double t0 = b0 + e * Score.BEAT / 2;
                double note = root + ((e % 2 == 1) ? 12 : 0);
                bassNote(t0, Score.BEAT / 2 - 0.01, note);
            }
        }
        // final note
        bassNote(26.0, 3.6, CHORDS[13][0]);
    }

    static void bassNote(double t0, double dur, double note) {
        double f = hz(note), fsub = hz(note - (note > 40 ? 12 : 0));
        int a = s(t0), n = s(dur + 0.06);
        double ph = RND.nextDouble(), ph2 = 0, ph3 = 0.3;
        Biquad lp = Biquad.lowpass(380, 1.1);
        for (int i = 0; i < n && a + i < N; i++) {
            double t = i / (double) SR;
            double env = Math.min(1, t / 0.004) * (t > dur ? Math.max(0, 1 - (t - dur) / 0.05) : 1);
            if (dur > 1) env *= Math.exp(-t / 1.4);
            double dt = f / SR;
            ph += dt; if (ph >= 1) ph -= 1;
            ph3 += dt * 1.007; if (ph3 >= 1) ph3 -= 1;
            double saw = (2 * ph - 1 - polyBlep(ph, dt)) + (2 * ph3 - 1 - polyBlep(ph3, dt * 1.007));
            ph2 += 2 * Math.PI * fsub / SR;
            lp.setLowpass(260 + 900 * Math.exp(-t / 0.05), 1.2);
            double mid = lp.process(saw * 0.5);
            double v = (Math.sin(ph2) * 0.21 + mid * 0.22) * env * side[a + i];
            L[a + i] += v;
            R[a + i] += v;
        }
    }

    // ------------------------------------------------------------------ pads

    static double padCutoff(double t) {
        if (t < 2) return 280 + 700 * t / 2;
        if (t < 4) return 980 + 2600 * Math.pow((t - 2) / 2, 2);
        if (t < 16) return 3400;
        if (t < 18) return 650 + 3000 * Math.pow((t - 16) / 2, 2.2);
        if (t < 26) return 4600;
        return 600 + 4400 * Math.exp(-(t - 26) / 1.1);
    }

    static void pads() {
        double[] detune = {-0.17, -0.08, 0, 0.08, 0.17};
        for (int bar = 0; bar < 15; bar++) {
            double b0 = bar * Score.BAR;
            if (bar == 14) continue;   // the final chord (bar 13) rings through bar 14
            double dur = bar == 13 ? 3.6 : 2.0;
            int[] chord = CHORDS[bar];
            int n = s(dur + 0.35);
            double[] bl = new double[n], br = new double[n];
            for (int v = 1; v < chord.length; v++) {
                double f = hz(chord[v]);
                for (int k = 0; k < 5; k++) {
                    double fk = f * Math.pow(2, detune[k] / 12.0), dt = fk / SR, ph = RND.nextDouble();
                    double pan = (k - 2) / 2.0;
                    double gl = Math.cos((pan + 1) * Math.PI / 4), gr = Math.sin((pan + 1) * Math.PI / 4);
                    for (int i = 0; i < n; i++) {
                        ph += dt; if (ph >= 1) ph -= 1;
                        double saw = 2 * ph - 1 - polyBlep(ph, dt);
                        bl[i] += saw * gl;
                        br[i] += saw * gr;
                    }
                }
            }
            Biquad fl = Biquad.lowpass(1000, 0.8), fr = Biquad.lowpass(1000, 0.8);
            double attack = bar < 2 ? 0.6 : bar == 8 ? 0.3 : 0.012;
            for (int i = 0; i < n; i++) {
                double t = i / (double) SR, abs = b0 + t;
                if (i % 32 == 0) {
                    double c = padCutoff(abs);
                    fl.setLowpass(c, 0.85);
                    fr.setLowpass(c, 0.85);
                }
                double env = Math.min(1, t / attack) * (t > dur ? Math.max(0, 1 - (t - dur) / 0.3) : 1);
                if (bar == 13) env *= Math.max(0, 1 - Math.max(0, t - 2.2) / 1.4);
                int k = s(b0) + i;
                double duck = k < N ? side[k] : 1;
                bl[i] = fl.process(bl[i]) * env * duck;
                br[i] = fr.process(br[i]) * env * duck;
            }
            double gain = bar < 2 ? 0.05 : bar == 8 ? 0.058 : 0.07;
            putStereo(bl, br, b0, gain, 0.45, 0.0);
        }
    }

    // ------------------------------------------------------------------ pluck arpeggio

    static void arp() {
        int[] pattern = {0, 1, 2, 3, 4, 3, 2, 1};
        for (int bar = 3; bar < 13; bar++) {
            if (bar == 8) continue;
            int[] chord = CHORDS[bar];
            int[] tones = {chord[1] + 12, chord[2] + 12, chord[3] + 12, chord[4] + 12, chord[2] + 24};
            for (int st = 0; st < 16; st++) {
                double t = bar * Score.BAR + st * Score.BEAT / 4;
                int note = tones[pattern[st % 8]];
                double vel = st % 4 == 0 ? 1.0 : 0.72;
                pluck(t, note, 0.15 * vel, st % 2 == 0 ? -0.35 : 0.35);
            }
        }
    }

    static void pluck(double t0, double note, double gain, double pan) {
        double f = hz(note), dt = f / SR, ph = 0, ph2 = 0.5;
        int n = s(0.32);
        double[] b = new double[n];
        Biquad lp = Biquad.lowpass(4000, 1.6);
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            ph += dt; if (ph >= 1) ph -= 1;
            ph2 += dt * 2.003; if (ph2 >= 1) ph2 -= 1;
            double x = (2 * ph - 1 - polyBlep(ph, dt)) * 0.7 + ((ph2 < 0.5) ? 0.3 : -0.3);
            if (i % 16 == 0) lp.setLowpass(380 + 5200 * Math.exp(-t / 0.045), 2.0);
            b[i] = lp.process(x) * Math.exp(-t / 0.11) * Math.min(1, t / 0.002);
        }
        double gl = Math.cos((pan + 1) * Math.PI / 4), gr = Math.sin((pan + 1) * Math.PI / 4);
        double[] bl = new double[n], br = new double[n];
        for (int i = 0; i < n; i++) { bl[i] = b[i] * gl; br[i] = b[i] * gr; }
        int k0 = s(t0);
        for (int i = 0; i < n && k0 + i < N; i++) { bl[i] *= 0.4 + 0.6 * side[k0 + i]; br[i] *= 0.4 + 0.6 * side[k0 + i]; }
        putStereo(bl, br, t0, gain, 0.25, 0.35);
    }

    // ------------------------------------------------------------------ FM bell lead (drop B)

    static void bell() {
        double[][] melody = {
            {18.0, 76, 0.5}, {18.5, 74, 0.25}, {18.75, 72, 0.25}, {19.0, 76, 0.5}, {19.5, 81, 0.5},
            {20.0, 81, 0.5}, {20.5, 79, 0.25}, {20.75, 77, 0.25}, {21.0, 76, 0.5}, {21.5, 72, 0.5},
            {22.0, 76, 0.5}, {22.5, 79, 0.25}, {22.75, 76, 0.25}, {23.0, 72, 0.5}, {23.5, 74, 0.5},
            {24.0, 74, 0.5}, {24.5, 71, 0.25}, {24.75, 74, 0.25}, {25.0, 79, 0.75},
            {26.0, 81, 2.0}, {26.0, 76, 2.0}, {26.0, 69, 2.0},
        };
        for (double[] m : melody) fmBell(m[0], m[1], m[2], m[0] >= 26 ? 0.13 : 0.16);
    }

    static void fmBell(double t0, double note, double len, double gain) {
        double f = hz(note);
        int n = s(len + 1.2);
        double[] b = new double[n];
        double pc = 0, pm = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            pm += 2 * Math.PI * f * 3.5 / SR;
            double index = 2.4 * Math.exp(-t / 0.18) + 0.35;
            pc += 2 * Math.PI * f / SR;
            double env = Math.min(1, t / 0.003) * Math.exp(-t / (0.55 + len * 0.4));
            b[i] = Math.sin(pc + index * Math.sin(pm)) * env + Math.sin(pc * 2) * env * 0.12;
        }
        put(b, t0, gain, 0.1, 0.4);
        int k = s(t0);
        for (int i = 0; i < n && k + i < N; i++) { dlyL[k + i] += b[i] * gain * 0.35; dlyR[k + i] += b[i] * gain * 0.35; }
    }

    // ------------------------------------------------------------------ FX

    static void effects() {
        riser(2.0, 1.875, 0.30);
        riser(16.0, 1.94, 0.32);
        riser(24.5, 1.45, 0.16);
        reverseCymbal(0.0, 1.0, 0.10);
        for (double t : Score.IMPACTS) impact(t, t == 26.0 ? 1.15 : 1.0);
        for (double t : Score.WHOOSHES) whoosh(t, 0.55, 0.32);
        downlifter(16.0, 1.1, 0.22);
    }

    static void riser(double t0, double dur, double gain) {
        int n = s(dur);
        double[] bl = new double[n], br = new double[n];
        Biquad fl = Biquad.bandpass(400, 2.0), fr = Biquad.bandpass(400, 2.0);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double p = i / (double) n;
            if (i % 32 == 0) {
                double fc = 350 * Math.pow(9000 / 350.0, p);
                fl.setBandpass(fc, 2.2);
                fr.setBandpass(fc * 1.05, 2.2);
            }
            double env = Math.pow(p, 2.2);
            double f = hz(45 + 24 * p * p);
            ph += f / SR; if (ph >= 1) ph -= 1;
            double saw = (2 * ph - 1) * 0.12 * env;
            bl[i] = fl.process(noise()) * env * 2.0 + saw;
            br[i] = fr.process(noise()) * env * 2.0 + saw;
        }
        putStereo(bl, br, t0, gain, 0.35, 0);
    }

    static void reverseCymbal(double t0, double dur, double gain) {
        int n = s(dur);
        double[] bl = new double[n], br = new double[n];
        Biquad hl = Biquad.highpass(5000, 0.7), hr = Biquad.highpass(5000, 0.7);
        for (int i = 0; i < n; i++) {
            double env = Math.pow(i / (double) n, 3);
            bl[i] = hl.process(noise()) * env;
            br[i] = hr.process(noise()) * env;
        }
        putStereo(bl, br, t0, gain, 0.5, 0);
    }

    static void impact(double t0, double scale) {
        int n = s(3.0);
        double[] b = new double[n], cl = new double[n], cr = new double[n];
        double ph = 0;
        Biquad hl = Biquad.highpass(2500, 0.7), hr = Biquad.highpass(2500, 0.7);
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            ph += 2 * Math.PI * (30 + 48 * Math.exp(-t / 0.25)) / SR;
            b[i] = Math.tanh(Math.sin(ph) * Math.exp(-t / 0.9) * 1.6) * 0.8;
            double crash = Math.exp(-t / 0.75) * 0.5 + Math.exp(-t / 0.05) * 0.5;
            cl[i] = hl.process(noise()) * crash;
            cr[i] = hr.process(noise()) * crash;
        }
        put(b, t0, 0.55 * scale, 0, 0.15);
        putStereo(cl, cr, t0, 0.20 * scale, 0.6, 0);
    }

    static void whoosh(double peak, double dur, double gain) {
        int n = s(dur);
        double t0 = peak - dur * 0.7;
        double[] bl = new double[n], br = new double[n];
        Biquad fl = Biquad.bandpass(500, 1.5), fr = Biquad.bandpass(500, 1.5);
        for (int i = 0; i < n; i++) {
            double p = i / (double) n;
            double shape = p < 0.7 ? Math.pow(p / 0.7, 2.5) : Math.pow(1 - (p - 0.7) / 0.3, 1.5);
            if (i % 32 == 0) {
                double fc = 300 + 3800 * shape;
                fl.setBandpass(fc, 1.4);
                fr.setBandpass(fc, 1.4);
            }
            double pan = -0.9 + 1.8 * p;
            double gl = Math.cos((pan + 1) * Math.PI / 4), gr = Math.sin((pan + 1) * Math.PI / 4);
            bl[i] = fl.process(noise()) * shape * gl * 2.2;
            br[i] = fr.process(noise()) * shape * gr * 2.2;
        }
        putStereo(bl, br, t0, gain, 0.3, 0);
    }

    static void downlifter(double t0, double dur, double gain) {
        int n = s(dur);
        double[] bl = new double[n], br = new double[n];
        Biquad fl = Biquad.bandpass(6000, 1.8), fr = Biquad.bandpass(6000, 1.8);
        for (int i = 0; i < n; i++) {
            double p = i / (double) n;
            if (i % 32 == 0) {
                double fc = 6000 * Math.pow(200 / 6000.0, p);
                fl.setBandpass(fc, 1.8);
                fr.setBandpass(fc, 1.8);
            }
            double env = Math.pow(1 - p, 1.5);
            bl[i] = fl.process(noise()) * env * 2;
            br[i] = fr.process(noise()) * env * 2;
        }
        putStereo(bl, br, t0, gain, 0.5, 0);
    }

    // ------------------------------------------------------------------ UI sound design

    static double[] pop(double pitch) {
        int n = s(0.16);
        double[] b = new double[n];
        double ph = 0, ph2 = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            double f = pitch * (0.55 + 0.45 * (1 - Math.exp(-t / 0.012)));
            ph += 2 * Math.PI * f / SR;
            ph2 += 2 * Math.PI * 170 / SR;
            b[i] = Math.sin(ph) * Math.exp(-t / 0.05) * Math.min(1, t / 0.002) + Math.sin(ph2) * Math.exp(-t / 0.03) * 0.6;
        }
        return b;
    }

    static double[] tick(double pitch, long seed) {
        Random r = new Random(seed);
        int n = s(0.05);
        double[] b = new double[n];
        Biquad bp = Biquad.bandpass(pitch, 4);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            ph += 2 * Math.PI * pitch / SR;
            b[i] = bp.process(r.nextDouble() * 2 - 1) * Math.exp(-t / 0.004) * 3 + Math.sin(ph) * Math.exp(-t / 0.012) * 0.5;
        }
        return b;
    }

    static double[] keyClick(long seed) {
        Random r = new Random(seed);
        int n = s(0.06);
        double[] b = new double[n];
        Biquad bp = Biquad.bandpass(3200 + r.nextInt(600), 3);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) SR;
            ph += 2 * Math.PI * 140 / SR;
            b[i] = bp.process(r.nextDouble() * 2 - 1) * Math.exp(-t / 0.008) * 3.5 + Math.sin(ph) * Math.exp(-t / 0.012) * 0.5;
        }
        return b;
    }

    static double[] glitch() {
        Random r = new Random(77);
        int n = s(0.22);
        double[] b = new double[n];
        double hold = 0;
        for (int i = 0; i < n; i++) {
            if (i % 180 == 0) hold = (r.nextDouble() * 2 - 1);
            double t = i / (double) SR;
            double gate = ((int) (t / 0.018)) % 2 == 0 ? 1 : 0.15;
            b[i] = Math.round(hold * 4) / 4.0 * gate * Math.exp(-t / 0.12) + Math.sin(2 * Math.PI * 95 * t) * gate * Math.exp(-t / 0.1) * 0.6;
        }
        return b;
    }

    static void uiSounds() {
        // intro boxes: soft ticks rising in pitch
        double[] boxes = Score.INTRO_BOXES;
        for (int i = 0; i < boxes.length; i++) {
            put(tick(1800 + 1400.0 * i / boxes.length, 300 + i), boxes[i], 0.10, (i % 3 - 1) * 0.5, 0.25);
        }
        for (int i = 0; i < Score.CARD_POPS.length; i++) put(pop(900 + 120 * i), Score.CARD_POPS[i], 0.30, -0.4 + 0.27 * i, 0.15);
        for (int i = 0; i < Score.BAR_GROW.length; i++) put(pop(1300 + 260 * i), Score.BAR_GROW[i], 0.22, 0.3, 0.2);
        for (double t : Score.ALERT_PINGS) { put(tick(2600, 51), t, 0.16, 0.5, 0.4); put(tick(3900, 52), t + 0.06, 0.10, 0.5, 0.4); }
        for (int i = 0; i < Score.MOVE_TILES.length; i++) put(pop(700 + 180 * i), Score.MOVE_TILES[i], 0.30, -0.5 + 0.5 * i, 0.2);
        for (int i = 0; i < Score.ROW_SLIDES.length; i++) put(tick(2200 + 90 * i, 400 + i), Score.ROW_SLIDES[i], 0.11, 0.35, 0.15);
        put(glitch(), Score.VOID_GLITCH, 0.16, 0.2, 0.1);
        for (int i = 0; i < Score.ORBIT_TICKS.length; i++) put(tick(1500 + 140 * i, 500 + i), Score.ORBIT_TICKS[i], 0.07 + 0.01 * i, Math.sin(i) * 0.6, 0.4);
        for (int i = 0; i < Score.TYPE_KEYS.length; i++) put(keyClick(600 + i), Score.TYPE_KEYS[i], 0.22, 0.1, 0.05);
        put(pop(1500), Score.SEARCH_HIT, 0.26, 0, 0.25);
        put(pop(2250), Score.SEARCH_HIT + 0.07, 0.18, 0, 0.25);
        for (int i = 0; i < Score.ROLE_FLIPS.length; i++) put(pop(800 + 200 * i), Score.ROLE_FLIPS[i], 0.26, -0.5 + 0.5 * i, 0.2);
        for (int i = 0; i < Score.REPORT_TILES.length; i++) put(tick(1600 + 260 * i, 700 + i), Score.REPORT_TILES[i], 0.10, -0.6 + 0.2 * i, 0.2);
        for (int i = 0; i < Score.TECH_CHIPS.length; i++) put(pop(1000 + 150 * i), Score.TECH_CHIPS[i], 0.20, -0.45 + 0.3 * i, 0.15);
        for (int i = 0; i < Score.ASSEMBLE.length; i++) put(tick(1200 + 70 * i, 800 + i), Score.ASSEMBLE[i], 0.12, (i % 2 == 0) ? -0.4 : 0.4, 0.2);
        put(tick(5200, 901), Score.SWEEP, 0.10, -0.6, 0.6);
    }

    // ------------------------------------------------------------------ reverb, delay, master

    static void mixdown() {
        // ping-pong delay (dotted eighth)
        int d = s(Score.BEAT * 0.75);
        double[] bl = new double[N], br = new double[N];
        double lpl = 0, lpr = 0;
        for (int i = 0; i < N; i++) {
            double fbL = i >= d ? br[i - d] : 0, fbR = i >= d ? bl[i - d] : 0;
            lpl += (fbL - lpl) * 0.35;
            lpr += (fbR - lpr) * 0.35;
            bl[i] = dlyL[i] + lpl * 0.42;
            br[i] = dlyR[i] + lpr * 0.42;
            L[i] += lpl * 0.55;
            R[i] += lpr * 0.55;
            revL[i] += lpl * 0.2;
            revR[i] += lpr * 0.2;
        }
        // Freeverb-style reverb
        double[] wl = freeverb(revL, 0), wr = freeverb(revR, 23);
        for (int i = 0; i < N; i++) {
            L[i] += wl[i] * 0.32;
            R[i] += wr[i] * 0.32;
        }
        // DC / rumble high-pass
        Biquad hpl = Biquad.highpass(28, 0.7), hpr = Biquad.highpass(28, 0.7);
        for (int i = 0; i < N; i++) { L[i] = hpl.process(L[i]); R[i] = hpr.process(R[i]); }
        // glue compressor
        double env = 0, att = Math.exp(-1 / (0.004 * SR)), rel = Math.exp(-1 / (0.14 * SR));
        double peak = 0;
        for (int i = 0; i < N; i++) peak = Math.max(peak, Math.max(Math.abs(L[i]), Math.abs(R[i])));
        double pre = 1.0 / peak;
        for (int i = 0; i < N; i++) {
            double x = Math.max(Math.abs(L[i]), Math.abs(R[i])) * pre;
            env = x > env ? att * env + (1 - att) * x : rel * env + (1 - rel) * x;
            double thr = 0.42, gain = 1;
            if (env > thr) gain = Math.pow(env / thr, 1 / 3.0 - 1);
            L[i] *= pre * gain;
            R[i] *= pre * gain;
        }
        // soft clip + normalize + end fade
        peak = 0;
        for (int i = 0; i < N; i++) {
            L[i] = Math.tanh(L[i] * 1.6);
            R[i] = Math.tanh(R[i] * 1.6);
            peak = Math.max(peak, Math.max(Math.abs(L[i]), Math.abs(R[i])));
        }
        double norm = 0.94 / peak;
        for (int i = 0; i < N; i++) {
            double t = i / (double) SR;
            double fade = t > 29.2 ? Math.max(0, (30.0 - t) / 0.8) : 1;
            L[i] *= norm * fade;
            R[i] *= norm * fade;
        }
    }

    static double[] freeverb(double[] in, int spread) {
        double scale = SR / 44100.0;
        int[] combs = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
        int[] aps = {556, 441, 341, 225};
        double[] out = new double[N];
        double room = 0.86, damp = 0.35;
        for (int c : combs) {
            int len = (int) ((c + spread) * scale);
            double[] buf = new double[len];
            int idx = 0;
            double store = 0;
            for (int i = 0; i < N; i++) {
                double y = buf[idx];
                store = y * (1 - damp) + store * damp;
                buf[idx] = in[i] * 0.015 + store * room;
                out[i] += y;
                if (++idx >= len) idx = 0;
            }
        }
        for (int a : aps) {
            int len = (int) ((a + spread) * scale);
            double[] buf = new double[len];
            int idx = 0;
            for (int i = 0; i < N; i++) {
                double bo = buf[idx];
                double y = -out[i] + bo;
                buf[idx] = out[i] + bo * 0.5;
                out[i] = y;
                if (++idx >= len) idx = 0;
            }
        }
        return out;
    }

    static void writeWav(File f) throws IOException {
        f.getParentFile().mkdirs();
        try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            int dataLen = N * 4;
            o.writeBytes("RIFF"); o.writeInt(Integer.reverseBytes(36 + dataLen)); o.writeBytes("WAVE");
            o.writeBytes("fmt "); o.writeInt(Integer.reverseBytes(16));
            o.writeShort(Short.reverseBytes((short) 1)); o.writeShort(Short.reverseBytes((short) 2));
            o.writeInt(Integer.reverseBytes(SR)); o.writeInt(Integer.reverseBytes(SR * 4));
            o.writeShort(Short.reverseBytes((short) 4)); o.writeShort(Short.reverseBytes((short) 16));
            o.writeBytes("data"); o.writeInt(Integer.reverseBytes(dataLen));
            Random dither = new Random(5);
            for (int i = 0; i < N; i++) {
                double d1 = (dither.nextDouble() - dither.nextDouble()) / 32768.0;
                double d2 = (dither.nextDouble() - dither.nextDouble()) / 32768.0;
                o.writeShort(Short.reverseBytes((short) Math.round(Math.max(-1, Math.min(1, L[i] + d1)) * 32767)));
                o.writeShort(Short.reverseBytes((short) Math.round(Math.max(-1, Math.min(1, R[i] + d2)) * 32767)));
            }
        }
    }
}
