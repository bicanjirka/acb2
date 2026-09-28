package cz.cvut.fit.acb.coding;

import java.util.Arrays;

/**
 * Symbol frequencies that grow as symbols are seen and are halved when their total would pass a
 * limit, so the model follows the recent past. Cumulative frequencies come from a Fenwick tree.
 * The increment and the limit are part of the stream format.
 */
public final class AdaptiveFrequencyModel {

    /** What one occurrence adds; the starting frequencies of a field are in the same unit. */
    static final int INCREMENT = 32;
    static final int MIN_LIMIT = 1 << 16;
    /** A wide alphabet keeps history for longer: this many increments per symbol before the halving. */
    static final int SYMBOL_ALLOWANCE = 256;

    private final int[] frequencies;
    private final int[] tree;
    private final int limit;
    private final int highestStep;
    private int total;

    /**
     * @param initial the starting frequency of every symbol, each at least 1
     */
    public AdaptiveFrequencyModel(int[] initial) {
        this.frequencies = initial.clone();
        this.tree = new int[initial.length + 1];
        this.limit = limitFor(initial.length);
        this.highestStep = Integer.highestOneBit(initial.length);
        for (int frequency : this.frequencies) {
            this.total += frequency;
        }
        while (this.total > this.limit) {
            this.halve();
        }
        this.rebuildTree();
    }

    /** The most the frequencies of a model of {@code symbols} symbols may total. */
    public static int limitFor(int symbols) {
        return Math.min(RangeEncoder.MAX_TOTAL, Math.max(MIN_LIMIT, SYMBOL_ALLOWANCE * symbols));
    }

    public int total() {
        return this.total;
    }

    public int frequency(int symbol) {
        return this.frequencies[symbol];
    }

    /** The frequencies of all the symbols before {@code symbol}. */
    public int cumulative(int symbol) {
        int sum = 0;
        for (int i = symbol; i > 0; i -= i & -i) {
            sum += this.tree[i];
        }
        return sum;
    }

    /** The symbol whose cumulative range contains {@code target}, which is below {@link #total()}. */
    public int symbolAt(int target) {
        int position = 0;
        int remaining = target;
        for (int step = this.highestStep; step > 0; step >>= 1) {
            int next = position + step;
            if (next < this.tree.length && this.tree[next] <= remaining) {
                position = next;
                remaining -= this.tree[next];
            }
        }
        return position;
    }

    public void increment(int symbol) {
        if (this.total + INCREMENT > this.limit) {
            this.halve();
            this.rebuildTree();
        }
        this.frequencies[symbol] += INCREMENT;
        this.total += INCREMENT;
        for (int i = symbol + 1; i < this.tree.length; i += i & -i) {
            this.tree[i] += INCREMENT;
        }
    }

    private void halve() {
        this.total = 0;
        for (int i = 0; i < this.frequencies.length; i++) {
            this.frequencies[i] = (this.frequencies[i] + 1) >>> 1;
            this.total += this.frequencies[i];
        }
    }

    private void rebuildTree() {
        Arrays.fill(this.tree, 0);
        for (int i = 1; i < this.tree.length; i++) {
            this.tree[i] += this.frequencies[i - 1];
            int parent = i + (i & -i);
            if (parent < this.tree.length) {
                this.tree[parent] += this.tree[i];
            }
        }
    }
}
