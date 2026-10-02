package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.coding.FrequencyCounts;

/**
 * The excess of a match over what the decoder knows of it, as the recent matches had it, and the
 * lengths that candidates after the chosen one share with it, which are likelier than the rest: a
 * text that leaves the chosen content where such a candidate does has the length of their common
 * prefix.
 */
final class LengthModel {

    /** Every candidate that shares a length adds this share of the model's total, more for the first ones. */
    private static final int BOOST_DIVISOR = 10;

    private final FrequencyCounts frequencies;
    private final int[] boosts;
    /** The excesses shared, in a slot more than there are excesses, which a share writes to whether it is new or not. */
    private final int[] touched;
    private int touchedCount;

    LengthModel(int[] startingTable) {
        this.frequencies = new FrequencyCounts(startingTable);
        this.boosts = new int[startingTable.length];
        this.touched = new int[startingTable.length + 1];
    }

    /** Notes a candidate after the chosen one that shares exactly {@code excess + floor} bytes with it. */
    void share(int excess) {
        int before = this.boosts[excess];
        this.touched[this.touchedCount] = excess;
        this.touchedCount += before == 0 ? 1 : 0;
        this.boosts[excess] = before + 1;
    }

    /** Makes the distribution of the excesses {@code 0 .. allowed - 1}, and forgets the shared lengths. */
    void fill(CumulativeTable table, int allowed) {
        table.begin(this.frequencies, allowed);
        long total = this.frequencies.total();
        for (int i = 0; i < this.touchedCount; i++) {
            int excess = this.touched[i];
            if (excess < allowed) {
                int shared = this.boosts[excess] - (this.boosts[excess] >> 2);
                int weight = shared + Integer.SIZE - 1 - Integer.numberOfLeadingZeros(shared + 2);
                table.set(excess, (int) Math.min(Integer.MAX_VALUE, table.frequency(excess)
                        + total * weight / BOOST_DIVISOR));
            }
            this.boosts[excess] = 0;
        }
        this.touchedCount = 0;
        table.seal();
    }

    void update(int excess) {
        this.frequencies.increment(excess);
    }
}
