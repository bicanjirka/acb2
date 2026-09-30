package cz.cvut.fit.acb.coding;

/**
 * Symbol frequencies that grow as symbols are seen and are halved when their total would pass a
 * limit, so they follow the recent past; what {@link AdaptiveFrequencyModel} counts, without the
 * cumulative frequencies, for a model that sets a distribution from the frequencies itself. The
 * increment and the limit are part of the stream format.
 */
public final class FrequencyCounts {

    /** What one occurrence adds; the starting frequencies of a field are in the same unit. */
    static final int INCREMENT = 32;
    static final int MIN_LIMIT = 1 << 16;
    /** A wide alphabet keeps history for longer: this many increments per symbol before the halving. */
    static final int SYMBOL_ALLOWANCE = 256;

    private final int[] frequencies;
    private final int limit;
    private int total;

    /** @param initial the starting frequency of every symbol, each at least 1 */
    public FrequencyCounts(int[] initial) {
        this.frequencies = initial.clone();
        this.limit = limitFor(initial.length);
        for (int frequency : this.frequencies) {
            this.total += frequency;
        }
        while (this.total > this.limit) {
            this.halve();
        }
    }

    /** The most the frequencies of {@code symbols} symbols may total. */
    public static int limitFor(int symbols) {
        return Math.min(RangeEncoder.MAX_TOTAL, Math.max(MIN_LIMIT, SYMBOL_ALLOWANCE * symbols));
    }

    public int total() {
        return this.total;
    }

    public int frequency(int symbol) {
        return this.frequencies[symbol];
    }

    /**
     * Counts one more {@code symbol}.
     *
     * @return whether every frequency was halved first
     */
    public boolean increment(int symbol) {
        boolean halved = this.total + INCREMENT > this.limit;
        if (halved) {
            this.halve();
        }
        this.frequencies[symbol] += INCREMENT;
        this.total += INCREMENT;
        return halved;
    }

    /** The frequencies themselves, for a tree of their sums to be rebuilt from; not to be changed. */
    int[] frequencies() {
        return this.frequencies;
    }

    private void halve() {
        this.total = 0;
        for (int i = 0; i < this.frequencies.length; i++) {
            this.frequencies[i] = (this.frequencies[i] + 1) >>> 1;
            this.total += this.frequencies[i];
        }
    }
}
