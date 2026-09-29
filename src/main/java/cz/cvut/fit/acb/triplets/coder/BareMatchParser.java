package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.SearchResult;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletParser;

/** For a coder whose match carries no literal, so a match may end the segment. */
public final class BareMatchParser implements TripletParser {

    @Override
    public Triplet parse(SegmentBuffer segment, int idx, SearchResult found) {
        return switch (found) {
            case SearchResult.Miss miss -> Triplet.literal(segment.byteAt(idx));
            case SearchResult.Hit hit -> Triplet.match(hit.distance(), hit.length());
        };
    }
}
