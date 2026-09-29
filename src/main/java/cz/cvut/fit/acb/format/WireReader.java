package cz.cvut.fit.acb.format;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/** Reads the big-endian body of a stream; running out of bytes is a malformed stream, never a stray exception. */
final class WireReader {

    private final ByteBuffer in;

    WireReader(byte[] bytes, int from, int to) {
        this.in = ByteBuffer.wrap(bytes, from, to - from);
    }

    int unsignedByte() throws MalformedStreamException {
        try {
            return Byte.toUnsignedInt(this.in.get());
        } catch (BufferUnderflowException e) {
            throw ends();
        }
    }

    int int32() throws MalformedStreamException {
        try {
            return this.in.getInt();
        } catch (BufferUnderflowException e) {
            throw ends();
        }
    }

    /** A count read before its items, checked against what is left so a bad value cannot over-allocate. */
    int count(int itemSize, String what) throws MalformedStreamException {
        int count = this.int32();
        if (count < 0 || (long) count * itemSize > this.in.remaining()) {
            throw new MalformedStreamException("Invalid " + what + " count " + count);
        }
        return count;
    }

    byte[] bytes(int length) throws MalformedStreamException {
        if (length > this.in.remaining()) {
            throw new MalformedStreamException("A block of " + length + " bytes runs past the end of the stream");
        }
        byte[] bytes = new byte[length];
        this.in.get(bytes);
        return bytes;
    }

    int remaining() {
        return this.in.remaining();
    }

    private static MalformedStreamException ends() {
        return new MalformedStreamException("ACB stream ends inside its blocks");
    }
}
