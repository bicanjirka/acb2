package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** The part of a dictionary the two sides share, over a {@link ContextIndex}. */
abstract sealed class ContextIndexDictionary implements Dictionary
        permits IndexedEncoderDictionary, IndexedDecoderDictionary {

    private final ContextIndex index;
    private final SegmentBuffer segment;
    private final int contextDepth;

    /** {@code index} must order the positions of {@code segment}, by at most {@code contextDepth} bytes of context. */
    ContextIndexDictionary(ContextIndex index, SegmentBuffer segment, int contextDepth) {
        this.index = index;
        this.segment = segment;
        this.contextDepth = contextDepth;
    }

    protected final ContextIndex index() {
        return this.index;
    }

    @Override
    public final int size() {
        return this.index.size();
    }

    @Override
    public final void update(int idx, int count) {
        for (int i = 0; i < count; i++) {
            this.index.insert(idx + i);
        }
    }

    @Override
    public final int select(int rank) throws MalformedStreamException {
        if (rank < 0 || rank >= this.index.size()) {
            throw new MalformedStreamException("Rank " + rank + " is outside a dictionary of " + this.index.size());
        }
        return this.index.cursorAt(rank).position();
    }

    /**
     * The rank every distance is counted from: of the two entries that {@code idx} would sort
     * between, the one whose context agrees longer with the context of {@code idx}, the one below
     * on a tie; -1 if the dictionary is empty. Reads only bytes before {@code idx}, so both sides
     * choose alike.
     */
    protected final int contextRankOf(int idx) {
        int successor = this.index.rank(idx);
        if (successor == this.index.size()) {
            return successor - 1;
        }
        if (successor == 0) {
            return 0;
        }
        ContextCursor cursor = this.index.cursorAt(successor - 1);
        int predecessorAgreement = this.segment.commonSuffixLength(idx, cursor.position(), this.contextDepth);
        cursor.moveUp();
        int successorAgreement = this.segment.commonSuffixLength(idx, cursor.position(), this.contextDepth);
        return successorAgreement > predecessorAgreement ? successor : successor - 1;
    }
}
