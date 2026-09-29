package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.dictionary.EncoderDictionary;
import cz.cvut.fit.acb.dictionary.SearchWindow;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;

/** Codes one whole segment: searches, parses each step into a triplet and writes its fields. */
public final class SegmentEncoder {

    private final EncoderDictionary dictionary;
    private final TripletParser parser;
    private final TripletLayout layout;
    private final SearchWindow window;

    public SegmentEncoder(EncoderDictionary dictionary, TripletParser parser, TripletLayout layout,
                          SearchWindow window) {
        this.dictionary = dictionary;
        this.parser = parser;
        this.layout = layout;
        this.window = window;
    }

    /** @return how many triplets the segment took */
    public long encode(SegmentBuffer segment, FieldSink sink) {
        LiteralTracker tracker = new LiteralTracker(this.dictionary, segment, segment.length(),
                this.window.maxLength(), this.window.matchLimit());
        FieldSink fields = sink.wantsLiteralContext() ? new LiteralContextSink(sink, tracker) : sink;
        SegmentState state = new SegmentState(this.dictionary);
        long triplets = 0;
        while (state.position() < segment.length()) {
            int idx = state.position();
            tracker.at(idx);
            Triplet triplet = this.parser.parse(segment, idx, this.dictionary.search(idx));
            this.layout.write(triplet.without(distance -> this.dictionary.impliedLength(idx, distance)), fields);
            state.apply(triplet);
            triplets++;
        }
        return triplets;
    }
}
