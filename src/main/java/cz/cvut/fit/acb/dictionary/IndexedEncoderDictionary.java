package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** An {@link EncoderDictionary} over a {@link ContextIndex}, searching a window of ranks around the context. */
public final class IndexedEncoderDictionary extends ContextIndexDictionary implements EncoderDictionary {

    /**
     * {@code index} must order the positions of {@code segment}, which the dictionary reads but never
     * changes, by {@code window.contextDepth()} bytes of context.
     */
    public IndexedEncoderDictionary(ContextIndex index, SegmentBuffer segment, SearchWindow window) {
        super(index, segment, window);
    }

    @Override
    public SearchResult search(int idx) {
        return switch (this.window().rule()) {
            case NEAREST -> this.searchNearest(idx);
            case SMALLEST_WITH_LCP -> this.searchSmallest(idx);
        };
    }

    @Override
    public int contextRank(int idx) {
        return this.contextRankOf(idx);
    }

    @Override
    public int impliedLength(int idx, int distance) {
        try {
            return this.impliedOf(idx, distance);
        } catch (MalformedStreamException e) {
            throw new IllegalArgumentException("Not a distance found by this dictionary: " + distance, e);
        }
    }

    /**
     * Scans the ranks {@code ctx - maxDistance + 1 .. ctx + maxDistance} outward from the context, so
     * the first longest match is the nearest; at equal distance the rank above the context wins, as
     * in ExCom.
     */
    private SearchResult searchNearest(int idx) {
        int ctx = this.contextRankOf(idx);
        int first = this.window().first(ctx);
        int last = this.window().last(ctx, this.index().size());
        int maxLength = this.window().maxLength();
        int bestRank = -1;
        int bestLen = 0;
        if (first <= last) {
            ContextCursor above = this.index().cursorAt(Math.max(ctx, first));
            ContextCursor below = ctx - 1 >= first ? this.index().cursorAt(ctx - 1) : null;
            for (int offset = 0; bestLen < maxLength && (above != null || below != null); offset++) {
                if (above != null && above.rank() == ctx + offset) {
                    int len = this.segment().commonLength(idx, above.position(), maxLength);
                    if (len > bestLen) {
                        bestLen = len;
                        bestRank = above.rank();
                    }
                    above = above.rank() < last && above.moveUp() ? above : null;
                }
                if (below != null && offset > 0) {
                    int len = this.segment().commonLength(idx, below.position(), maxLength);
                    if (len > bestLen) {
                        bestLen = len;
                        bestRank = below.rank();
                    }
                    below = below.rank() > first && below.moveDown() ? below : null;
                }
            }
        }
        return bestLen == 0 ? SearchResult.miss() : SearchResult.hit(ctx - bestRank, bestLen);
    }

    /**
     * Matches every candidate of the window and takes the longest match, the lexicographically
     * smallest content among equals. The length is cut to what a triplet carries beyond the bytes the
     * decoder works out itself.
     */
    private SearchResult searchSmallest(int idx) {
        int ctx = this.contextRankOf(idx);
        int first = this.window().first(ctx);
        int maxLength = this.window().maxLength();
        int[] positions = this.positionsOf(first, this.window().last(ctx, this.index().size()));
        int bestLength = 0;
        int best = -1;
        for (int i = 0; i < positions.length; i++) {
            int length = this.segment().commonLength(idx, positions[i], this.window().matchLimit());
            boolean longer = length > bestLength;
            boolean smaller = length == bestLength && length > 0
                    && this.lcp().compare(idx, positions[i], positions[best]) < 0;
            if (longer || smaller) {
                bestLength = length;
                best = i;
            }
        }
        if (bestLength == 0) {
            return SearchResult.miss();
        }
        int implied = this.lcp().impliedLength(idx, positions[best], positions);
        return SearchResult.hit(ctx - (first + best), Math.min(bestLength, implied + maxLength), implied);
    }
}
