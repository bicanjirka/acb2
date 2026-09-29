package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.SearchResult;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletParser;

/**
 * For coders whose match ends in a literal. A match that reaches the end of the segment leaves no
 * byte for that literal, so it gives up its last byte for it; a match of one byte becomes a
 * literal.
 */
public final class LiteralAfterMatchParser implements TripletParser {

    @Override
    public Triplet parse(SegmentBuffer segment, int idx, SearchResult found) {
        return switch (found) {
            case SearchResult.Miss miss -> Triplet.literal(segment.byteAt(idx));
            case SearchResult.Hit hit -> {
                int length = Math.min(hit.length(), segment.length() - idx - 1);
                yield length == 0
                        ? Triplet.literal(segment.byteAt(idx))
                        : Triplet.matchWithLiteral(hit.distance(), length, segment.byteAt(idx + length));
            }
        };
    }
}
