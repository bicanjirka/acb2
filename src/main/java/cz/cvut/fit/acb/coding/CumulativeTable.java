package cz.cvut.fit.acb.coding;

/**
 * A distribution made afresh for one symbol, from frequencies the caller sets one by one: the
 * cumulative sums the range coder needs, and the symbol a target falls in. A frequency of 0 makes a
 * symbol impossible. One table is refilled at every step of a segment, so it keeps its arrays and
 * overwrites them in place, and it is not cleared: the caller sets every symbol. It is not shared
 * between threads.
 */
public final class CumulativeTable {

    private int[] frequencies = new int[0];
    private int[] cumulative = new int[1];
    private int size;

    /** Starts a distribution over {@code symbols} symbols, whose frequencies the caller sets, all of them. */
    public void begin(int symbols) {
        if (this.frequencies.length < symbols) {
            this.frequencies = new int[symbols];
            this.cumulative = new int[symbols + 1];
        }
        this.size = symbols;
    }

    public void set(int symbol, int frequency) {
        this.frequencies[symbol] = frequency;
    }

    /**
     * Ends the distribution. The frequencies are halved, never to 0, until their total fits what the
     * range coder codes against.
     *
     * @return whether some symbol is possible
     */
    public boolean seal() {
        long total = this.sum();
        while (total > RangeEncoder.MAX_TOTAL) {
            for (int i = 0; i < this.size; i++) {
                this.frequencies[i] = (this.frequencies[i] + 1) >>> 1;
            }
            total = this.sum();
        }
        return total > 0;
    }

    public int total() {
        return this.cumulative[this.size];
    }

    public int frequency(int symbol) {
        return this.frequencies[symbol];
    }

    /** The frequencies of all the symbols before {@code symbol}. */
    public int cumulative(int symbol) {
        return this.cumulative[symbol];
    }

    /** The symbol whose range contains {@code target}, which is below {@link #total()}. */
    public int symbolAt(int target) {
        int low = 0;
        int high = this.size - 1;
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

    private long sum() {
        long total = 0;
        for (int i = 0; i < this.size; i++) {
            this.cumulative[i] = (int) Math.min(total, Integer.MAX_VALUE);
            total += this.frequencies[i];
        }
        this.cumulative[this.size] = (int) Math.min(total, Integer.MAX_VALUE);
        return total;
    }
}
