package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

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
 * {@link ContainerWriter} and {@link ContainerReader} do the work a block at a time; the methods here
 * are their in-memory form.
 */
public final class ContainerFormat {

    static final int VERSION = 4;

    static final byte[] MAGIC = {'A', 'C', 'B'};

    private ContainerFormat() {
    }

    public static byte[] encode(CompressedStream stream) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ContainerWriter writer = ContainerWriter.begin(bytes::writeBytes, stream.header(), stream.blocks().size());
        stream.blocks().forEach(writer);
        writer.finish();
        return bytes.toByteArray();
    }

    public static CompressedStream decode(byte[] bytes) throws MalformedStreamException {
        try (ContainerReader reader = ContainerReader.open(ByteSource.of(bytes))) {
            return new CompressedStream(reader.header(), reader.drain());
        } catch (MalformedStreamException e) {
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException("In-memory read failed", e);
        }
    }
}
