package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * {@link MatchRule#SMALLEST_WITH_LCP}: the longest match, the lexicographically smallest content
 * among equals, with the common prefix of the best content and those below it implied.
 */
final class SmallestWithLcpMatcher implements Matcher {

    private final ContextRanking ranking;
    private final LcpMatches lcp;

    SmallestWithLcpMatcher(ContextRanking ranking) {
        this.ranking = ranking;
        this.lcp = new LcpMatches(ranking.segment());
    }

    /**
     * Matches every candidate of the window and takes the longest match, the lexicographically
     * smallest content among equals. The length is cut to what a triplet carries beyond the bytes the
     * decoder works out itself.
     */
    @Override
    public SearchResult search(int idx) {
        SearchWindow window = this.ranking.window();
        SegmentBuffer segment = this.ranking.segment();
        int ctx = this.ranking.contextRankOf(idx);
        int first = window.first(ctx);
        int maxLength = window.maxLength();
        int[] positions = this.ranking.positionsOf(first, window.last(ctx, this.ranking.size()));
        int bestLength = 0;
        int best = -1;
        for (int i = 0; i < positions.length; i++) {
            int length = segment.commonLength(idx, positions[i], window.matchLimit());
            boolean longer = length > bestLength;
            boolean smaller = length == bestLength && length > 0
                    && this.lcp.compare(idx, positions[i], positions[best]) < 0;
            if (longer || smaller) {
                bestLength = length;
                best = i;
            }
        }
        if (bestLength == 0) {
            return SearchResult.miss();
        }
        int implied = this.lcp.impliedLength(idx, positions[best], positions);
        return SearchResult.hit(ctx - (first + best), Math.min(bestLength, implied + maxLength), implied);
    }

    @Override
    public int impliedLength(int idx, int distance) throws MalformedStreamException {
        SearchWindow window = this.ranking.window();
        int ctx = this.ranking.contextRankOf(idx);
        int rank = ctx - distance;
        int first = window.first(ctx);
        int last = window.last(ctx, this.ranking.size());
        if (rank < first || rank > last) {
            throw new MalformedStreamException("Rank " + rank + " is outside the window of ranks " + first + " to "
                    + last);
        }
        return this.lcp.impliedLength(idx, this.ranking.select(rank), this.ranking.positionsOf(first, last));
    }
}
