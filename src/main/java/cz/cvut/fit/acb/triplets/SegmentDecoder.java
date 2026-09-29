package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.dictionary.DecoderDictionary;
import cz.cvut.fit.acb.dictionary.SearchWindow;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;

/** Rebuilds one segment of a known length from the triplets a {@link SegmentEncoder} wrote. */
public final class SegmentDecoder {

    private final DecoderDictionary dictionary;
    private final TripletLayout layout;
    private final SearchWindow window;

    public SegmentDecoder(DecoderDictionary dictionary, TripletLayout layout, SearchWindow window) {
        this.dictionary = dictionary;
        this.layout = layout;
        this.window = window;
    }

    /**
     * Decodes triplets into {@code segment}, which must be empty, until it holds {@code length} bytes.
     *
     * @throws MalformedStreamException if a triplet cannot be read, points outside the dictionary or
     *                                  runs past the end of the segment
     */
    public void decode(SegmentBuffer segment, int length, FieldSource source) throws MalformedStreamException {
        LiteralTracker tracker = new LiteralTracker(this.dictionary, segment, length, this.window.maxLength(),
                this.window.matchLimit());
        FieldSource fields = source.wantsLiteralContext() ? new LiteralContextSource(source, tracker) : source;
        SegmentState state = new SegmentState(this.dictionary);
        while (state.position() < length) {
            int idx = state.position();
            tracker.at(idx);
            Triplet triplet = this.layout.read(fields).with(distance -> this.dictionary.impliedLength(idx, distance));
            int overrun = idx + triplet.consumed() - length;
            if (overrun > 0) {
                throw new MalformedStreamException("A triplet runs " + overrun + " bytes past the end of its segment");
            }
            this.materialize(triplet, idx, segment);
            state.apply(triplet);
        }
    }

    private void materialize(Triplet triplet, int idx, SegmentBuffer segment) throws MalformedStreamException {
        switch (triplet) {
            case Triplet.Literal literal -> segment.append(literal.value());
            case Triplet.Match match -> this.copy(match.distance(), match.length(), idx, segment);
            case Triplet.MatchWithLiteral match -> {
                this.copy(match.distance(), match.length(), idx, segment);
                segment.append(match.literal());
            }
        }
    }

    /** Appends {@code length} bytes of the content {@code distance} ranks from the context of {@code idx}. */
    private void copy(int distance, int length, int idx, SegmentBuffer segment) throws MalformedStreamException {
        int rank = this.dictionary.contextRank(idx) - distance;
        segment.appendCopy(this.dictionary.select(rank), length);
    }
}
