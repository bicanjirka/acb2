package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.dictionary.Funnel;

/**
 * Which candidate of a funnel continues the text, or none: each candidate is as likely as it weighs,
 * and the escape weighs as much as the recent steps that found no match make it. How often the escape
 * was needed is counted apart for funnels whose best candidate weighs alike, and the counts are
 * halved when they grow, so the model follows the recent past.
 */
final class PositionModel {

    private static final int BUCKETS = 16;
    private static final int HALVE_AT = 256;
    /** The weights of a funnel are scaled to total at most this many bits, which the range coder codes exactly. */
    private static final int TABLE_BITS = 19;

    private final int[] escapes = new int[BUCKETS];
    private final int[] steps = new int[BUCKETS];

    /** Makes the distribution: symbol 0 is the escape, symbol {@code i + 1} candidate {@code i}. */
    void fill(Funnel funnel, CumulativeTable table) {
        int size = funnel.size();
        int shift = Math.max(0, Long.SIZE - Long.numberOfLeadingZeros(funnel.totalWeight()) - TABLE_BITS);
        table.begin(size + 1);
        long scaled = 0;
        for (int i = 0; i < size; i++) {
            int weight = Math.max(1, funnel.weight(i) >> shift);
            table.set(i + 1, weight);
            scaled += weight;
        }
        int bucket = bucket(funnel);
        long odds = scaled * (this.escapes[bucket] + 1) / (this.steps[bucket] - this.escapes[bucket] + 1);
        table.set(0, (int) Math.max(1, Math.min(odds, 1 << TABLE_BITS)));
        table.seal();
    }

    /** Learns whether the step found a match in the funnel. */
    void record(Funnel funnel, boolean escaped) {
        int bucket = bucket(funnel);
        this.steps[bucket]++;
        if (escaped) {
            this.escapes[bucket]++;
        }
        if (this.steps[bucket] >= HALVE_AT) {
            this.steps[bucket] >>= 1;
            this.escapes[bucket] >>= 1;
        }
    }

    private static int bucket(Funnel funnel) {
        return Math.min(BUCKETS - 1, Integer.SIZE - 1 - Integer.numberOfLeadingZeros(Math.max(1, funnel.weight(0))));
    }
}
