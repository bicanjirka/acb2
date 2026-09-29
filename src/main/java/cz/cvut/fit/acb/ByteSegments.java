package cz.cvut.fit.acb;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** The bytes of an array cut into segments of {@code segmentSize}, the last one shorter if need be. */
final class ByteSegments implements Iterator<byte[]> {

    private final byte[] input;
    private final int segmentSize;
    private int from;

    ByteSegments(byte[] input, int segmentSize) {
        this.input = input;
        this.segmentSize = segmentSize;
    }

    @Override
    public boolean hasNext() {
        return this.from < this.input.length;
    }

    @Override
    public byte[] next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        int to = (int) Math.min(this.input.length, (long) this.from + this.segmentSize);
        byte[] segment = Arrays.copyOfRange(this.input, this.from, to);
        this.from = to;
        return segment;
    }
}
