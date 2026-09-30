package cz.cvut.fit.acb.dictionary;

/**
 * Fills a {@link Funnel}: from the place a context would take in the dictionary it walks outward on
 * both sides over the entries whose contexts agree with it beyond chance, and merges the two sides,
 * always taking the one whose next candidate weighs more. A side ends at its first entry that is not
 * admitted or after {@code reach} entries. The entries are ordered by context, so the agreement only
 * falls outward, and how far an entry agrees follows from how far the one before it does and how much
 * the two have in common, which the index keeps, and how its agreement ends follows from the first bytes
 * of its context, which the index keeps too: the text is read only for the two entries next to the
 * context, and for an agreement of more than eight bytes. The weights of a side are found first, in a
 * loop of their own, and the merge follows, which measured faster than deciding between the sides
 * entry by entry. The walk reads only bytes before the position, so both sides of a stream walk alike.
 */
final class FunnelWalk {

    /** A context must agree on more than {@code log2(COEFFICIENT * entries / DIVISOR)} bits to be more than chance. */
    private static final int COEFFICIENT = 17;
    private static final int DIVISOR = 500;
    /** Below this many bytes of context nothing is chosen from. */
    private static final int MIN_CONTEXT = 4;

    private final ContextIndex index;
    private final SegmentBuffer segment;
    private final int reach;
    private final int agreementBytes;
    private final Surroundings surroundings;
    private final int[] aboveWeights;
    private final int[] belowWeights;
    private byte[] bytes;
    private int position;
    private long contextPrefix;
    private int threshold;
    private Weighting weighting;

    /**
     * @param reach the most entries a side of a funnel holds
     * @param agreementBytes how many bytes of context are compared; the index orders by as many
     */
    FunnelWalk(ContextIndex index, SegmentBuffer segment, int reach, int agreementBytes) {
        this.index = index;
        this.segment = segment;
        this.reach = reach;
        this.agreementBytes = agreementBytes;
        this.surroundings = new Surroundings(reach);
        this.aboveWeights = new int[reach];
        this.belowWeights = new int[reach];
    }

    /** Capacity a funnel needs for this walk. */
    int capacity() {
        return 2 * this.reach;
    }

    /** Fills {@code funnel} with the analogies of the context before {@code position}. */
    void fill(int position, Weighting weighting, Funnel funnel) {
        funnel.clear();
        int size = this.index.size();
        if (size == 0 || position < MIN_CONTEXT) {
            return;
        }
        this.bytes = this.segment.bytes();
        this.position = position;
        this.contextPrefix = ContextAgreement.nearestBytes(this.bytes, position,
                Math.min(Math.min(position, this.agreementBytes), Long.BYTES));
        this.weighting = weighting;
        this.threshold = Weighting.floorLog2((int) ((long) COEFFICIENT * size / DIVISOR));
        this.index.around(position, this.reach, this.surroundings);
        int[] abovePositions = this.surroundings.abovePositions();
        int[] belowPositions = this.surroundings.belowPositions();
        int aboveCount = this.weigh(abovePositions, this.surroundings.abovePrefixes(),
                this.surroundings.aboveShared(), this.surroundings.aboveCount(), true, this.aboveWeights);
        int belowCount = this.weigh(belowPositions, this.surroundings.belowPrefixes(),
                this.surroundings.belowShared(), this.surroundings.belowCount(), false, this.belowWeights);
        int nearestBelow = belowPositions.length - 1;
        int above = 0;
        int below = 0;
        while (above < aboveCount || below < belowCount) {
            int aboveWeight = above < aboveCount ? this.aboveWeights[above] : 0;
            int belowWeight = below < belowCount ? this.belowWeights[below] : 0;
            if (aboveWeight >= belowWeight) {
                funnel.add(abovePositions[above++], aboveWeight);
            } else {
                funnel.add(belowPositions[nearestBelow - below++], belowWeight);
            }
        }
    }

    /**
     * The weights of the entries of one side, nearest first, up to the first that is not admitted. Going
     * up, what an entry shares with the one before it is kept with it; going down, with the one after.
     *
     * @return how many entries are admitted
     */
    private int weigh(int[] positions, long[] prefixes, byte[] shared, int count, boolean ascending,
                      int[] weights) {
        if (count == 0) {
            return 0;
        }
        int step = ascending ? 1 : -1;
        int at = ascending ? 0 : positions.length - 1;
        byte[] bytes = this.bytes;
        int position = this.position;
        long contextPrefix = this.contextPrefix;
        int maxBytes = this.agreementBytes;
        int threshold = this.threshold;
        Weighting weighting = this.weighting;
        int content = positions[at];
        int limit = Math.min(maxBytes, Math.min(position, content));
        long difference = contextPrefix ^ prefixes[at];
        int agreed = difference != 0 ? Math.min(limit, Long.numberOfLeadingZeros(difference) >>> 3)
                : limit <= Long.BYTES ? limit
                : ContextAgreement.commonBytesFrom(bytes, position, content, Long.BYTES, limit);
        int admitted = 0;
        for (int taken = 1; ; taken++) {
            int agreement;
            if (agreed >= limit) {
                agreement = limit << 3;
            } else if (agreed < Long.BYTES) {
                int shift = Long.SIZE - Byte.SIZE * (agreed + 1);
                agreement = (agreed << 3) + Long.numberOfLeadingZeros(((prefixes[at] ^ contextPrefix) >>> shift) & 0xFF)
                        - (Long.SIZE - Byte.SIZE);
            } else {
                agreement = ContextAgreement.bitsAfter(bytes, position, content, agreed, limit);
            }
            int weight = weighting.of(agreement, threshold, taken);
            if (weight == 0) {
                return admitted;
            }
            weights[admitted++] = weight;
            if (taken >= count) {
                return admitted;
            }
            int sharedWithNext = (ascending ? shared[at + 1] : shared[at]) & 0xFF;
            at += step;
            agreed = Math.min(agreed, sharedWithNext);
            content = positions[at];
            limit = Math.min(maxBytes, Math.min(position, content));
            if (agreed > limit) {
                agreed = limit;
            }
        }
    }
}
