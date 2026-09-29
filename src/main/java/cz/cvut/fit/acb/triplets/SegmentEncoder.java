package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.dictionary.EncoderDictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;

/** Codes one whole segment: searches, parses each step into a triplet and writes its fields. */
public final class SegmentEncoder {

    private final EncoderDictionary dictionary;
    private final TripletParser parser;
    private final TripletLayout layout;

    public SegmentEncoder(EncoderDictionary dictionary, TripletParser parser, TripletLayout layout) {
        this.dictionary = dictionary;
        this.parser = parser;
        this.layout = layout;
    }

    /** @return how many triplets the segment took */
    public long encode(SegmentBuffer segment, FieldSink sink) {
        SegmentState state = new SegmentState(this.dictionary);
        long triplets = 0;
        while (state.position() < segment.length()) {
            int idx = state.position();
            Triplet triplet = this.parser.parse(segment, idx, this.dictionary.search(idx));
            this.layout.write(triplet, sink);
            state.apply(triplet);
            triplets++;
        }
        return triplets;
    }
}
