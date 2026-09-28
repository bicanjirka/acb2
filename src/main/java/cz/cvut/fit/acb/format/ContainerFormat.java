package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * The on-disk layout of one compressed stream, big-endian:
 * <pre>
 * "ACB"  version:u8
 * distanceBits:u8  lengthBits:u8  tripletCoding:u8  entropyCoding:u8
 * frequencyCount:i32  frequency:i32 * frequencyCount
 * arrayCount:i32  (length:i32  bytes) * arrayCount
 * crc32 of everything above:i32
 * </pre>
 * Coder codes are fixed here rather than taken from enum ordinals, so reordering an enum cannot
 * change what old files mean. Any change to this layout bumps {@link #VERSION}.
 */
public final class ContainerFormat {

    static final int VERSION = 2;

    private static final byte[] MAGIC = {'A', 'C', 'B'};
    private static final TripletCoding[] TRIPLET_CODES = {
            TripletCoding.SIMPLE, TripletCoding.SALOMON, TripletCoding.SALOMON2, TripletCoding.VALACH, TripletCoding.LCP};
    private static final EntropyCoding[] ENTROPY_CODES = {EntropyCoding.ADAPTIVE_ARITHMETIC, EntropyCoding.BIT_ARRAY};

    private ContainerFormat() {
    }

    public static byte[] encode(CompressedStream stream) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            StreamHeader header = stream.header();
            out.write(MAGIC);
            out.writeByte(VERSION);
            out.writeByte(header.distanceBits());
            out.writeByte(header.lengthBits());
            out.writeByte(codeOf(TRIPLET_CODES, header.tripletCoding()));
            out.writeByte(codeOf(ENTROPY_CODES, header.entropyCoding()));
            int[] frequencies = header.lengthFrequencies();
            out.writeInt(frequencies.length);
            for (int frequency : frequencies) {
                out.writeInt(frequency);
            }
            out.writeInt(stream.payload().size());
            for (byte[] array : stream.payload()) {
                out.writeInt(array.length);
                out.write(array);
            }
            out.writeInt((int) checksum(bytes.toByteArray(), bytes.size()));
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("In-memory write failed", e);
        }
    }

    public static CompressedStream decode(byte[] bytes) throws MalformedStreamException {
        if (bytes.length < MAGIC.length + 1 + Integer.BYTES
                || bytes[0] != MAGIC[0] || bytes[1] != MAGIC[1] || bytes[2] != MAGIC[2]) {
            throw new MalformedStreamException("Not an ACB stream");
        }
        int version = Byte.toUnsignedInt(bytes[MAGIC.length]);
        if (version != VERSION) {
            throw new MalformedStreamException("Unsupported ACB stream version " + version + ", expected " + VERSION);
        }
        int bodyLength = bytes.length - Integer.BYTES;
        int storedChecksum = ByteBuffer.wrap(bytes, bodyLength, Integer.BYTES).getInt();
        if (storedChecksum != (int) checksum(bytes, bodyLength)) {
            throw new MalformedStreamException("ACB stream is corrupt or truncated (checksum mismatch)");
        }
        try {
            ByteBuffer in = ByteBuffer.wrap(bytes, MAGIC.length + 1, bodyLength - MAGIC.length - 1);
            int distanceBits = bits(in.get(), "distance");
            int lengthBits = bits(in.get(), "length");
            TripletCoding tripletCoding = byCode(TRIPLET_CODES, in.get(), "triplet coding");
            EntropyCoding entropyCoding = byCode(ENTROPY_CODES, in.get(), "entropy coding");
            int frequencyCount = count(in, Integer.BYTES, "frequency");
            if (frequencyCount > CompressionSettings.lengthAlphabetSize(lengthBits)) {
                throw new MalformedStreamException("Invalid frequency count " + frequencyCount);
            }
            int[] frequencies = new int[frequencyCount];
            for (int i = 0; i < frequencies.length; i++) {
                frequencies[i] = in.getInt();
            }
            try {
                CompressionSettings.requireLengthFrequencies(lengthBits, frequencies);
            } catch (IllegalArgumentException e) {
                throw new MalformedStreamException("Invalid length frequencies: " + e.getMessage());
            }
            int arrayCount = count(in, Integer.BYTES, "array");
            List<byte[]> payload = new ArrayList<>(arrayCount);
            for (int i = 0; i < arrayCount; i++) {
                byte[] array = new byte[count(in, 1, "byte")];
                in.get(array);
                payload.add(array);
            }
            if (in.hasRemaining()) {
                throw new MalformedStreamException(in.remaining() + " unexpected bytes after the payload");
            }
            return new CompressedStream(
                    new StreamHeader(distanceBits, lengthBits, tripletCoding, entropyCoding, frequencies), payload);
        } catch (BufferUnderflowException e) {
            throw new MalformedStreamException("ACB stream ends inside its payload");
        }
    }

    private static long checksum(byte[] bytes, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, length);
        return crc.getValue();
    }

    private static <E> int codeOf(E[] codes, E value) {
        for (int code = 0; code < codes.length; code++) {
            if (codes[code] == value) {
                return code;
            }
        }
        throw new IllegalArgumentException("No format code for " + value);
    }

    private static <E> E byCode(E[] codes, byte stored, String what) throws MalformedStreamException {
        int code = Byte.toUnsignedInt(stored);
        if (code >= codes.length) {
            throw new MalformedStreamException("Unknown " + what + " code " + code);
        }
        return codes[code];
    }

    private static int bits(byte stored, String what) throws MalformedStreamException {
        int bits = Byte.toUnsignedInt(stored);
        if (bits < 1 || bits > CompressionSettings.MAX_FIELD_BITS) {
            throw new MalformedStreamException("Invalid " + what + " bit width " + bits);
        }
        return bits;
    }

    /** A count read before its items, checked against what is left so a bad value cannot over-allocate. */
    private static int count(ByteBuffer in, int itemSize, String what) throws MalformedStreamException {
        int count = in.getInt();
        if (count < 0 || (long) count * itemSize > in.remaining()) {
            throw new MalformedStreamException("Invalid " + what + " count " + count);
        }
        return count;
    }
}
