package cz.cvut.fit.acb.mixing;

/**
 * The logistic domain the models of a bit are mixed in: {@code stretch(p) = ln(p / (1 - p))} and its
 * inverse {@code squash}, over 12-bit probabilities and stretches in {@code -2047 .. 2047} (units of 1/256).
 * Integer tables only, so that the encoder and the decoder compute the same on every machine.
 */
public final class Logistic {

    /** A probability is a number of {@code 2^-12}ths. */
    public static final int BITS = 12;
    public static final int ONE = 1 << BITS;
    public static final int MAX_STRETCH = 2047;

    /** {@code squash} at every 128th stretch from -2048 to 2048, from which the rest is interpolated. */
    private static final int[] SQUASH_KNOTS = squashKnots();
    private static final int[] STRETCH = stretchTable();

    private Logistic() {
    }

    /** The probability, in {@code 1 .. 4095}, of a stretch; stretches past the range saturate. */
    public static int squash(int stretch) {
        if (stretch > MAX_STRETCH) {
            return ONE - 1;
        }
        if (stretch < -MAX_STRETCH) {
            return 1;
        }
        int fraction = stretch & 127;
        int knot = (stretch >> 7) + 16;
        return (SQUASH_KNOTS[knot] * (128 - fraction) + SQUASH_KNOTS[knot + 1] * fraction + 64) >> 7;
    }

    /** The stretch of a probability in {@code 0 .. 4095}: the inverse of {@link #squash}. */
    public static int stretch(int probability) {
        return STRETCH[probability];
    }

    /** {@code 4096 / (1 + e^(-x / 256))}, rounded, at {@code x = -2048, -1920, ..., 2048}; strict, so alike everywhere. */
    private static int[] squashKnots() {
        int[] knots = new int[33];
        for (int i = 0; i < knots.length; i++) {
            double value = ONE / (1 + StrictMath.exp(-(i - 16) * 128 / 256.0));
            knots[i] = (int) Math.max(1, Math.min(ONE - 1, StrictMath.round(value)));
        }
        return knots;
    }

    private static int[] stretchTable() {
        int[] table = new int[ONE];
        int at = 0;
        for (int stretch = -MAX_STRETCH; stretch <= MAX_STRETCH; stretch++) {
            int probability = squash(stretch);
            for (int p = at; p <= probability; p++) {
                table[p] = stretch;
            }
            at = probability + 1;
        }
        for (int p = at; p < ONE; p++) {
            table[p] = MAX_STRETCH;
        }
        return table;
    }
}
