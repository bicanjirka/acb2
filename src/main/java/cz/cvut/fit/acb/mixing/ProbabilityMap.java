package cz.cvut.fit.acb.mixing;

/**
 * Refines a probability by what followed it before in the same small context: a table from the stretch
 * of the probability, in 33 steps with interpolation between the two nearest, to the rate at which 1
 * came after it (an adaptive probability map, or secondary estimation). Each context starts as the
 * identity.
 */
public final class ProbabilityMap {

    private static final int STEPS = 33;

    private final int[] table;
    private final int rate;
    private int index;

    /** @param rate how slowly the map learns: each bit moves it by {@code 2^-rate} of the way */
    public ProbabilityMap(int contexts, int rate) {
        this.table = new int[contexts * STEPS];
        this.rate = rate;
        for (int context = 0; context < contexts; context++) {
            for (int step = 0; step < STEPS; step++) {
                this.table[context * STEPS + step] = Logistic.squash((step - 16) * 128) * 16;
            }
        }
    }

    /** @return the refined 12-bit probability of {@code probability} in {@code context} */
    public int refine(int probability, int context) {
        int stretch = Logistic.stretch(probability) + 2048;
        int fraction = stretch & 127;
        this.index = (stretch >> 7) + context * STEPS;
        int refined = (this.table[this.index] * (128 - fraction) + this.table[this.index + 1] * fraction) >> 11;
        if (fraction >= 64) {
            this.index++;
        }
        return Math.max(1, Math.min(Logistic.ONE - 1, refined));
    }

    /** Learns the bit that followed the last {@link #refine}. */
    public void update(int bit) {
        int target = bit != 0 ? (1 << 16) - 1 : 0;
        this.table[this.index] += (target - this.table[this.index]) >> this.rate;
    }
}
