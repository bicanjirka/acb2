package cz.cvut.fit.acb.dictionary;

/**
 * Fills a {@link Funnel}: from the place a context would take in the dictionary it walks outward on
 * both sides over the entries whose contexts agree with it beyond chance, and merges the two sides,
 * always taking the one whose next candidate weighs more. A side ends at its first entry that is not
 * admitted or after {@code reach} entries. The entries are ordered by context, so the agreement only
 * falls outward, and how far an entry agrees, to the bit, is the least of how far the one before it does
 * and how much the two have in common, which the index keeps: no text is read but for the two entries next
 * to the context, which the index compares. The weights of a side are found first, in a loop of their own,
 * and the merge follows, which measured faster than deciding between the sides entry by entry (a merge
 * that chose a side without a branch measured no faster). The walk reads only bytes before the position, so
 * both sides of a stream walk alike.
 */
final class FunnelWalk {

    /** A context must agree on more than {@code log2(COEFFICIENT * entries / DIVISOR)} bits to be more than chance. */
    private static final int COEFFICIENT = 17;
    private static final int DIVISOR = 500;
    /** Below this many bytes of context nothing is chosen from. */
    private static final int MIN_CONTEXT = 4;

    private final ContextIndex index;
    private final int reach;
    private final Surroundings surroundings;
    private final Side above;
    private final Side below;

    /**
     * @param reach the most entries a side of a funnel holds
     */
    FunnelWalk(ContextIndex index, int reach) {
        this.index = index;
        this.reach = reach;
        this.surroundings = new Surroundings(reach);
        this.above = new Side(reach);
        this.below = new Side(reach);
    }

    /** Capacity a funnel needs for this walk. */
    int capacity() {
        return 2 * this.reach;
    }

    /** Fills {@code funnel} with the analogies of the context before {@code position}, in the order of their weights. */
    void fill(int position, Weighting weighting, Funnel funnel) {
        funnel.clear();
        Surroundings around = this.surroundings;
        int threshold = this.around(position, around);
        if (threshold < 0) {
            return;
        }
        Side up = this.above;
        Side down = this.below;
        up.weigh(around.abovePositions(), around.aboveShared(), 0, true, around.aboveCount(), around.aboveAgreement(),
                weighting, threshold);
        down.weigh(around.belowPositions(), around.belowShared(), around.belowShared().length - 1, false,
                around.belowCount(), around.belowAgreement(), weighting, threshold);
        int fromAbove = 0;
        int fromBelow = 0;
        for (int left = up.count + down.count; left > 0; left--) {
            int aboveWeight = up.weights[fromAbove];
            int belowWeight = down.weights[fromBelow];
            if (aboveWeight >= belowWeight) {
                funnel.add(up.positions[fromAbove], up.firstBytes[fromAbove], aboveWeight, up.agreements[fromAbove],
                        fromAbove + 1);
                fromAbove++;
            } else {
                funnel.add(down.positions[fromBelow], down.firstBytes[fromBelow], belowWeight,
                        down.agreements[fromBelow], -fromBelow - 1);
                fromBelow++;
            }
        }
    }

    /**
     * Lets the analogies of the context before {@code position} vote, those above it first, nearest first, and
     * then those below: what the votes add up to needs no merge by weight and no funnel.
     */
    void vote(int position, Weighting weighting, Ballot ballot) {
        Surroundings around = this.surroundings;
        int threshold = this.around(position, around);
        if (threshold < 0) {
            return;
        }
        vote(around.aboveShared(), 0, true, around.aboveCount(), around.aboveAgreement(), weighting, threshold,
                ballot);
        vote(around.belowShared(), around.belowShared().length - 1, false, around.belowCount(),
                around.belowAgreement(), weighting, threshold, ballot);
    }

    /**
     * Copies the entries around the place of {@code position} into {@code around}.
     *
     * @return the bits a context must agree on to be more than chance, or -1 if there is nothing to choose from
     */
    private int around(int position, Surroundings around) {
        int size = this.index.size();
        if (size == 0 || position < MIN_CONTEXT) {
            return -1;
        }
        this.index.around(position, this.reach, around);
        return Weighting.floorLog2((int) ((long) COEFFICIENT * size / DIVISOR));
    }

    /** As {@link Side#weigh}, voting with each entry that is admitted instead of keeping it. */
    private static void vote(int[] shared, int nearest, boolean ascending, int count, int agreement,
                             Weighting weighting, int threshold, Ballot ballot) {
        int bits = agreement;
        for (int taken = 0; taken < count; taken++) {
            int at = ascending ? nearest + taken : nearest - taken;
            if (taken > 0) {
                bits = Math.min(bits, Surroundings.sharedBits(shared[ascending ? at : at + 1]));
            }
            int weight = weighting.of(bits, threshold, taken + 1);
            if (weight == 0) {
                return;
            }
            ballot.vote(Surroundings.followingByte(shared[at]), weight);
        }
    }

    /** The admitted entries of one side of a walk, nearest first, and past the last of them a weight of 0. */
    private static final class Side {

        private final int[] positions;
        private final int[] firstBytes;
        private final int[] weights;
        private final int[] agreements;
        private int count;

        private Side(int reach) {
            this.positions = new int[reach + 1];
            this.firstBytes = new int[reach + 1];
            this.weights = new int[reach + 1];
            this.agreements = new int[reach + 1];
        }

        /**
         * Keeps the entries of one side, nearest first, up to the first that is not admitted. Going up, what an
         * entry shares with the one before it is kept with it; going down, with the one after.
         *
         * @param nearest where the nearest entry is in {@code positions} and {@code shared}, the next ones out
         *                following it if {@code ascending}, else coming before it
         * @param agreement how many bits the nearest entry has in common with the context
         */
        private void weigh(int[] positions, int[] shared, int nearest, boolean ascending, int count, int agreement,
                           Weighting weighting, int threshold) {
            int bits = agreement;
            int taken = 0;
            for (; taken < count; taken++) {
                int at = ascending ? nearest + taken : nearest - taken;
                if (taken > 0) {
                    bits = Math.min(bits, Surroundings.sharedBits(shared[ascending ? at : at + 1]));
                }
                int weight = weighting.of(bits, threshold, taken + 1);
                if (weight == 0) {
                    break;
                }
                this.positions[taken] = positions[at];
                this.firstBytes[taken] = Surroundings.followingByte(shared[at]);
                this.weights[taken] = weight;
                this.agreements[taken] = bits;
            }
            this.count = taken;
            this.weights[taken] = 0;
        }
    }
}
