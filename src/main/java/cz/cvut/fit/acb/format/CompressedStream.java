package cz.cvut.fit.acb.format;

import java.util.List;

/** A header and one block per segment, in order; no block is longer than the header's segment size. */
public record CompressedStream(StreamHeader header, List<Block> blocks) {

    public CompressedStream {
        blocks = List.copyOf(blocks);
        for (Block block : blocks) {
            if (block.rawLength() > header.segmentSize()) {
                throw new IllegalArgumentException("A block of " + block.rawLength() + " bytes exceeds the segment size "
                        + header.segmentSize());
            }
        }
    }
}
