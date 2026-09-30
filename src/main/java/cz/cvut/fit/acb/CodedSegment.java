package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.Block;

/** One segment as coded, and what coding it did. */
public record CodedSegment(Block block, CompressionStats stats) {
}
