package cz.cvut.fit.acb.format;

import java.io.DataOutputStream;
import java.io.IOException;

/** The blocks of the container, as {@link ContainerFormat} lays them out: a count, then each block by its kind. */
final class BlockCodec {

    private static final int STORED = 0;
    private static final int CODED = 1;
    /** The least a block takes: its kind and its length. */
    private static final int MIN_BLOCK_SIZE = 1 + Integer.BYTES;

    private BlockCodec() {
    }

    static void write(Block block, DataOutputStream out) throws IOException {
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

    static int readCount(WireReader in) throws MalformedStreamException {
        return in.count(MIN_BLOCK_SIZE, "block");
    }

    static Block read(WireReader in, int segmentSize) throws MalformedStreamException {
        int kind = in.unsignedByte();
        int rawLength = in.int32();
        if (rawLength < 1 || rawLength > segmentSize) {
            throw new MalformedStreamException("Invalid block length " + rawLength + " for segments of "
                    + segmentSize);
        }
        return switch (kind) {
            case STORED -> Block.stored(in.bytes(rawLength));
            case CODED -> Block.coded(rawLength, in.bytes(codedLength(in, segmentSize)));
            default -> throw new MalformedStreamException("Unknown block kind " + kind);
        };
    }

    /** Coding keeps a segment only when it comes out shorter, so a coded block is never longer than a segment. */
    private static int codedLength(WireReader in, int segmentSize) throws MalformedStreamException {
        int length = in.count(1, "coded byte");
        if (length > segmentSize) {
            throw new MalformedStreamException("Invalid coded byte count " + length + " for segments of "
                    + segmentSize);
        }
        return length;
    }
}
