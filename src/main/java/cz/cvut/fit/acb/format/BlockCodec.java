package cz.cvut.fit.acb.format;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** The blocks of the container, as {@link ContainerFormat} lays them out: a count, then each block by its kind. */
final class BlockCodec {

    private static final int STORED = 0;
    private static final int CODED = 1;
    /** The least a block takes: its kind and its length. */
    private static final int MIN_BLOCK_SIZE = 1 + Integer.BYTES;

    private BlockCodec() {
    }

    static void write(List<Block> blocks, DataOutputStream out) throws IOException {
        out.writeInt(blocks.size());
        for (Block block : blocks) {
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
    }

    static List<Block> read(WireReader in, int segmentSize) throws MalformedStreamException {
        int blockCount = in.count(MIN_BLOCK_SIZE, "block");
        List<Block> blocks = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) {
            blocks.add(readBlock(in, segmentSize));
        }
        return blocks;
    }

    private static Block readBlock(WireReader in, int segmentSize) throws MalformedStreamException {
        int kind = in.unsignedByte();
        int rawLength = in.int32();
        if (rawLength < 1 || rawLength > segmentSize) {
            throw new MalformedStreamException("Invalid block length " + rawLength + " for segments of "
                    + segmentSize);
        }
        return switch (kind) {
            case STORED -> Block.stored(in.bytes(rawLength));
            case CODED -> Block.coded(rawLength, in.bytes(in.count(1, "coded byte")));
            default -> throw new MalformedStreamException("Unknown block kind " + kind);
        };
    }
}
