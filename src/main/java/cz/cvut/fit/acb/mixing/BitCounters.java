package cz.cvut.fit.acb.mixing;

import java.util.Arrays;

/**
 * The probability that a bit is 1 in each of a number of contexts, learnt from the bits seen in it: fast
 * at first, each bit moving the estimate by {@code 1 / (n + 1.5)} of the way after {@code n} bits, and at a
 * steady rate once {@code n} reaches the limit, so that a context follows its recent past. A table is
 * updated at every bit it predicts, so it keeps a primitive array and changes it in place. A context's
 * probability and count share one {@code int}, so that the large hashed tables of the mixed associative
 * coder miss the cache once per bit rather than twice: that coder measured 4% faster over Calgary, where a
 * JFR profile had put an eighth of its time in these two methods.
 */
public final class BitCounters {

    private static final int PRECISION = 16;
    private static final int HALF = 1 << (PRECISION - 1);
    private static final int[] RATE = rates();
    /** The count takes the low byte of a context's state, the probability the bits above it. */
    private static final int COUNT_BITS = 8;
    private static final int COUNT_MASK = (1 << COUNT_BITS) - 1;

    private final int[] states;
    private final int limit;

    /**
     * @param contexts how many contexts the table has
     * @param limit after how many bits of a context its rate stops falling, at most {@code 255}
     */
    public BitCounters(int contexts, int limit) {
        this.states = new int[contexts];
        this.limit = limit;
        Arrays.fill(this.states, HALF << COUNT_BITS);
    }

    public int contexts() {
        return this.states.length;
    }

    /** The 12-bit probability that the next bit of {@code context} is 1. */
    public int probability(int context) {
        return this.states[context] >>> (COUNT_BITS + PRECISION - Logistic.BITS);
    }

    /** How many bits {@code context} has seen, up to the limit. */
    public int count(int context) {
        return this.states[context] & COUNT_MASK;
    }

    public void update(int context, int bit) {
        int state = this.states[context];
        int count = state & COUNT_MASK;
        int probability = state >>> COUNT_BITS;
        int target = bit != 0 ? (1 << PRECISION) - 1 : 0;
        probability += (int) (((long) (target - probability) * RATE[count]) >> PRECISION);
        this.states[context] = probability << COUNT_BITS | (count < this.limit ? count + 1 : count);
    }

    private static int[] rates() {
        int[] rates = new int[256];
        for (int n = 0; n < rates.length; n++) {
            rates[n] = (int) ((1L << PRECISION) * 2 / (2 * n + 3));
        }
        return rates;
    }
}
