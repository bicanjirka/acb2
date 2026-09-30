package cz.cvut.fit.acb.mixing;

import java.util.Arrays;

/**
 * The probability that a bit is 1 in each of a number of contexts, learnt from the bits seen in it: fast
 * at first, each bit moving the estimate by {@code 1 / (n + 1.5)} of the way after {@code n} bits, and at a
 * steady rate once {@code n} reaches the limit, so that a context follows its recent past. A table is
 * updated at every bit it predicts, so it keeps primitive arrays and changes them in place.
 */
public final class BitCounters {

    private static final int PRECISION = 16;
    private static final int HALF = 1 << (PRECISION - 1);
    private static final int[] RATE = rates();

    private final int[] probabilities;
    private final byte[] counts;
    private final int limit;

    /**
     * @param contexts how many contexts the table has
     * @param limit after how many bits of a context its rate stops falling, at most {@code 255}
     */
    public BitCounters(int contexts, int limit) {
        this.probabilities = new int[contexts];
        this.counts = new byte[contexts];
        this.limit = limit;
        Arrays.fill(this.probabilities, HALF);
    }

    public int contexts() {
        return this.probabilities.length;
    }

    /** The 12-bit probability that the next bit of {@code context} is 1. */
    public int probability(int context) {
        return this.probabilities[context] >>> (PRECISION - Logistic.BITS);
    }

    /** How many bits {@code context} has seen, up to the limit. */
    public int count(int context) {
        return this.counts[context] & 0xFF;
    }

    public void update(int context, int bit) {
        int count = this.counts[context] & 0xFF;
        int target = bit != 0 ? (1 << PRECISION) - 1 : 0;
        this.probabilities[context] += (int) (((long) (target - this.probabilities[context]) * RATE[count]) >> PRECISION);
        if (count < this.limit) {
            this.counts[context] = (byte) (count + 1);
        }
    }

    private static int[] rates() {
        int[] rates = new int[256];
        for (int n = 0; n < rates.length; n++) {
            rates[n] = (int) ((1L << PRECISION) * 2 / (2 * n + 3));
        }
        return rates;
    }
}
