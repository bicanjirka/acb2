package cz.cvut.fit.acb;

import java.util.Optional;
import java.util.Set;

/** What one {@link TripletCoding} is: how it codes a segment, and what of the settings it understands. */
interface Coder {

    SegmentCoding segmentCoding(CompressionSettings settings);

    /** The coder as a layout of triplet fields, if it is one. */
    Optional<LayoutCoder> layoutCoder();

    /** The entropy codings the coder can be used with. */
    Set<EntropyCoding> entropyCodings();

    /** The settings this coder starts from, given the general defaults. */
    CompressionSettings preset(CompressionSettings defaults);
}
