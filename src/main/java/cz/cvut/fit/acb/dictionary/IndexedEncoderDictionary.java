package cz.cvut.fit.acb.dictionary;

/** An {@link EncoderDictionary} over a {@link ContextIndex}, searching a window of ranks around the context. */
public final class IndexedEncoderDictionary extends ContextIndexDictionary implements EncoderDictionary {

    private final SegmentBuffer segment;
    private final int maxDistance;
    private final int maxLength;

    /** {@code index} must order the positions of {@code segment}, which the dictionary reads but never changes. */
    public IndexedEncoderDictionary(ContextIndex index, SegmentBuffer segment, int maxDistance, int maxLength) {
        super(index);
        this.segment = segment;
        this.maxDistance = maxDistance;
        this.maxLength = maxLength;
    }

    /**
     * Scans the ranks {@code ctx - maxDistance + 1 .. ctx + maxDistance} outward from the context, so
     * the first longest match is the nearest; at equal distance the rank above the context wins, as
     * in ExCom.
     */
    @Override
    public SearchResult search(int idx) {
        int ctx = this.contextRankOf(idx);
        int first = Math.max(0, ctx - this.maxDistance + 1);
        int last = Math.min(this.index().size() - 1, ctx + this.maxDistance);
        int bestRank = -1;
        int bestLen = 0;
        if (first <= last) {
            ContextCursor above = this.index().cursorAt(Math.max(ctx, first));
            ContextCursor below = ctx - 1 >= first ? this.index().cursorAt(ctx - 1) : null;
            for (int offset = 0; bestLen < this.maxLength && (above != null || below != null); offset++) {
                if (above != null && above.rank() == ctx + offset) {
                    int len = this.segment.commonLength(idx, above.position(), this.maxLength);
                    if (len > bestLen) {
                        bestLen = len;
                        bestRank = above.rank();
                    }
                    above = above.rank() < last && above.moveUp() ? above : null;
                }
                if (below != null && offset > 0) {
                    int len = this.segment.commonLength(idx, below.position(), this.maxLength);
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
}
