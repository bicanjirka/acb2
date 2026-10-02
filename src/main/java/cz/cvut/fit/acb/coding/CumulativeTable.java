package cz.cvut.fit.acb.coding;

import java.util.Arrays;

/**
 * A distribution made afresh for one symbol, from frequencies the caller sets one by one or copies from a
 * model: the cumulative sums the range coder needs, and the symbol a target falls in. A frequency of 0 makes
 * a symbol impossible. One table is refilled at every step of a segment, so it keeps its arrays and
 * overwrites them in place. The cumulative sums are found only as far as they are asked for, since coding
 * one symbol needs those up to it: over Calgary the literals of {@code acb} take symbols in the lower half and
 * the lengths of its matches the first few. The first sum asked for is added up without keeping the sums on
 * the way, from whichever end is nearer, which is all an encoder asks; from the second on they are kept. It
 * is not shared between threads.
 */
public final class CumulativeTable {

    private int[] frequencies = new int[0];
    private int[] cumulative = new int[1];
    private int size;
    /** The frequencies added up, kept as they are set, so that ending a distribution needs no pass over them. */
    private long total;
    /** How many of the cumulative sums are found: those of the symbols up to this one. */
    private int summed;
    /** Whether a cumulative sum was asked for since the distribution ended. */
    private boolean asked;

    /** Starts a distribution over {@code symbols} symbols, all of frequency 0 until the caller sets them. */
    public void begin(int symbols) {
        if (this.frequencies.length < symbols) {
            this.frequencies = new int[symbols];
            this.cumulative = new int[symbols + 1];
        } else {
            Arrays.fill(this.frequencies, 0, symbols, 0);
        }
        this.size = symbols;
        this.total = 0;
    }

    /**
     * Starts a distribution over the first {@code symbols} symbols of {@code counts}, with their frequencies
     * there, which the caller may then change.
     */
    public void begin(FrequencyCounts counts, int symbols) {
        if (this.frequencies.length < symbols) {
            this.frequencies = new int[symbols];
            this.cumulative = new int[symbols + 1];
        }
        int[] counted = counts.frequencies();
        System.arraycopy(counted, 0, this.frequencies, 0, symbols);
        long total = counts.total();
        for (int symbol = symbols; symbol < counted.length; symbol++) {
            total -= counted[symbol];
        }
        this.size = symbols;
        this.total = total;
    }

    public void set(int symbol, int frequency) {
        this.total += frequency - this.frequencies[symbol];
        this.frequencies[symbol] = frequency;
    }

    /**
     * Ends the distribution. The frequencies are halved, never to 0, until their total fits what the
     * range coder codes against.
     *
     * @return whether some symbol is possible
     */
    public boolean seal() {
        long total = this.total;
        while (total > RangeEncoder.MAX_TOTAL) {
            for (int i = 0; i < this.size; i++) {
                this.frequencies[i] = (this.frequencies[i] + 1) >>> 1;
            }
            total = this.sum();
        }
        this.total = total;
        this.summed = 0;
        this.asked = false;
        return total > 0;
    }

    public int total() {
        return (int) this.total;
    }

    public int frequency(int symbol) {
        return this.frequencies[symbol];
    }

    /** The frequencies of all the symbols before {@code symbol}. */
    public int cumulative(int symbol) {
        if (symbol <= this.summed) {
            return this.cumulative[symbol];
        }
        if (!this.asked) {
            this.asked = true;
            return symbol <= this.size / 2 ? this.sum(0, symbol) : (int) (this.total - this.sum(symbol, this.size));
        }
        this.sumUpTo(symbol);
        return this.cumulative[symbol];
    }

    /** The symbol whose range contains {@code target}, which is below {@link #total()}. */
    public int symbolAt(int target) {
        if (this.cumulative[this.summed] <= target) {
            int[] frequencies = this.frequencies;
            int[] cumulative = this.cumulative;
            int at = this.summed;
            int sum = cumulative[at];
            while (sum <= target) {
                sum += frequencies[at++];
                cumulative[at] = sum;
            }
            this.summed = at;
            return at - 1;
        }
        int low = 0;
        int high = this.summed - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (this.cumulative[middle + 1] > target) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return low;
    }

    private void sumUpTo(int symbol) {
        int[] frequencies = this.frequencies;
        int[] cumulative = this.cumulative;
        int sum = cumulative[this.summed];
        for (int at = this.summed; at < symbol; at++) {
            sum += frequencies[at];
            cumulative[at + 1] = sum;
        }
        this.summed = symbol;
    }

    private long sum() {
        long total = 0;
        for (int i = 0; i < this.size; i++) {
            total += this.frequencies[i];
        }
        return total;
    }

    /** The frequencies of the symbols {@code from .. to - 1} added up, in a sealed table, whose total fits an int. */
    private int sum(int from, int to) {
        int[] frequencies = this.frequencies;
        int sum = 0;
        for (int i = from; i < to; i++) {
            sum += frequencies[i];
        }
        return sum;
    }
}
