/**
 * The shared timing sheet of the reel. Music and picture both read these
 * times, so every visual hit lands exactly on its sound.
 * 120 BPM: 1 beat = 0.5 s, 1 bar = 2 s, 15 bars = 30 s.
 */
final class Score {
    static final double BPM = 120;
    static final double BEAT = 60.0 / BPM;
    static final double BAR = BEAT * 4;
    static final double LENGTH = 30.0;

    // sections
    static final double INTRO = 0, DROP_A = 4, DASH = 8, MOVES = 12, BREAK = 16, DROP_B = 18, ASSEMBLY = 22, OUTRO = 26;

    /** Big hits: shockwave, flash, camera shake, chromatic aberration. */
    static final double[] IMPACTS = {4.0, 18.0, 26.0};
    /** Transition whooshes (time of the loudest point). */
    static final double[] WHOOSHES = {7.875, 11.875, 15.85, 21.875, 25.75};

    // UI sound + motion events
    static final double[] INTRO_BOXES = steps(1.0, 3.5, BEAT / 8);        // isometric boxes pop in
    static final double[] CARD_POPS = {8.0, 8.5, 9.0, 9.5};        // stat cards land
    static final double[] BAR_GROW = {10.0, 10.25, 10.5};          // chart bars grow
    static final double[] ALERT_PINGS = {11.0, 11.5};              // low-stock badge pulses
    static final double[] MOVE_TILES = {12.0, 12.5, 13.0};         // receiving / releasing / transfer tiles
    static final double[] ROW_SLIDES = eighths(13.5, 15.5);        // history rows slide in
    static final double VOID_GLITCH = 15.0;                        // one row becomes VOIDED
    static final double[] ORBIT_TICKS = eighths(16.0, 17.75);      // breakdown icon ticks
    static final double[] TYPE_KEYS = {18.25, 18.375, 18.5, 18.625, 18.75, 18.875}; // typing "coffee"
    static final double SEARCH_HIT = 19.0;                         // the search result locks
    static final double[] ROLE_FLIPS = {19.5, 19.75, 20.0};        // admin / manager / staff cards
    static final double[] REPORT_TILES = steps(20.5, 21.25, BEAT / 8);  // report tiles cascade
    static final double[] TECH_CHIPS = {21.25, 21.5, 21.625, 21.75};
    static final double[] ASSEMBLE = eighths(22.0, 24.5);          // UI pieces lock into place
    static final double SWEEP = 25.0;                              // light sweep over the finished UI

    static double[] eighths(double from, double to) { return steps(from, to, BEAT / 2); }

    static double[] sixteenths(double from, double to) { return steps(from, to, BEAT / 4); }

    static double[] steps(double from, double to, double step) {
        int n = (int) Math.round((to - from) / step);
        double[] r = new double[n];
        for (int i = 0; i < n; i++) r[i] = from + i * step;
        return r;
    }

    /** true while the four-on-the-floor kick is playing at time t. */
    static boolean kickSection(double t) {
        return (t >= 4.0 && t < 16.0) || (t >= 18.0 && t < 26.0);
    }

    /** All kick drum times (used for the sidechain pump and the visual pulse). */
    static double[] kicks() {
        java.util.List<Double> k = new java.util.ArrayList<>();
        k.add(2.0);
        k.add(3.0);
        for (double t = 4.0; t < 16.0 - 1e-9; t += BEAT) k.add(t);
        for (double t = 18.0; t < 26.0 - 1e-9; t += BEAT) k.add(t);
        k.add(26.0);
        double[] r = new double[k.size()];
        for (int i = 0; i < r.length; i++) r[i] = k.get(i);
        return r;
    }

    /** Pulse 1 -> 0 after the most recent kick (for beat-synced breathing). */
    static double kickPulse(double t, double decay) {
        double last = -100;
        for (double k : kicks()) if (k <= t) last = k;
        return Math.exp(-(t - last) / decay);
    }

    private Score() {
    }
}
