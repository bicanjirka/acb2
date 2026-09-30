package cz.cvut.fit.acb.dictionary;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/**
 * How far two contexts agree, going back from the two positions. Eight bytes at a time are compared
 * as little-endian {@code long}s, whose top byte is the nearest, so the first difference of a pair is
 * found by a count of leading zeros. The handle is a constant, which the compiler needs to inline it.
 */
final class ContextAgreement {

    private static final VarHandle LONGS = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private ContextAgreement() {
    }

    /**
     * How many bytes are equal going back from just before {@code first} and from just before
     * {@code second}, at most {@code limit}, which must not pass the start of {@code bytes} for either.
     */
    static int commonBytes(byte[] bytes, int first, int second, int limit) {
        return commonBytesFrom(bytes, first, second, 0, limit);
    }

    /** As {@link #commonBytes}, for contexts known to agree on the first {@code known} bytes. */
    static int commonBytesFrom(byte[] bytes, int first, int second, int known, int limit) {
        int agreed = known;
        while (limit - agreed >= Long.BYTES) {
            long difference = (long) LONGS.get(bytes, first - agreed - Long.BYTES)
                    ^ (long) LONGS.get(bytes, second - agreed - Long.BYTES);
            if (difference != 0) {
                return agreed + (Long.numberOfLeadingZeros(difference) >>> 3);
            }
            agreed += Long.BYTES;
        }
        while (agreed < limit && bytes[first - 1 - agreed] == bytes[second - 1 - agreed]) {
            agreed++;
        }
        return agreed;
    }

    /**
     * The {@code count} bytes before {@code position}, which are at most eight and no more than
     * {@code position} itself, nearest first as the top bytes of a number, and zeros below them.
     */
    static long nearestBytes(byte[] bytes, int position, int count) {
        if (count == Long.BYTES) {
            return (long) LONGS.get(bytes, position - Long.BYTES);
        }
        long nearest = 0;
        for (int k = 1; k <= count; k++) {
            nearest |= (bytes[position - k] & 0xFFL) << (Long.SIZE - k * Byte.SIZE);
        }
        return nearest;
    }

    /**
     * The number of equal bits of two contexts, the nearest byte first and the top bit of a byte first,
     * known to agree on exactly {@code agreedBytes} bytes within {@code limit}, the most that can be
     * compared: the bits of the byte that broke the agreement are added to the whole bytes.
     */
    static int bitsAfter(byte[] bytes, int first, int second, int agreedBytes, int limit) {
        if (agreedBytes >= limit) {
            return limit << 3;
        }
        int difference = (bytes[first - 1 - agreedBytes] ^ bytes[second - 1 - agreedBytes]) & 0xFF;
        return (agreedBytes << 3) + Integer.numberOfLeadingZeros(difference) - (Integer.SIZE - Byte.SIZE);
    }
}
