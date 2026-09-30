package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.counts.FenwickTree;
import cz.cvut.fit.acb.dictionary.ByteSet;

/**
 * Symbol frequencies that grow as symbols are seen and are halved when their total would pass a
 * limit, so the model follows the recent past: {@link FrequencyCounts}, with the cumulative
 * frequencies from a Fenwick tree.
 * The increment and the limit are part of the stream format.
 */
public final class AdaptiveFrequencyModel {

    /** What one occurrence adds; the starting frequencies of a field are in the same unit. */
    static final int INCREMENT = FrequencyCounts.INCREMENT;

    private final FrequencyCounts counts;
    private final int[] frequencies;
    private final FenwickTree tree;

    /**
     * @param initial the starting frequency of every symbol, each at least 1
     */
    public AdaptiveFrequencyModel(int[] initial) {
        this.counts = new FrequencyCounts(initial);
        this.frequencies = this.counts.frequencies();
        this.tree = new FenwickTree(initial.length);
        this.tree.rebuild(this.frequencies, this.frequencies.length);
    }

    /** The most the frequencies of a model of {@code symbols} symbols may total. */
    public static int limitFor(int symbols) {
        return FrequencyCounts.limitFor(symbols);
    }

    public int total() {
        return this.counts.total();
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

    /** The total of the frequencies without the symbols of {@code excluded}. */
    public int totalWithout(ByteSet excluded) {
        int total = this.counts.total();
        for (int symbol = excluded.next(0); symbol >= 0; symbol = excluded.next(symbol + 1)) {
            total -= this.frequencies[symbol];
        }
        return total;
    }

    /** {@link #cumulative} in a model that leaves the symbols of {@code excluded} out; {@code symbol} is not one of them. */
    public int cumulativeWithout(int symbol, ByteSet excluded) {
        int sum = this.cumulative(symbol);
        for (int left = excluded.next(0); left >= 0 && left < symbol; left = excluded.next(left + 1)) {
            sum -= this.frequencies[left];
        }
        return sum;
    }

    /** {@link #symbolAt} in a model that leaves the symbols of {@code excluded} out. */
    public int symbolAtWithout(int target, ByteSet excluded) {
        int removed = 0;
        for (int left = excluded.next(0); left >= 0; left = excluded.next(left + 1)) {
            if (target < this.cumulative(left) - removed) {
                break;
            }
            removed += this.frequencies[left];
        }
        return this.symbolAt(target + removed);
    }

    public void increment(int symbol) {
        if (this.counts.increment(symbol)) {
            this.tree.rebuild(this.frequencies, this.frequencies.length);
        } else {
            this.tree.add(symbol, INCREMENT);
        }
    }
}
