package cz.cvut.fit.acb.dictionary;

/**
 * How much a candidate of a funnel counts, from how far its context agrees with the current one. A
 * constant is one rule; a weight of 0 means the candidate is not admitted, and a side of a funnel ends
 * at its first such candidate.
 */
public enum Weighting {

    /**
     * For coding which candidate continues the text: grows with the agreement in bytes, with a bonus for
     * the bits of the byte that broke it, and falls with the rank distance from the context, so the
     * nearest contexts decide when the agreement is alike.
     */
    POSITION {
        @Override
        int of(int agreement, int threshold, int distance) {
            int bytes = agreement >> 3;
            boolean admitted = agreement - threshold >= 2;
            return admitted ? (bytes + NEARNESS[Math.min(distance, PROXIMITY + 1)] + ((agreement + 1) & 7))
                    * floorLog2(1 + bytes) : 0;
        }
    },

    /** For voting on the next byte: the agreement in bits times its logarithm, whatever the distance. */
    FORECAST {
        @Override
        int of(int agreement, int threshold, int distance) {
            return agreement - threshold >= 4 ? agreement * floorLog2(agreement) : 0;
        }
    };

    /** What the nearest neighbour adds to its weight; a neighbour {@code d} ranks away adds this over {@code d}. */
    private static final int PROXIMITY = 1024;

    /** {@code PROXIMITY / distance}, for the distances that add anything, which is worth a table over a division. */
    private static final int[] NEARNESS = nearness();

    /**
     * @param agreement the bits the contexts agree on
     * @param threshold the bits any agreement must exceed to be more than chance
     * @param distance how many ranks the candidate is from the context, at least 1
     */
    abstract int of(int agreement, int threshold, int distance);

    private static int[] nearness() {
        int[] table = new int[PROXIMITY + 2];
        for (int distance = 1; distance < table.length; distance++) {
            table[distance] = PROXIMITY / distance;
        }
        return table;
    }

    /** The largest {@code k} with {@code 2^k <= value}; 0 for values below 2. */
    static int floorLog2(int value) {
        return value < 2 ? 0 : Integer.SIZE - 1 - Integer.numberOfLeadingZeros(value);
    }
}
