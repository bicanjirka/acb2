package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;

/**
 * The bytes of one segment, as far as they are known: all of them for the encoder, and those
 * decoded so far for the decoder, which appends as it goes. Both sides read it the same way.
 */
public final class SegmentBuffer {

    private static final int INITIAL_CAPACITY = 64;
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    private byte[] bytes;
    private int length;

    private SegmentBuffer(byte[] bytes, int length) {
        this.bytes = bytes;
        this.length = length;
    }

    /** A buffer over {@code segment} itself, which the caller must not change afterwards. */
    public static SegmentBuffer of(byte[] segment) {
        return new SegmentBuffer(segment, segment.length);
    }

    public static SegmentBuffer empty() {
        return new SegmentBuffer(new byte[INITIAL_CAPACITY], 0);
    }

    public int length() {
        return this.length;
    }

    public byte byteAt(int index) {
        if (index >= this.length) {
            throw new IndexOutOfBoundsException("Index " + index + " is past the " + this.length + " bytes known");
        }
        return this.bytes[index];
    }

    public void append(byte value) {
        this.ensureRoomFor(1);
        this.bytes[this.length++] = value;
    }

    /**
     * Appends {@code count} bytes read from {@code from} on. A copy that runs into the bytes it is
     * appending repeats them, which is what a match longer than its distance means.
     *
     * @throws IllegalArgumentException if there is nothing at {@code from} to copy
     */
    public void appendCopy(int from, int count) {
        if (count == 0) {
            return;
        }
        if (from < 0 || from >= this.length) {
            throw new IllegalArgumentException("Position " + from + " is outside the " + this.length + " bytes known");
        }
        this.ensureRoomFor(count);
        int apart = this.length - from;
        if (count <= apart) {
            System.arraycopy(this.bytes, from, this.bytes, this.length, count);
        } else {
            for (int i = 0; i < count; i++) {
                this.bytes[this.length + i] = this.bytes[from + i];
            }
        }
        this.length += count;
    }

    /**
     * How many bytes, at most {@code limit}, are equal from {@code first} on and from {@code second}
     * on, without reading past the known bytes.
     */
    public int commonLength(int first, int second, int limit) {
        int count = Math.min(limit, this.length - Math.max(first, second));
        if (count <= 0) {
            return 0;
        }
        int mismatch = Arrays.mismatch(this.bytes, first, first + count, this.bytes, second, second + count);
        return mismatch < 0 ? count : mismatch;
    }

    /**
     * How many bytes, at most {@code limit}, are equal going backwards from just before {@code first}
     * and from just before {@code second}, stopping at the start of the segment.
     */
    public int commonSuffixLength(int first, int second, int limit) {
        int count = Math.min(limit, Math.min(first, second));
        int k = 0;
        while (k < count && this.bytes[first - 1 - k] == this.bytes[second - 1 - k]) {
            k++;
        }
        return k;
    }

    /** A copy of the known bytes. */
    public byte[] toArray() {
        return Arrays.copyOf(this.bytes, this.length);
    }

    /** The backing array, for readers that compare bytes in a loop; valid until the next append. */
    byte[] bytes() {
        return this.bytes;
    }

    private void ensureRoomFor(int extra) {
        long needed = (long) this.length + extra;
        if (needed > this.bytes.length) {
            if (needed > MAX_CAPACITY) {
                throw new IllegalStateException("A segment cannot hold " + needed + " bytes");
            }
            this.bytes = Arrays.copyOf(this.bytes, (int) Math.min(MAX_CAPACITY, Math.max(needed, 2L * this.bytes.length)));
        }
    }
}
