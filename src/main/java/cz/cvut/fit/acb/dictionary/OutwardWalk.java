package cz.cvut.fit.acb.dictionary;

/**
 * The ranks of a window in the order {@link NearestMatcher} scans them: the context's own rank, then
 * the rank above it before the one below it at each distance, {@code ctx, ctx+1, ctx-1, ctx+2, ctx-2,
 * ...}, until the window runs out on both sides. It moves cursors of the index and makes no array, so
 * an encoder and a decoder can walk it on every step. Valid only until the index is next changed.
 */
final class OutwardWalk {

    private final int first;
    private final int last;
    private ContextCursor above;
    private ContextCursor below;
    private boolean aboveNext = true;
    private boolean started;
    private int rank;
    private int position;

    private OutwardWalk(int first, int last, ContextCursor above, ContextCursor below) {
        this.first = first;
        this.last = last;
        this.above = above;
        this.below = below;
    }

    /** The walk over the ranks {@code first .. last} outward from {@code ctx}; it has none if the window is empty. */
    static OutwardWalk over(ContextIndex index, int ctx, int first, int last) {
        if (first > last) {
            return new OutwardWalk(first, last, ContextCursor.none(), ContextCursor.none());
        }
        ContextCursor below = ctx - 1 >= first ? index.cursorAt(ctx - 1) : ContextCursor.none();
        return new OutwardWalk(first, last, index.cursorAt(Math.max(ctx, first)), below);
    }

    /** @return whether there was a next rank; {@link #rank()} and {@link #position()} then name it */
    boolean advance() {
        boolean fromAbove = this.aboveNext ? this.above.isPresent() : !this.below.isPresent();
        ContextCursor cursor = fromAbove ? this.above : this.below;
        if (!cursor.isPresent()) {
            return false;
        }
        this.rank = cursor.rank();
        this.position = cursor.position();
        if (fromAbove) {
            this.above = this.rank < this.last && cursor.moveUp() ? cursor : ContextCursor.none();
        } else {
            this.below = this.rank > this.first && cursor.moveDown() ? cursor : ContextCursor.none();
        }
        this.aboveNext = !fromAbove || !this.started;
        this.started = true;
        return true;
    }

    int rank() {
        return this.rank;
    }

    int position() {
        return this.position;
    }
}
