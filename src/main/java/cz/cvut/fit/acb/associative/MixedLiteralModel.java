package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.coding.FrequencyCounts;
import cz.cvut.fit.acb.coding.RangeEncoder;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.mixing.BitCounters;
import cz.cvut.fit.acb.mixing.Logistic;
import cz.cvut.fit.acb.mixing.Mixer;
import cz.cvut.fit.acb.mixing.ProbabilityMap;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.util.Arrays;

/**
 * The literals of the mixed associative coder, coded a bit at a time, the highest first. Each bit mixes
 * what the funnel says of the literal, the distribution of {@link ForecastedLiterals} (the literals so far
 * and the votes of the funnel of the context, without the bytes the literal cannot be), with the literals
 * so far alone, those that came after the same byte, as a distribution without those bytes and as bits,
 * and those that came after the same two bytes. A bit that only one value can take, since every byte on
 * the other side is left out, is not coded.
 */
final class MixedLiteralModel {

    private static final int SYMBOLS = 256;
    private static final int INPUTS = 6;
    private static final int BIAS = 256;
    private static final int RATE = 24;
    private static final int COUNTER_LIMIT = 30;
    private static final int REFINEMENT_RATE = 6;
    /** Whether the literal is voted on, follows a match without votes, or follows an escape. */
    private static final int STATES = 3;

    private final ForecastedLiterals forecast;
    private final CumulativeTable table = new CumulativeTable();
    private final SegmentBuffer text;
    private final DecisionPort port;
    private final BitCounters afterByte = new BitCounters(SYMBOLS * SYMBOLS, COUNTER_LIMIT);
    private final BitCounters afterTwoBytes;
    private final int historyShift;
    private final FrequencyCounts[] following = new FrequencyCounts[SYMBOLS];
    private final Mixer mixer = new Mixer(INPUTS, RATE, 1 << 14, Byte.SIZE * STATES, SYMBOLS);
    private final ProbabilityMap refinement = new ProbabilityMap(SYMBOLS * STATES, REFINEMENT_RATE);
    private final long[] counts = new long[SYMBOLS + 1];
    private final long[] followingCounts = new long[SYMBOLS + 1];

    /**
     * @param forecast where the exclusions and votes of a literal are gathered before it is coded
     * @param historyBits how many bits the table of the literals after the same two bytes has
     */
    MixedLiteralModel(ForecastedLiterals forecast, SegmentBuffer text, DecisionPort port, int historyBits) {
        this.forecast = forecast;
        this.text = text;
        this.port = port;
        this.afterTwoBytes = new BitCounters(1 << historyBits, COUNTER_LIMIT);
        this.historyShift = Integer.SIZE - historyBits;
    }

    /**
     * Codes the literal at {@code at}, whose exclusions and votes the forecast holds; the decoder appends it.
     *
     * @throws MalformedStreamException if every byte is left out
     */
    void code(int at) throws MalformedStreamException {
        int previous = at > 0 ? Byte.toUnsignedInt(this.text.byteAt(at - 1)) : 0;
        FrequencyCounts afterPrevious = this.following[previous];
        if (afterPrevious == null) {
            afterPrevious = flat();
            this.following[previous] = afterPrevious;
        }
        if (!this.fillAllowed(afterPrevious)) {
            throw new MalformedStreamException("No byte can follow the match");
        }
        int history = (previous << 8 | (at > 1 ? Byte.toUnsignedInt(this.text.byteAt(at - 2)) : 0)) + 1;
        int state = this.forecast.voted() ? 2 : this.forecast.excludedAny() ? 1 : 0;
        int truth = this.port.textByte(at);
        int node = 1;
        int low = 0;
        for (int width = SYMBOLS >> 1; width > 0; width >>= 1) {
            int middle = low + width;
            int high = middle + width;
            int below = this.table.cumulative(middle) - this.table.cumulative(low);
            int above = this.table.cumulative(high) - this.table.cumulative(middle);
            int bit;
            if (below == 0 || above == 0) {
                bit = below == 0 ? 1 : 0;
            } else {
                int depth = Integer.numberOfTrailingZeros(SYMBOLS) - 1 - Integer.numberOfTrailingZeros(width);
                int twoBytes = (history * 0x2F0B4F27 + node * 0x9E3779B1) >>> this.historyShift;
                this.mixer.addProbability(ContextBuckets.share(above, above + below));
                this.mixer.addProbability(split(this.counts, low, middle, high));
                this.mixer.addProbability(this.afterByte.probability(previous * SYMBOLS + node));
                this.mixer.addProbability(this.afterTwoBytes.probability(twoBytes));
                this.mixer.addProbability(split(this.followingCounts, low, middle, high));
                this.mixer.add(BIAS);
                this.mixer.select(0, depth * STATES + state);
                this.mixer.select(1, previous);
                int mixed = this.mixer.mix();
                int refined = this.refinement.refine(mixed, state * SYMBOLS + node);
                int probability = (mixed + 3 * refined + 2) >> 2;
                bit = this.port.bit(TripletFieldKind.LITERAL, (truth / width) & 1,
                        probability << (RangeEncoder.PROBABILITY_BITS - Logistic.BITS));
                this.mixer.update(bit);
                this.refinement.update(bit);
                this.afterByte.update(previous * SYMBOLS + node, bit);
                this.afterTwoBytes.update(twoBytes, bit);
            }
            node = node * 2 + bit;
            if (bit != 0) {
                low = middle;
            }
        }
        int literal = node - SYMBOLS;
        this.forecast.update(literal);
        afterPrevious.increment(literal);
        this.port.appended(literal);
    }

    /**
     * Fills the forecast's distribution, and the running sums of the literal counts and of the counts after
     * the previous byte, without the bytes left out: one pass over the bytes for all three, which measured
     * 2% faster than a pass for the distribution and another for the sums.
     *
     * @return whether some byte is possible
     */
    private boolean fillAllowed(FrequencyCounts afterPrevious) {
        this.table.begin(SYMBOLS);
        long countSum = 0;
        long followingSum = 0;
        for (int symbol = 0; symbol < SYMBOLS; symbol++) {
            this.counts[symbol] = countSum;
            this.followingCounts[symbol] = followingSum;
            if (this.forecast.excluded(symbol)) {
                this.table.set(symbol, 0);
            } else {
                int frequency = this.forecast.frequency(symbol);
                this.table.set(symbol, frequency);
                countSum += frequency;
                followingSum += afterPrevious.frequency(symbol);
            }
        }
        this.counts[SYMBOLS] = countSum;
        this.followingCounts[SYMBOLS] = followingSum;
        return this.forecast.voteInto(this.table, countSum);
    }

    /** The 12-bit probability of the upper half of {@code low .. high} by running sums {@code sums}. */
    private static int split(long[] sums, int low, int middle, int high) {
        long total = sums[high] - sums[low];
        return total == 0 ? Logistic.ONE / 2 : ContextBuckets.share(sums[high] - sums[middle], total);
    }

    private static FrequencyCounts flat() {
        int[] initial = new int[SYMBOLS];
        Arrays.fill(initial, 1);
        return new FrequencyCounts(initial);
    }
}
