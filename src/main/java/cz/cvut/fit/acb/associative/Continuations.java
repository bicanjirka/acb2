package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.dictionary.Funnel;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;

/**
 * The candidates of a step that still agree with its text, and the bytes they go on with next, each with
 * the summed weight of the candidates that give it, heaviest first. The candidates are the funnel's and
 * the repeats: the candidates the last step ended with, moved past the byte where the text left them. The
 * step walks down their contents a byte at a time and keeps only the candidates that went on as the text
 * did. One is refilled at every byte, so it holds primitive arrays that are overwritten in place.
 */
final class Continuations {

    private static final int SYMBOLS = 256;

    private final int[] positions;
    private final int[] weights;
    private final int[] agreements;
    private final boolean[] repeats;
    private final int[] live;
    private final long[] weightOf = new long[SYMBOLS];
    private final int[] countOf = new int[SYMBOLS];
    private final int[] agreementOf = new int[SYMBOLS];
    private final boolean[] repeatedOf = new boolean[SYMBOLS];
    private final int[] nearestOf = new int[SYMBOLS];
    private final int[] bytes = new int[SYMBOLS];
    private int size;
    private int liveCount;
    private int distinct;
    private long total;
    private boolean anyRepeat;

    /** @param capacity the most candidates a step has, repeats included */
    Continuations(int capacity) {
        this.positions = new int[capacity];
        this.weights = new int[capacity];
        this.agreements = new int[capacity];
        this.repeats = new boolean[capacity];
        this.live = new int[capacity];
    }

    /** Starts a step with every candidate of {@code funnel} alive. */
    void start(Funnel funnel) {
        this.size = 0;
        for (int i = 0; i < funnel.size(); i++) {
            this.add(funnel.position(i), funnel.weight(i), funnel.agreement(i), false);
        }
    }

    /** Adds a repeat whose content starts at {@code position}. */
    void addRepeat(int position, int weight) {
        this.add(position, weight, 0, true);
    }

    private void add(int position, int weight, int agreement, boolean repeat) {
        this.positions[this.size] = position;
        this.weights[this.size] = weight;
        this.agreements[this.size] = agreement;
        this.repeats[this.size] = repeat;
        this.live[this.size] = this.size;
        this.size++;
        this.liveCount = this.size;
    }

    /** Finds the bytes the live candidates go on with {@code depth} bytes into their contents. */
    void gather(SegmentBuffer text, int depth, int idx) {
        for (int i = 0; i < this.distinct; i++) {
            int value = this.bytes[i];
            this.weightOf[value] = 0;
            this.countOf[value] = 0;
            this.agreementOf[value] = 0;
            this.repeatedOf[value] = false;
            this.nearestOf[value] = Integer.MAX_VALUE;
        }
        this.distinct = 0;
        this.total = 0;
        this.anyRepeat = false;
        for (int i = 0; i < this.liveCount; i++) {
            int candidate = this.live[i];
            int value = Byte.toUnsignedInt(text.byteAt(this.positions[candidate] + depth));
            if (this.countOf[value] == 0) {
                this.bytes[this.distinct++] = value;
                this.nearestOf[value] = Integer.MAX_VALUE;
            }
            this.nearestOf[value] = Math.min(this.nearestOf[value], idx - this.positions[candidate]);
            int weight = this.weights[candidate];
            this.weightOf[value] += weight;
            this.countOf[value]++;
            this.agreementOf[value] = Math.max(this.agreementOf[value], this.agreements[candidate]);
            if (this.repeats[candidate]) {
                this.repeatedOf[value] = true;
                this.anyRepeat = true;
            }
            this.total += weight;
        }
        for (int i = 1; i < this.distinct; i++) {
            int value = this.bytes[i];
            long weight = this.weightOf[value];
            int j = i - 1;
            while (j >= 0 && this.weightOf[this.bytes[j]] < weight) {
                this.bytes[j + 1] = this.bytes[j];
                j--;
            }
            this.bytes[j + 1] = value;
        }
    }

    /** Keeps the candidates that go on with {@code value} {@code depth} bytes into their contents. */
    void keep(SegmentBuffer text, int depth, int value) {
        int kept = 0;
        for (int i = 0; i < this.liveCount; i++) {
            int candidate = this.live[i];
            if (Byte.toUnsignedInt(text.byteAt(this.positions[candidate] + depth)) == value) {
                this.live[kept++] = candidate;
            }
        }
        this.liveCount = kept;
    }

    int liveCount() {
        return this.liveCount;
    }

    /** Where the content of the {@code i}-th live candidate starts. */
    int livePosition(int i) {
        return this.positions[this.live[i]];
    }

    /** How many different bytes the live candidates go on with. */
    int distinct() {
        return this.distinct;
    }

    /** The {@code rank}-th heaviest of the bytes. */
    int byteAt(int rank) {
        return this.bytes[rank];
    }

    long weight(int rank) {
        return this.weightOf[this.bytes[rank]];
    }

    /** How many live candidates go on with the {@code rank}-th heaviest byte. */
    int count(int rank) {
        return this.countOf[this.bytes[rank]];
    }

    /** The most bits of context agreement among the candidates that go on with the {@code rank}-th byte. */
    int agreement(int rank) {
        return this.agreementOf[this.bytes[rank]];
    }

    /** How far back from the step the nearest candidate that goes on with the {@code rank}-th byte lies. */
    int nearest(int rank) {
        return this.nearestOf[this.bytes[rank]];
    }

    /** Whether a repeat goes on with the {@code rank}-th heaviest byte. */
    boolean repeated(int rank) {
        return this.repeatedOf[this.bytes[rank]];
    }

    /** Whether a repeat is among the live candidates. */
    boolean anyRepeat() {
        return this.anyRepeat;
    }

    long total() {
        return this.total;
    }

    /** The rank of {@code value} among the bytes, or -1 if no live candidate goes on with it. */
    int rankOf(int value) {
        if (this.countOf[value] == 0) {
            return -1;
        }
        for (int rank = 0; ; rank++) {
            if (this.bytes[rank] == value) {
                return rank;
            }
        }
    }
}
