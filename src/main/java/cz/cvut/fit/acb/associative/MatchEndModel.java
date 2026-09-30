package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.mixing.BitCounters;
import cz.cvut.fit.acb.mixing.Mixer;
import cz.cvut.fit.acb.mixing.ProbabilityMap;

/**
 * Whether the text leaves every live candidate of a step at the next byte, so that the match ends there;
 * before the first byte this is the escape. The chance is learnt from what followed in like situations:
 * how deep into the match the step is, how many candidates are left, whether they agree on the next byte
 * and whether a repeat is among them, how far the context of the heaviest agrees, how far back the nearest
 * lies, and the one and two bytes before, alone and with the byte the candidates predict. The situations
 * are mixed and the mixture refined by the depth and by the byte before.
 */
final class MatchEndModel {

    private static final int BIAS = 256;
    private static final int RATE = 16;
    private static final int COUNTER_LIMIT = 255;
    private static final int REFINEMENT_RATE = 6;
    private static final int STATES = 2 * ContextBuckets.COUNTS * 3;

    private final BitCounters byCandidates = new BitCounters(ContextBuckets.DEPTHS * STATES, COUNTER_LIMIT);
    private final BitCounters byAgreement = new BitCounters(ContextBuckets.DEPTHS * ContextBuckets.AGREEMENTS,
            COUNTER_LIMIT);
    private final BitCounters byPrevious = new BitCounters(256 * 8, COUNTER_LIMIT);
    private final BitCounters byPrediction = new BitCounters(1 << 16, COUNTER_LIMIT);
    private final BitCounters byHistory;
    private final BitCounters byDistance = new BitCounters(ContextBuckets.DEPTHS * ContextBuckets.DISTANCES * 2,
            COUNTER_LIMIT);
    private final Mixer mixer = new Mixer(7, RATE, 1 << 14, ContextBuckets.DEPTHS, STATES);
    private final ProbabilityMap byDepth = new ProbabilityMap(ContextBuckets.DEPTHS * 2, REFINEMENT_RATE);
    private final ProbabilityMap afterPrevious = new ProbabilityMap(256 * 2 * 4, REFINEMENT_RATE);
    private final int historyShift;
    private int candidates;
    private int agreement;
    private int previousContext;
    private int prediction;
    private int history;
    private int distance;

    /** @param historyBits how many bits the table of the two bytes before has */
    MatchEndModel(int historyBits) {
        this.byHistory = new BitCounters(1 << historyBits, COUNTER_LIMIT);
        this.historyShift = Integer.SIZE - historyBits;
    }

    /**
     * @param depth how many bytes of the step the text has gone on with the candidates
     * @param history the two bytes before the next one, the nearer in the low byte
     * @return the 12-bit probability that the match ends here
     */
    int probability(int depth, Continuations next, int history) {
        int depthBucket = ContextBuckets.depth(depth);
        int unanimous = next.distinct() == 1 ? 1 : 0;
        int previous = history & 0xFF;
        int repeat = next.anyRepeat() ? next.repeated(0) ? 1 : 2 : 0;
        int state = (unanimous * ContextBuckets.COUNTS + ContextBuckets.count(next.liveCount())) * 3 + repeat;
        this.candidates = depthBucket * STATES + state;
        this.agreement = depthBucket * ContextBuckets.AGREEMENTS + ContextBuckets.agreement(next.agreement(0));
        this.previousContext = previous * 8 + Math.min(7, depthBucket);
        this.prediction = previous << 8 | next.byteAt(0);
        this.history = ((history + 1) * 0x2F0B4F27 + Math.min(3, depth) * 0x9E3779B1) >>> this.historyShift;
        this.distance = (depthBucket * ContextBuckets.DISTANCES + ContextBuckets.distance(next.nearest(0))) * 2
                + unanimous;
        this.mixer.addProbability(this.byCandidates.probability(this.candidates));
        this.mixer.addProbability(this.byAgreement.probability(this.agreement));
        this.mixer.addProbability(this.byPrevious.probability(this.previousContext));
        this.mixer.addProbability(this.byPrediction.probability(this.prediction));
        this.mixer.addProbability(this.byHistory.probability(this.history));
        this.mixer.addProbability(this.byDistance.probability(this.distance));
        this.mixer.add(BIAS);
        this.mixer.select(0, depthBucket);
        this.mixer.select(1, state);
        int mixed = this.mixer.mix();
        int refined = this.byDepth.refine(mixed, depthBucket * 2 + unanimous);
        int followed = this.afterPrevious.refine(mixed, (previous * 2 + unanimous) * 4 + Math.min(3, depth));
        return (2 * mixed + 3 * refined + 3 * followed + 4) >> 3;
    }

    /** Learns whether the match ended where {@link #probability} was last asked. */
    void update(int ended) {
        this.mixer.update(ended);
        this.byDepth.update(ended);
        this.afterPrevious.update(ended);
        this.byCandidates.update(this.candidates, ended);
        this.byAgreement.update(this.agreement, ended);
        this.byPrevious.update(this.previousContext, ended);
        this.byPrediction.update(this.prediction, ended);
        this.byHistory.update(this.history, ended);
        this.byDistance.update(this.distance, ended);
    }
}
