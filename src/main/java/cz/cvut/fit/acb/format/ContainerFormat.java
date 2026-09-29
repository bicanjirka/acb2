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
 * distanceBits:u8  lengthBits:u8  tripletCoding:u8  entropyCoding:u8  segmentSize:i32
 * frequencyCount:i32  frequency:i32 * frequencyCount
 * blockCount:i32  block * blockCount
 * crc32 of everything above:i32
 * </pre>
 * where a block is one segment, either
 * <pre>
 * 0  length:i32  bytes                    the segment as it is, when coding did not shrink it
 * 1  rawLength:i32  codedLength:i32  bytes    the segment coded
 * </pre>
 * with a length of 1 to {@code segmentSize}. Every block is coded on its own: the dictionary and the
 * entropy models start empty in each, so blocks depend on nothing before them. Coder codes are fixed
 * by {@link TripletCoding#formatCode()} and {@link EntropyCoding#formatCode()}. Any change to this
 * layout, to those codes, or to how a coder lays out triplets bumps {@link #VERSION}.
 */
public final class ContainerFormat {

    static final int VERSION = 3;

    private static final byte[] MAGIC = {'A', 'C', 'B'};
    private static final int STORED = 0;
    private static final int CODED = 1;
    /** The least a block takes: its kind and its length. */
    private static final int MIN_BLOCK_SIZE = 1 + Integer.BYTES;

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
            out.writeByte(header.tripletCoding().formatCode());
            out.writeByte(header.entropyCoding().formatCode());
            out.writeInt(header.segmentSize());
            int[] frequencies = header.lengthFrequencies();
            out.writeInt(frequencies.length);
            for (int frequency : frequencies) {
                out.writeInt(frequency);
            }
            out.writeInt(stream.blocks().size());
            for (Block block : stream.blocks()) {
                switch (block) {
                    case Block.Stored stored -> {
                        out.writeByte(STORED);
                        out.writeInt(stored.rawLength());
                        out.write(stored.bytes());
                    }
                    case Block.Coded coded -> {
                        out.writeByte(CODED);
                        out.writeInt(coded.rawLength());
                        out.writeInt(coded.storedLength());
                        out.write(coded.bytes());
                    }
                }
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
            int tripletCode = Byte.toUnsignedInt(in.get());
            int entropyCode = Byte.toUnsignedInt(in.get());
            TripletCoding tripletCoding = TripletCoding.byFormatCode(tripletCode)
                    .orElseThrow(() -> new MalformedStreamException("Unknown triplet coding code " + tripletCode));
            EntropyCoding entropyCoding = EntropyCoding.byFormatCode(entropyCode)
                    .orElseThrow(() -> new MalformedStreamException("Unknown entropy coding code " + entropyCode));
            int segmentSize = in.getInt();
            if (segmentSize < 1) {
                throw new MalformedStreamException("Invalid segment size " + segmentSize);
            }
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
            int blockCount = count(in, MIN_BLOCK_SIZE, "block");
            List<Block> blocks = new ArrayList<>(blockCount);
            for (int i = 0; i < blockCount; i++) {
                blocks.add(block(in, segmentSize));
            }
            if (in.hasRemaining()) {
                throw new MalformedStreamException(in.remaining() + " unexpected bytes after the last block");
            }
            return new CompressedStream(
                    new StreamHeader(distanceBits, lengthBits, tripletCoding, entropyCoding, frequencies,
                            segmentSize), blocks);
        } catch (BufferUnderflowException e) {
            throw new MalformedStreamException("ACB stream ends inside its blocks");
        }
    }

    private static Block block(ByteBuffer in, int segmentSize) throws MalformedStreamException {
        int kind = Byte.toUnsignedInt(in.get());
        int rawLength = in.getInt();
        if (rawLength < 1 || rawLength > segmentSize) {
            throw new MalformedStreamException("Invalid block length " + rawLength + " for segments of "
                    + segmentSize);
        }
        return switch (kind) {
            case STORED -> Block.stored(bytes(in, rawLength));
            case CODED -> Block.coded(rawLength, bytes(in, count(in, 1, "coded byte")));
            default -> throw new MalformedStreamException("Unknown block kind " + kind);
        };
    }

    private static byte[] bytes(ByteBuffer in, int length) throws MalformedStreamException {
        if (length > in.remaining()) {
            throw new MalformedStreamException("A block of " + length + " bytes runs past the end of the stream");
        }
        byte[] bytes = new byte[length];
        in.get(bytes);
        return bytes;
    }

    private static long checksum(byte[] bytes, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, length);
        return crc.getValue();
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
