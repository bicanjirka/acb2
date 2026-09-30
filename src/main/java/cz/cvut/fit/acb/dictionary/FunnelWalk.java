package cz.cvut.fit.acb.dictionary;

/**
 * Fills a {@link Funnel}: from the place a context would take in the dictionary it walks outward on
 * both sides, always to the side whose next candidate weighs more, over the entries whose contexts
 * agree with it beyond chance. A side ends at its first entry that is not admitted or after
 * {@code reach} entries. The entries are ordered by context, so the agreement only falls outward, and
 * how far an entry agrees follows from how far the one before it does and how much the two have in
 * common, which the index keeps, and how its agreement ends follows from the first bytes of its context,
 * which the index keeps too: the text is read only for the two entries next to the context, and for an
 * agreement of more than eight bytes. The walk reads only bytes before the position, so both sides of a
 * stream walk alike.
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
    private final Side above = new Side(true);
    private final Side below = new Side(false);
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
        this.above.start(this.surroundings.abovePositions(), this.surroundings.abovePrefixes(),
                this.surroundings.aboveShared(), this.surroundings.aboveCount());
        this.below.start(this.surroundings.belowPositions(), this.surroundings.belowPrefixes(),
                this.surroundings.belowShared(), this.surroundings.belowCount());
        while (true) {
            Side next = this.above.weight >= this.below.weight ? this.above : this.below;
            if (next.weight == 0) {
                return;
            }
            funnel.add(next.positions[next.at], next.weight);
            next.advance();
        }
    }

    /** One direction of the walk: where it is, how far it has gone and what its next candidate weighs. */
    private final class Side {

        private final boolean ascending;
        private int[] positions;
        private long[] prefixes;
        private int[] shared;
        private int count;
        private int at;
        private int agreedBytes;
        private int weight;

        private Side(boolean ascending) {
            this.ascending = ascending;
        }

        /** The entry next to the context is compared with it, the only one that is. */
        private void start(int[] positions, long[] prefixes, int[] shared, int count) {
            this.positions = positions;
            this.prefixes = prefixes;
            this.shared = shared;
            this.count = count;
            this.at = 0;
            if (count == 0) {
                this.weight = 0;
                return;
            }
            int content = positions[0];
            int limit = this.limitFor(content);
            long difference = FunnelWalk.this.contextPrefix ^ prefixes[0];
            this.agreedBytes = difference != 0 ? Math.min(limit, Long.numberOfLeadingZeros(difference) >>> 3)
                    : limit <= Long.BYTES ? limit : ContextAgreement.commonBytesFrom(FunnelWalk.this.bytes,
                    FunnelWalk.this.position, content, Long.BYTES, limit);
            this.weight = this.weigh(content, prefixes[0]);
        }

        /** Moves to the next entry out; going down, what it shares with the one before it is kept with the one left. */
        private void advance() {
            if (this.at + 1 >= this.count) {
                this.weight = 0;
                return;
            }
            int sharedWithNext = this.ascending ? this.shared[this.at + 1] : this.shared[this.at];
            this.at++;
            this.agreedBytes = Math.min(this.agreedBytes, sharedWithNext);
            this.weight = this.weigh(this.positions[this.at], this.prefixes[this.at]);
        }

        /** The weight of the entry, whose agreement is counted in bits from the first bytes of its context where they reach. */
        private int weigh(int content, long prefix) {
            int limit = this.limitFor(content);
            int agreed = Math.min(this.agreedBytes, limit);
            int agreement;
            if (agreed >= limit) {
                agreement = limit << 3;
            } else if (agreed < Long.BYTES) {
                int shift = Long.SIZE - Byte.SIZE * (agreed + 1);
                agreement = (agreed << 3) + Long.numberOfLeadingZeros(((prefix ^ FunnelWalk.this.contextPrefix) >>> shift)
                        & 0xFF) - (Long.SIZE - Byte.SIZE);
            } else {
                agreement = ContextAgreement.bitsAfter(FunnelWalk.this.bytes, FunnelWalk.this.position, content,
                        agreed, limit);
            }
            return FunnelWalk.this.weighting.of(agreement, FunnelWalk.this.threshold, this.at + 1);
        }

        private int limitFor(int content) {
            return Math.min(FunnelWalk.this.agreementBytes, Math.min(FunnelWalk.this.position, content));
        }
    }
}
