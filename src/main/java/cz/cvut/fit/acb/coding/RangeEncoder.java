package cz.cvut.fit.acb.coding;

import java.util.Arrays;

/**
 * A byte-oriented range coder: a 32-bit range renormalised a byte at a time, with the carry
 * propagated into the bytes already pending, as in LZMA. Symbols come as {@code (cumulative,
 * frequency, total)} against any distribution whose total is at most {@link #MAX_TOTAL}.
 */
public final class RangeEncoder {

    /** Largest total a symbol may be coded against; the range must stay much larger than the total. */
    public static final int MAX_TOTAL = 1 << 20;

    /** A bit's probability is a number of {@code 2^-16}ths. */
    public static final int PROBABILITY_BITS = 16;
    public static final int PROBABILITY_ONE = 1 << PROBABILITY_BITS;

    static final long TOP = 1L << 24;
    static final long FULL_RANGE = 0xFFFFFFFFL;

    private byte[] bytes = new byte[64];
    private int length;
    private long low;
    private long range = FULL_RANGE;
    private int cache;
    private long pending = 1;
    private boolean started;
    private boolean finished;

    public void encode(int cumulative, int frequency, int total) {
        if (this.finished) {
            throw new IllegalStateException("The coder is finished");
        }
        if (frequency <= 0 || cumulative < 0 || cumulative + frequency > total || total > MAX_TOTAL) {
            throw new IllegalArgumentException("Not a symbol of a distribution: cumulative " + cumulative
                    + ", frequency " + frequency + ", total " + total);
        }
        long unit = this.range / total;
        this.low += unit * cumulative;
        this.range = unit * frequency;
        while (this.range < TOP) {
            this.range <<= 8;
            this.shiftLow();
        }
    }

    /**
     * Codes one bit against the probability that it is 1, in units of {@code 2^-}{@link #PROBABILITY_BITS};
     * the same as {@link #encode} over a total of {@code 2^PROBABILITY_BITS}, with 1 the first symbol.
     */
    public void encodeBit(int bit, int probabilityOfOne) {
        if (this.finished) {
            throw new IllegalStateException("The coder is finished");
        }
        if (probabilityOfOne <= 0 || probabilityOfOne >= PROBABILITY_ONE) {
            throw new IllegalArgumentException("Not a probability of a bit that can be either: " + probabilityOfOne);
        }
        long unit = this.range >>> PROBABILITY_BITS;
        if (bit != 0) {
            this.range = unit * probabilityOfOne;
        } else {
            this.low += unit * probabilityOfOne;
            this.range = unit * (PROBABILITY_ONE - probabilityOfOne);
        }
        while (this.range < TOP) {
            this.range <<= 8;
            this.shiftLow();
        }
    }

    /** Flushes the coder; further symbols are refused. Safe to call again. */
    public void finish() {
        if (!this.finished) {
            for (int i = 0; i < 5; i++) {
                this.shiftLow();
            }
            this.finished = true;
        }
    }

    /** The bytes so far; all of the stream once {@link #finish()} has been called. */
    public byte[] toArray() {
        return Arrays.copyOf(this.bytes, this.length);
    }

    private void shiftLow() {
        if (this.low < 0xFF000000L || this.low > FULL_RANGE) {
            int carry = (int) (this.low >>> 32);
            int next = this.cache;
            do {
                this.write(next + carry);
                next = 0xFF;
            } while (--this.pending != 0);
            this.cache = (int) ((this.low >>> 24) & 0xFF);
        }
        this.pending++;
        this.low = (this.low & 0x00FFFFFFL) << 8;
    }

    /** The first byte the shifts produce is always zero, so it is left out and the decoder skips it. */
    private void write(int value) {
        if (!this.started) {
            this.started = true;
            return;
        }
        if (this.length == this.bytes.length) {
            this.bytes = Arrays.copyOf(this.bytes, this.length * 2);
        }
        this.bytes[this.length++] = (byte) value;
    }
}
