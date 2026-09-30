package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.AdaptiveFrequencyModel;
import cz.cvut.fit.acb.coding.CumulativeTable;

import java.util.Arrays;

/**
 * The literals of a segment: how often each byte came, minus the bytes a literal cannot be, plus, when
 * the funnel of the context before it is asked, the bytes that funnel votes for. A vote adds to a byte
 * a share of the model's total as big as its share of all the votes, more when the votes are heavy.
 * The exclusions and the votes of a literal are set first, then it is coded, then {@link #update}
 * counts it. The arrays are overwritten in place, and a literal's entries are told from the last one's
 * by a stamp, so that starting a literal costs nothing.
 */
final class ForecastedLiterals {

    private static final int SYMBOLS = 256;
    /** A byte whose votes weigh at least this much counts double in the mixture, and more for each doubling. */
    private static final int HEAVY_VOTES = 2048;

    private final AdaptiveFrequencyModel frequencies = flat();
    private final int[] excludedIn = new int[SYMBOLS];
    private final int[] votedIn = new int[SYMBOLS];
    private final int[] votes = new int[SYMBOLS];
    private final int[] votedList = new int[SYMBOLS];
    private int literal = 1;
    private int votedCount;
    private long voteTotal;

    /** Forgets the exclusions and votes of the last literal. */
    void reset() {
        this.literal++;
        this.votedCount = 0;
        this.voteTotal = 0;
    }

    /** The literal cannot be {@code symbol}. */
    void exclude(int symbol) {
        this.excludedIn[symbol] = this.literal;
    }

    /** A funnel candidate of weight {@code weight} says the literal is {@code symbol}; a byte left out gets no vote. */
    void vote(int symbol, int weight) {
        if (this.excludedIn[symbol] != this.literal) {
            if (this.votedIn[symbol] != this.literal) {
                this.votedIn[symbol] = this.literal;
                this.votes[symbol] = 0;
                this.votedList[this.votedCount++] = symbol;
            }
            this.votes[symbol] += weight;
            this.voteTotal += weight;
        }
    }

    /** @return whether some byte is possible */
    boolean fill(CumulativeTable table) {
        table.begin(SYMBOLS);
        long total = 0;
        for (int symbol = 0; symbol < SYMBOLS; symbol++) {
            int frequency = this.excludedIn[symbol] != this.literal ? this.frequencies.frequency(symbol) : 0;
            table.set(symbol, frequency);
            total += frequency;
        }
        for (int i = 0; i < this.votedCount; i++) {
            int symbol = this.votedList[i];
            int heavy = this.votes[symbol] / HEAVY_VOTES;
            int doublings = heavy < 2 ? 0 : Integer.SIZE - 1 - Integer.numberOfLeadingZeros(heavy);
            long share = (doublings + 1L) * total * this.votes[symbol] / this.voteTotal;
            table.set(symbol, (int) Math.min(Integer.MAX_VALUE, table.frequency(symbol) + share));
        }
        return table.seal();
    }

    void update(int symbol) {
        this.frequencies.increment(symbol);
    }

    private static AdaptiveFrequencyModel flat() {
        int[] initial = new int[SYMBOLS];
        Arrays.fill(initial, 1);
        return new AdaptiveFrequencyModel(initial);
    }
}
