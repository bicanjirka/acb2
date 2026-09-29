package cz.cvut.fit.acb.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

/**
 * Reads the big-endian body of a stream of a known length; running out of bytes is a malformed
 * stream, never a stray exception, and no count or length allocates more than the body has left.
 */
final class WireReader {

    private final InputStream in;
    private long remaining;

    WireReader(InputStream in, long length) {
        this.in = in;
        this.remaining = length;
    }

    int unsignedByte() throws MalformedStreamException {
        return Byte.toUnsignedInt(this.take(1)[0]);
    }

    int int32() throws MalformedStreamException {
        return ByteBuffer.wrap(this.take(Integer.BYTES)).getInt();
    }

    /** A count read before its items, checked against what is left so a bad value cannot over-allocate. */
    int count(int itemSize, String what) throws MalformedStreamException {
        int count = this.int32();
        if (count < 0 || (long) count * itemSize > this.remaining) {
            throw new MalformedStreamException("Invalid " + what + " count " + count);
        }
        return count;
    }

    byte[] bytes(int length) throws MalformedStreamException {
        if (length > this.remaining) {
            throw new MalformedStreamException("A block of " + length + " bytes runs past the end of the stream");
        }
        return this.take(length);
    }

    long remaining() {
        return this.remaining;
    }

    private byte[] take(int length) throws MalformedStreamException {
        if (length > this.remaining) {
            throw ends();
        }
        byte[] bytes;
        try {
            bytes = this.in.readNBytes(length);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the stream", e);
        }
        if (bytes.length < length) {
            throw ends();
        }
        this.remaining -= length;
        return bytes;
    }

    private static MalformedStreamException ends() {
        return new MalformedStreamException("ACB stream ends inside its blocks");
    }
}
