package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * The order of the positions of a segment by their contexts, as a {@link ContextIndex} keeps it,
 * and the rank of the context of a position: what the encoder's and the decoder's dictionary share,
 * whatever their rule for the best match.
 */
final class ContextRanking {

    private final ContextIndex index;
    private final SegmentBuffer segment;
    private final SearchWindow window;

    /** {@code index} must order the positions of {@code segment} by {@code window.contextDepth()} bytes of context. */
    ContextRanking(ContextIndex index, SegmentBuffer segment, SearchWindow window) {
        this.index = index;
        this.segment = segment;
        this.window = window;
    }

    ContextIndex index() {
        return this.index;
    }

    SegmentBuffer segment() {
        return this.segment;
    }

    SearchWindow window() {
        return this.window;
    }

    int size() {
        return this.index.size();
    }

    void update(int idx, int count) {
        for (int i = 0; i < count; i++) {
            this.index.insert(idx + i);
        }
    }

    int select(int rank) throws MalformedStreamException {
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
    int contextRankOf(int idx) {
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

    /** What {@link Dictionary#continuations} says over the window around the context of {@code idx}. */
    ByteSet continuations(int idx, int content, int length) {
        ByteSet.Builder continuations = ByteSet.builder();
        int ctx = this.contextRankOf(idx);
        int first = this.window.first(ctx);
        int last = this.window.last(ctx, this.index.size());
        if (first <= last) {
            byte[] bytes = this.segment.bytes();
            ContextCursor cursor = this.index.cursorAt(first);
            for (int rank = first; rank <= last; rank++) {
                int candidate = cursor.position();
                if (continues(bytes, candidate, content, length, idx)) {
                    continuations.add(Byte.toUnsignedInt(bytes[candidate + length]));
                }
                cursor.moveUp();
            }
        }
        return continuations.build();
    }

    private static boolean continues(byte[] bytes, int candidate, int content, int length, int idx) {
        if (candidate + length >= idx) {
            return false;
        }
        int period = idx - content;
        for (int k = 0; k < length; k++) {
            if (bytes[candidate + k] != bytes[content + k % period]) {
                return false;
            }
        }
        return true;
    }

    /** The positions of the ranks {@code first .. last}, in rank order. */
    int[] positionsOf(int first, int last) {
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
}
