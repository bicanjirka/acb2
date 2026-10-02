package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.coding.FrequencyCounts;

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

    private final FrequencyCounts frequencies = flat();
    private final int[] excludedIn = new int[SYMBOLS];
    private final int[] excludedList = new int[SYMBOLS];
    private final int[] votedIn = new int[SYMBOLS];
    private final int[] votes = new int[SYMBOLS];
    private final int[] votedList = new int[SYMBOLS];
    private int literal = 1;
    private int excludedCount;
    private int votedCount;
    private long voteTotal;

    /** Forgets the exclusions and votes of the last literal. */
    void reset() {
        this.literal++;
        this.excludedCount = 0;
        this.votedCount = 0;
        this.voteTotal = 0;
    }

    /** The literal cannot be {@code symbol}. */
    void exclude(int symbol) {
        if (this.excludedIn[symbol] != this.literal) {
            this.excludedIn[symbol] = this.literal;
            this.excludedList[this.excludedCount++] = symbol;
        }
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
        table.begin(this.frequencies, SYMBOLS);
        long total = this.frequencies.total();
        for (int i = 0; i < this.excludedCount; i++) {
            int symbol = this.excludedList[i];
            total -= this.frequencies.frequency(symbol);
            table.set(symbol, 0);
        }
        return this.voteInto(table, total);
    }

    /**
     * Adds the votes to a table in which every byte already has its frequency, 0 for a byte left out, and
     * ends it; {@link #fill} is this after setting those frequencies.
     *
     * @param total the frequencies set, summed
     * @return whether some byte is possible
     */
    boolean voteInto(CumulativeTable table, long total) {
        for (int i = 0; i < this.votedCount; i++) {
            int symbol = this.votedList[i];
            int heavy = this.votes[symbol] / HEAVY_VOTES;
            int doublings = heavy < 2 ? 0 : Integer.SIZE - 1 - Integer.numberOfLeadingZeros(heavy);
            long share = (doublings + 1L) * total * this.votes[symbol] / this.voteTotal;
            table.set(symbol, (int) Math.min(Integer.MAX_VALUE, table.frequency(symbol) + share));
        }
        return table.seal();
    }

    /** Whether the literal cannot be {@code symbol}. */
    boolean excluded(int symbol) {
        return this.excludedIn[symbol] == this.literal;
    }

    /** Whether any byte is left out. */
    boolean excludedAny() {
        return this.excludedCount > 0;
    }

    /** How often {@code symbol} came as a literal, in the model's units. */
    int frequency(int symbol) {
        return this.frequencies.frequency(symbol);
    }

    /** Whether any byte got a vote. */
    boolean voted() {
        return this.voteTotal > 0;
    }

    void update(int symbol) {
        this.frequencies.increment(symbol);
    }

    private static FrequencyCounts flat() {
        int[] initial = new int[SYMBOLS];
        Arrays.fill(initial, 1);
        return new FrequencyCounts(initial);
    }
}
