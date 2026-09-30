package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.mixing.BitCounters;
import cz.cvut.fit.acb.mixing.Mixer;
import cz.cvut.fit.acb.mixing.ProbabilityMap;

/**
 * Which of the bytes the live candidates go on with the text takes, asked of each byte in turn from the
 * heaviest: is it this one? The funnel's answer is the byte's share of the weight, and of the number, of
 * the candidates not yet ruled out; it is mixed with what followed before when a byte of that rank had as
 * many candidates, a repeat among them, as far an agreement of context and as near a candidate, and when
 * the same byte came after the same one and two bytes.
 */
final class ContinuationModel {

    private static final int RANKS = 4;
    private static final int BIAS = 256;
    private static final int RATE = 16;
    private static final int COUNTER_LIMIT = 255;
    private static final int BYTE_LIMIT = 60;
    private static final int REFINEMENT_RATE = 6;
    private static final int SHARES = 16;

    private final BitCounters byCandidates = new BitCounters(
            RANKS * ContextBuckets.COUNTS * ContextBuckets.COUNTS * 2, COUNTER_LIMIT);
    private final BitCounters byPrevious = new BitCounters(1 << 16, BYTE_LIMIT);
    private final BitCounters byAgreement = new BitCounters(
            RANKS * ContextBuckets.AGREEMENTS * ContextBuckets.AGREEMENTS, COUNTER_LIMIT);
    private final BitCounters byHistory;
    private final BitCounters byDistance = new BitCounters(
            RANKS * ContextBuckets.DISTANCES * ContextBuckets.DISTANCES, COUNTER_LIMIT);
    private final Mixer mixer = new Mixer(8, RATE, 1 << 14, RANKS * 2, 8 * 2 * ContextBuckets.COUNTS);
    private final ProbabilityMap byShare = new ProbabilityMap(RANKS * SHARES, REFINEMENT_RATE);
    private final int historyShift;
    private int candidates;
    private int previous;
    private int agreement;
    private int history;
    private int distance;

    /** @param historyBits how many bits the table of the two bytes before and the byte asked about has */
    ContinuationModel(int historyBits) {
        this.byHistory = new BitCounters(1 << historyBits, BYTE_LIMIT);
        this.historyShift = Integer.SIZE - historyBits;
    }

    /**
     * @param rank the rank of the byte asked about; the heavier bytes are ruled out
     * @param remaining the weight of the bytes of this rank and after
     * @param remainingCount the candidates that go on with those bytes
     * @param depth how many bytes of the step the text has gone on with the candidates
     * @param history the two bytes before the next one, the nearer in the low byte
     * @return the 12-bit probability that the text goes on with the byte of that rank
     */
    int probability(Continuations next, int rank, long remaining, int remainingCount, int depth, int history) {
        int ranked = Math.min(RANKS - 1, rank);
        int share = ContextBuckets.share(next.weight(rank), remaining);
        int value = next.byteAt(rank);
        int repeated = next.repeated(rank) ? 1 : 0;
        int left = ContextBuckets.count(remainingCount);
        this.candidates = ((ranked * ContextBuckets.COUNTS + ContextBuckets.count(next.count(rank)))
                * ContextBuckets.COUNTS + left) * 2 + repeated;
        this.previous = (history & 0xFF) << 8 | value;
        this.agreement = (ranked * ContextBuckets.AGREEMENTS + ContextBuckets.agreement(next.agreement(rank)))
                * ContextBuckets.AGREEMENTS + ContextBuckets.agreement(next.agreement(0));
        this.history = ((history << 8 | value) + 1) * 0x2F0B4F27 >>> this.historyShift;
        this.distance = (ranked * ContextBuckets.DISTANCES + ContextBuckets.distance(next.nearest(rank)))
                * ContextBuckets.DISTANCES + ContextBuckets.distance(next.nearest(0));
        this.mixer.addProbability(share);
        this.mixer.addProbability(ContextBuckets.share(next.count(rank), remainingCount));
        this.mixer.addProbability(this.byCandidates.probability(this.candidates));
        this.mixer.addProbability(this.byPrevious.probability(this.previous));
        this.mixer.addProbability(this.byAgreement.probability(this.agreement));
        this.mixer.addProbability(this.byHistory.probability(this.history));
        this.mixer.addProbability(this.byDistance.probability(this.distance));
        this.mixer.add(BIAS);
        this.mixer.select(0, ranked * 2 + (depth == 0 ? 0 : 1));
        this.mixer.select(1, (Math.min(7, depth) * 2 + repeated) * ContextBuckets.COUNTS + left);
        int mixed = this.mixer.mix();
        int refined = this.byShare.refine(mixed, ranked * SHARES + Math.min(SHARES - 1, share >> 8));
        return (mixed + 3 * refined + 2) >> 2;
    }

    /** Learns whether the text went on with the byte {@link #probability} was last asked about. */
    void update(int taken) {
        this.mixer.update(taken);
        this.byShare.update(taken);
        this.byCandidates.update(this.candidates, taken);
        this.byPrevious.update(this.previous, taken);
        this.byAgreement.update(this.agreement, taken);
        this.byHistory.update(this.history, taken);
        this.byDistance.update(this.distance, taken);
    }
}
