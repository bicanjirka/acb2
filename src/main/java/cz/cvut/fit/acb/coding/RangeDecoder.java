package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * Reads what {@link RangeEncoder} wrote. It looks four bytes ahead of the encoder, so a sound stream
 * may be read a little past its end; those bytes are zeros, and a stream that needs more than a few
 * is corrupt.
 */
public final class RangeDecoder {

    /** Zero bytes past the payload a sound stream may need; it needs four at most. */
    private static final int PADDING = 8;

    private final byte[] bytes;
    private int position;
    private long code;
    private long range = RangeEncoder.FULL_RANGE;
    private long unit;

    /** @throws MalformedStreamException if the payload is too short to start decoding */
    public RangeDecoder(byte[] bytes) throws MalformedStreamException {
        this.bytes = bytes;
        for (int i = 0; i < Integer.BYTES; i++) {
            this.code = (this.code << 8) | this.nextByte();
        }
    }

    /**
     * The value in {@code 0 .. total-1} that the next symbol's cumulative range contains; follow with
     * {@link #consume} for that symbol.
     *
     * @throws MalformedStreamException if the stream holds no such value
     */
    public int target(int total) throws MalformedStreamException {
        if (total <= 0 || total > RangeEncoder.MAX_TOTAL) {
            throw new IllegalArgumentException("Not a total of a distribution: " + total);
        }
        this.unit = this.range / total;
        long value = this.code / this.unit;
        if (value >= total) {
            throw new MalformedStreamException("A range-coded field holds a value outside its distribution");
        }
        return (int) value;
    }

    /** Takes the symbol found for the last {@link #target}. */
    public void consume(int cumulative, int frequency) throws MalformedStreamException {
        this.code -= this.unit * cumulative;
        this.range = this.unit * frequency;
        while (this.range < RangeEncoder.TOP) {
            this.code = ((this.code << 8) | this.nextByte()) & RangeEncoder.FULL_RANGE;
            this.range <<= 8;
        }
    }

    private int nextByte() throws MalformedStreamException {
        if (this.position < this.bytes.length) {
            return this.bytes[this.position++] & 0xFF;
        }
        if (++this.position > this.bytes.length + PADDING) {
            throw new MalformedStreamException("A range-coded field ends inside its data");
        }
        return 0;
    }
}
