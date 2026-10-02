package cz.cvut.fit.acb.dictionary;

/**
 * Fills a {@link Funnel}: from the place a context would take in the dictionary it walks outward on
 * both sides over the entries whose contexts agree with it beyond chance, and merges the two sides,
 * always taking the one whose next candidate weighs more. A side ends at its first entry that is not
 * admitted or after {@code reach} entries. The entries are ordered by context, so the agreement only
 * falls outward, and how far an entry agrees, to the bit, is the least of how far the one before it does
 * and how much the two have in common, which the index keeps: no text is read but for the two entries next
 * to the context, which the index compares. The weights of a side are found first, in a loop of their own,
 * and the merge follows, which measured faster than deciding between the sides entry by entry. The walk
 * reads only bytes before the position, so both sides of a stream walk alike.
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
    private final int[] aboveWeights;
    private final int[] belowWeights;
    private final int[] aboveAgreements;
    private final int[] belowAgreements;

    /**
     * @param reach the most entries a side of a funnel holds
     */
    FunnelWalk(ContextIndex index, int reach) {
        this.index = index;
        this.reach = reach;
        this.surroundings = new Surroundings(reach);
        this.aboveWeights = new int[reach];
        this.belowWeights = new int[reach];
        this.aboveAgreements = new int[reach];
        this.belowAgreements = new int[reach];
    }

    /** Capacity a funnel needs for this walk. */
    int capacity() {
        return 2 * this.reach;
    }

    /**
     * Fills {@code funnel} with the analogies of the context before {@code position}, in the order of their
     * weights if {@code ordered}, or else above the context and then below it.
     */
    void fill(int position, Weighting weighting, boolean ordered, Funnel funnel) {
        funnel.clear();
        int size = this.index.size();
        if (size == 0 || position < MIN_CONTEXT) {
            return;
        }
        int threshold = Weighting.floorLog2((int) ((long) COEFFICIENT * size / DIVISOR));
        Surroundings around = this.surroundings;
        this.index.around(position, this.reach, around);
        int[] abovePositions = around.abovePositions();
        int[] belowPositions = around.belowPositions();
        int[] aboveShared = around.aboveShared();
        int[] belowShared = around.belowShared();
        int aboveCount = weigh(aboveShared, 0, true, around.aboveCount(), around.aboveAgreement(), weighting,
                threshold, this.aboveWeights, this.aboveAgreements);
        int nearestBelow = belowPositions.length - 1;
        int belowCount = weigh(belowShared, nearestBelow, false, around.belowCount(), around.belowAgreement(),
                weighting, threshold, this.belowWeights, this.belowAgreements);
        if (!ordered) {
            for (int above = 0; above < aboveCount; above++) {
                funnel.add(abovePositions[above], Surroundings.followingByte(aboveShared[above]),
                        this.aboveWeights[above], this.aboveAgreements[above], above + 1);
            }
            for (int below = 0; below < belowCount; below++) {
                funnel.add(belowPositions[nearestBelow - below],
                        Surroundings.followingByte(belowShared[nearestBelow - below]), this.belowWeights[below],
                        this.belowAgreements[below], -below - 1);
            }
            return;
        }
        int above = 0;
        int below = 0;
        while (above < aboveCount || below < belowCount) {
            int aboveWeight = above < aboveCount ? this.aboveWeights[above] : 0;
            int belowWeight = below < belowCount ? this.belowWeights[below] : 0;
            if (aboveWeight >= belowWeight) {
                funnel.add(abovePositions[above], Surroundings.followingByte(aboveShared[above]), aboveWeight,
                        this.aboveAgreements[above], above + 1);
                above++;
            } else {
                funnel.add(belowPositions[nearestBelow - below],
                        Surroundings.followingByte(belowShared[nearestBelow - below]), belowWeight,
                        this.belowAgreements[below], -below - 1);
                below++;
            }
        }
    }

    /**
     * The weights of the entries of one side, nearest first, up to the first that is not admitted. Going
     * up, what an entry shares with the one before it is kept with it; going down, with the one after.
     *
     * @param nearest where the nearest entry is in {@code shared}, the next ones out following it if
     *                {@code ascending}, else coming before it
     * @param agreement how many bits the nearest entry has in common with the context
     * @return how many entries are admitted
     */
    private static int weigh(int[] shared, int nearest, boolean ascending, int count, int agreement,
                             Weighting weighting, int threshold, int[] weights, int[] agreements) {
        int bits = agreement;
        for (int taken = 0; taken < count; taken++) {
            if (taken > 0) {
                int between = ascending ? nearest + taken : nearest - taken + 1;
                bits = Math.min(bits, Surroundings.sharedBits(shared[between]));
            }
            int weight = weighting.of(bits, threshold, taken + 1);
            if (weight == 0) {
                return taken;
            }
            agreements[taken] = bits;
            weights[taken] = weight;
        }
        return count;
    }
}
