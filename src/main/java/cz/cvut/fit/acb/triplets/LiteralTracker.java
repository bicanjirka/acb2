package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * Follows the fields of a triplet as a layout reads or writes them, and works out the context of its
 * literal from them: the byte before the literal, and the byte the chosen content would go on with,
 * which the literal cannot be when the match ended on a mismatch. Both sides use it the same way, from
 * bytes both have.
 */
final class LiteralTracker {

    private final Dictionary dictionary;
    private final SegmentBuffer segment;
    private final int segmentLength;
    private final int maxLength;
    private final int matchLimit;
    private int position;
    private int distance;
    private int sentLength;

    /**
     * @param segment the bytes known so far: all of them for the encoder, those decoded for the decoder
     * @param segmentLength how long the whole segment is
     * @param maxLength the longest length a triplet carries; a match that long may have been cut short
     * @param matchLimit the longest a match is measured; a match that long may have been cut short too
     */
    LiteralTracker(Dictionary dictionary, SegmentBuffer segment, int segmentLength, int maxLength, int matchLimit) {
        this.dictionary = dictionary;
        this.segment = segment;
        this.segmentLength = segmentLength;
        this.maxLength = maxLength;
        this.matchLimit = matchLimit;
    }

    /** The position the next triplet is coded at. */
    void at(int idx) {
        this.position = idx;
    }

    /** Notes a field other than the literal. */
    void observe(TripletFieldId field, int value) {
        switch (field.kind()) {
            case FLAG -> this.sentLength = 0;
            case DISTANCE -> this.distance = FieldBits.signExtend(value, field.bitSize());
            case LENGTH -> this.sentLength = value;
            case LITERAL -> {
            }
        }
    }

    /** The context of the literal of the triplet at the position, whose other fields were observed; starts the next triplet. */
    LiteralContext context() throws MalformedStreamException {
        int idx = this.position;
        int length = this.sentLength;
        this.sentLength = 0;
        if (length == 0) {
            return new LiteralContext(idx > 0 ? Byte.toUnsignedInt(this.segment.byteAt(idx - 1)) : -1, -1);
        }
        int content = this.dictionary.select(this.dictionary.contextRank(idx) - this.distance);
        int full = length + this.dictionary.impliedLength(idx, this.distance);
        int period = idx - content;
        int previous = Byte.toUnsignedInt(this.segment.byteAt(content + (full - 1) % period));
        boolean endedOnMismatch = length < this.maxLength && full < this.matchLimit
                && idx + full + 1 < this.segmentLength;
        int excluded = endedOnMismatch ? Byte.toUnsignedInt(this.segment.byteAt(content + full % period)) : -1;
        return new LiteralContext(previous, excluded);
    }
}
