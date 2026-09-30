package cz.cvut.fit.acb.mixing;

import java.util.Arrays;

/**
 * Mixes the predictions of a bit in the logistic domain: the output is the squash of a weighted sum of
 * the inputs' stretches, and after the bit each weight moves along its input's share of the error, so the
 * inputs that predicted well gain. The weights come in banks, each of several sets, and a small context
 * picks the set of each bank; the banks mix apart, each learns from its own error, and the output is the
 * average of their stretches. The inputs are given anew for every bit, into arrays that are overwritten in
 * place. Mixing and learning take about a third of the time of the mixed associative coder (a JFR profile
 * over Calgary); keeping all the banks in one array measured no faster than an array per bank.
 */
public final class Mixer {

    /** A weight of 1.0. */
    private static final int UNIT = 1 << 16;

    private final int inputs;
    /** The weights of every set of every bank, one after the other. */
    private final int[] weights;
    /** Where the sets of each bank start in {@link #weights}. */
    private final int[] banks;
    private final int[] stretches;
    private final int[] selected;
    private final int[] outputs;
    private final int rate;
    private int added;

    /**
     * @param inputs how many inputs every bit has
     * @param rate how fast the weights learn: a rate of {@code r} is a step of about {@code r / 4096}
     * @param initialWeight every weight at the start, where {@code 65536} is 1.0
     * @param sets how many sets of weights each bank has
     */
    public Mixer(int inputs, int rate, int initialWeight, int... sets) {
        this.inputs = inputs;
        this.banks = new int[sets.length];
        int size = 0;
        for (int bank = 0; bank < sets.length; bank++) {
            this.banks[bank] = size;
            size += inputs * sets[bank];
        }
        this.weights = new int[size];
        Arrays.fill(this.weights, initialWeight);
        this.stretches = new int[inputs];
        this.selected = new int[sets.length];
        this.outputs = new int[sets.length];
        this.rate = rate;
    }

    /** Adds the next input, a stretch. */
    public void add(int stretch) {
        this.stretches[this.added++] = stretch;
    }

    /** Adds the next input as a 12-bit probability. */
    public void addProbability(int probability) {
        this.add(Logistic.stretch(probability));
    }

    /** Picks the set of weights {@code bank} mixes the next bit with. */
    public void select(int bank, int set) {
        this.selected[bank] = this.banks[bank] + set * this.inputs;
    }

    /**
     * Mixes the inputs added since the last bit with the weights selected.
     *
     * @return the 12-bit probability that the bit is 1
     */
    public int mix() {
        if (this.added != this.inputs) {
            throw new IllegalStateException("A bit needs " + this.inputs + " inputs, not " + this.added);
        }
        int[] weights = this.weights;
        int[] stretches = this.stretches;
        int inputs = this.inputs;
        int sum = 0;
        for (int bank = 0; bank < this.selected.length; bank++) {
            int offset = this.selected[bank];
            long dot = 0;
            for (int i = 0; i < inputs; i++) {
                dot += (long) stretches[i] * weights[offset + i];
            }
            int stretch = (int) Math.max(-Logistic.MAX_STRETCH, Math.min(Logistic.MAX_STRETCH, dot >> 16));
            this.outputs[bank] = Logistic.squash(stretch);
            sum += stretch;
        }
        return Logistic.squash(sum / this.selected.length);
    }

    /** Learns the bit that followed the last {@link #mix}, and makes ready for the next bit's inputs. */
    public void update(int bit) {
        int[] weights = this.weights;
        int[] stretches = this.stretches;
        int inputs = this.inputs;
        for (int bank = 0; bank < this.selected.length; bank++) {
            int offset = this.selected[bank];
            int error = ((bit << Logistic.BITS) - this.outputs[bank]) * this.rate;
            for (int i = 0; i < inputs; i++) {
                weights[offset + i] += (stretches[i] * error + (UNIT >> 1)) >> 16;
            }
        }
        this.added = 0;
    }
}
