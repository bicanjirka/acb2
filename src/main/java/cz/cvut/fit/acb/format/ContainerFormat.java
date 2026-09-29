package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.CRC32;

/**
 * The on-disk layout of one compressed stream, big-endian:
 * <pre>
 * "ACB"  version:u8
 * distanceBits:u8  lengthBits:u8  tripletCoding:u8  entropyCoding:u8  contextDepth:u8  segmentSize:i32
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
 * entropy models start empty in each, so blocks depend on nothing before them. The dictionary orders
 * its entries by the last {@code contextDepth} bytes before each position, as unsigned bytes read
 * from the nearest back, and by position when those are equal; that order decides every rank a
 * triplet names, so it is part of the format. Coder codes are fixed
 * by {@link TripletCoding#formatCode()} and {@link EntropyCoding#formatCode()}. Any change to this
 * layout, to those codes, to the order, or to how a coder lays out triplets bumps {@link #VERSION}.
 */
public final class ContainerFormat {

    static final int VERSION = 4;

    private static final byte[] MAGIC = {'A', 'C', 'B'};

    private ContainerFormat() {
    }

    public static byte[] encode(CompressedStream stream) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.write(MAGIC);
            out.writeByte(VERSION);
            HeaderCodec.write(stream.header(), out);
            BlockCodec.write(stream.blocks(), out);
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
        WireReader in = new WireReader(bytes, MAGIC.length + 1, bodyLength);
        StreamHeader header = HeaderCodec.read(in);
        List<Block> blocks = BlockCodec.read(in, header.segmentSize());
        if (in.remaining() > 0) {
            throw new MalformedStreamException(in.remaining() + " unexpected bytes after the last block");
        }
        return new CompressedStream(header, blocks);
    }

    private static long checksum(byte[] bytes, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, 0, length);
        return crc.getValue();
    }
}
