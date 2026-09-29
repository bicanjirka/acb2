package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.counts.FenwickTree;

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
    private final FenwickTree tree;
    private final int limit;
    private int total;

    /**
     * @param initial the starting frequency of every symbol, each at least 1
     */
    public AdaptiveFrequencyModel(int[] initial) {
        this.frequencies = initial.clone();
        this.tree = new FenwickTree(initial.length);
        this.limit = limitFor(initial.length);
        for (int frequency : this.frequencies) {
            this.total += frequency;
        }
        while (this.total > this.limit) {
            this.halve();
        }
        this.tree.rebuild(this.frequencies, this.frequencies.length);
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
        return this.tree.sumBefore(symbol);
    }

    /** The symbol whose cumulative range contains {@code target}, which is below {@link #total()}. */
    public int symbolAt(int target) {
        return this.tree.indexAt(target);
    }

    /** The total of the frequencies without symbol {@code excluded}; nothing is left out if it is -1. */
    public int totalWithout(int excluded) {
        return excluded < 0 ? this.total : this.total - this.frequencies[excluded];
    }

    /** {@link #cumulative} in a model that leaves symbol {@code excluded} out; {@code symbol} is not that symbol. */
    public int cumulativeWithout(int symbol, int excluded) {
        int sum = this.cumulative(symbol);
        return excluded >= 0 && symbol > excluded ? sum - this.frequencies[excluded] : sum;
    }

    /** {@link #symbolAt} in a model that leaves symbol {@code excluded} out. */
    public int symbolAtWithout(int target, int excluded) {
        if (excluded >= 0 && target >= this.cumulative(excluded)) {
            return this.symbolAt(target + this.frequencies[excluded]);
        }
        return this.symbolAt(target);
    }

    public void increment(int symbol) {
        if (this.total + INCREMENT > this.limit) {
            this.halve();
            this.tree.rebuild(this.frequencies, this.frequencies.length);
        }
        this.frequencies[symbol] += INCREMENT;
        this.total += INCREMENT;
        this.tree.add(symbol, INCREMENT);
    }

    private void halve() {
        this.total = 0;
        for (int i = 0; i < this.frequencies.length; i++) {
            this.frequencies[i] = (this.frequencies[i] + 1) >>> 1;
            this.total += this.frequencies[i];
        }
    }
}
