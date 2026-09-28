package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.CompressedStream;

/** A compressed stream and what producing it did. */
public record CompressionResult(CompressedStream stream, CompressionStats stats) {
}
