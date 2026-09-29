package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.dictionary.SearchResult;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;

/** Encoder only: turns what the dictionary found at a position into the triplet that codes it. */
public interface TripletParser {

    /**
     * @param segment the whole segment
     * @param idx the position coding continues from, below the segment's length
     */
    Triplet parse(SegmentBuffer segment, int idx, SearchResult found);
}
