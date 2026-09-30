package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * How one segment becomes a block and back, for the settings of a stream. It holds no state of its
 * own, so one serves every segment, on any number of threads; everything a segment is coded with is
 * made for it by the {@link ACBProvider}.
 */
public interface SegmentCoding {

    /** Codes {@code segment} into a block, stored as it is if coding does not shrink it. */
    CodedSegment encode(byte[] segment, ACBProvider provider);

    /** @throws MalformedStreamException if the block does not decode to a segment of its length */
    byte[] decode(Block.Coded block, ACBProvider provider) throws MalformedStreamException;
}
