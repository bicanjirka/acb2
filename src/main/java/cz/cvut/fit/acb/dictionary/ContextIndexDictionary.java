package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** The part of a dictionary the two sides share, over a {@link ContextIndex}. */
abstract sealed class ContextIndexDictionary implements Dictionary
        permits IndexedEncoderDictionary, IndexedDecoderDictionary {

    private final ContextIndex index;
    private final SegmentBuffer segment;
    private final SearchWindow window;
    private final LcpMatches lcp;

    /** {@code index} must order the positions of {@code segment} by {@code window.contextDepth()} bytes of context. */
    ContextIndexDictionary(ContextIndex index, SegmentBuffer segment, SearchWindow window) {
        this.index = index;
        this.segment = segment;
        this.window = window;
        this.lcp = new LcpMatches(segment);
    }

    protected final ContextIndex index() {
        return this.index;
    }

    protected final SegmentBuffer segment() {
        return this.segment;
    }

    protected final SearchWindow window() {
        return this.window;
    }

    protected final LcpMatches lcp() {
        return this.lcp;
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
        int depth = this.window.contextDepth();
        int predecessorAgreement = this.segment.commonSuffixLength(idx, cursor.position(), depth);
        cursor.moveUp();
        int successorAgreement = this.segment.commonSuffixLength(idx, cursor.position(), depth);
        return successorAgreement > predecessorAgreement ? successor : successor - 1;
    }

    /** The positions of the ranks {@code first .. last}, in rank order. */
    protected final int[] positionsOf(int first, int last) {
        int[] positions = new int[Math.max(0, last - first + 1)];
        if (positions.length > 0) {
            ContextCursor cursor = this.index.cursorAt(first);
            positions[0] = cursor.position();
            for (int i = 1; i < positions.length; i++) {
                cursor.moveUp();
                positions[i] = cursor.position();
            }
        }
        return positions;
    }

    /**
     * How many bytes of the length of a match {@code distance} ranks from the context of {@code idx}
     * the dictionary implies, by the window's rule.
     *
     * @throws MalformedStreamException if the distance does not name an entry of the window
     */
    protected final int impliedOf(int idx, int distance) throws MalformedStreamException {
        if (this.window.rule() == MatchRule.NEAREST) {
            return 0;
        }
        int ctx = this.contextRankOf(idx);
        int rank = ctx - distance;
        int first = this.window.first(ctx);
        int last = this.window.last(ctx, this.index.size());
        if (rank < first || rank > last) {
            throw new MalformedStreamException("Rank " + rank + " is outside the window of ranks " + first + " to "
                    + last);
        }
        return this.lcp.impliedLength(idx, this.select(rank), this.positionsOf(first, last));
    }
}
