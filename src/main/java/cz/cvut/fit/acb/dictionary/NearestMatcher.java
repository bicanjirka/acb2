package cz.cvut.fit.acb.dictionary;

/** {@link MatchRule#NEAREST}: the first longest match on a walk outward from the context; no length is implied. */
final class NearestMatcher implements Matcher {

    private final ContextRanking ranking;

    NearestMatcher(ContextRanking ranking) {
        this.ranking = ranking;
    }

    /**
     * Scans the ranks {@code ctx - maxDistance + 1 .. ctx + maxDistance} outward from the context, so
     * the first longest match is the nearest; at equal distance the rank above the context wins, as
     * in ExCom.
     */
    @Override
    public SearchResult search(int idx) {
        SearchWindow window = this.ranking.window();
        ContextIndex index = this.ranking.index();
        SegmentBuffer segment = this.ranking.segment();
        int ctx = this.ranking.contextRankOf(idx);
        int first = window.first(ctx);
        int last = window.last(ctx, index.size());
        int maxLength = window.maxLength();
        int bestRank = -1;
        int bestLen = 0;
        if (first <= last) {
            ContextCursor above = index.cursorAt(Math.max(ctx, first));
            ContextCursor below = ctx - 1 >= first ? index.cursorAt(ctx - 1) : ContextCursor.none();
            for (int offset = 0; bestLen < maxLength && (above.isPresent() || below.isPresent()); offset++) {
                if (above.isPresent() && above.rank() == ctx + offset) {
                    int len = segment.commonLength(idx, above.position(), maxLength);
                    if (len > bestLen) {
                        bestLen = len;
                        bestRank = above.rank();
                    }
                    above = above.rank() < last && above.moveUp() ? above : ContextCursor.none();
                }
                if (below.isPresent() && offset > 0) {
                    int len = segment.commonLength(idx, below.position(), maxLength);
                    if (len > bestLen) {
                        bestLen = len;
                        bestRank = below.rank();
                    }
                    below = below.rank() > first && below.moveDown() ? below : ContextCursor.none();
                }
            }
        }
        return bestLen == 0 ? SearchResult.miss() : SearchResult.hit(ctx - bestRank, bestLen);
    }

    @Override
    public int impliedLength(int idx, int distance) {
        return 0;
    }
}
