package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * {@link MatchRule#NEAREST_WITH_PREFIX}: the first longest match on a walk outward from the context,
 * with the common prefix of the best content and the contents walked before it implied.
 */
final class NearestWithPrefixMatcher implements Matcher {

    private final ContextRanking ranking;
    private final LcpMatches lcp;

    NearestWithPrefixMatcher(ContextRanking ranking) {
        this.ranking = ranking;
        this.lcp = new LcpMatches(ranking.segment());
    }

    /**
     * Walks the window as {@link NearestMatcher} does and takes the first longest match, so the best
     * is the one that rule takes. Every content walked before it matched fewer bytes, and shares with
     * it no more than that, so the prefix implied is below the match length. The length is cut to what
     * a triplet carries beyond that prefix.
     */
    @Override
    public SearchResult search(int idx) {
        SearchWindow window = this.ranking.window();
        SegmentBuffer segment = this.ranking.segment();
        int ctx = this.ranking.contextRankOf(idx);
        int first = window.first(ctx);
        int last = window.last(ctx, this.ranking.size());
        int limit = window.matchLimit();
        OutwardWalk walk = OutwardWalk.over(this.ranking.index(), ctx, first, last);
        int bestLength = 0;
        int bestRank = -1;
        int bestPosition = -1;
        while (bestLength < limit && walk.advance()) {
            int length = segment.commonLength(idx, walk.position(), limit);
            if (length > bestLength) {
                bestLength = length;
                bestRank = walk.rank();
                bestPosition = walk.position();
            }
        }
        if (bestLength == 0) {
            return SearchResult.miss();
        }
        int implied = this.prefixBefore(idx, OutwardWalk.over(this.ranking.index(), ctx, first, last), bestRank,
                bestPosition);
        return SearchResult.hit(ctx - bestRank, Math.min(bestLength, implied + window.maxLength()), implied);
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
        return this.prefixBefore(idx, OutwardWalk.over(this.ranking.index(), ctx, first, last), rank,
                this.ranking.select(rank));
    }

    /** The longest common prefix of the content at {@code position} with those the walk passes before {@code rank}. */
    private int prefixBefore(int idx, OutwardWalk walk, int rank, int position) {
        int implied = 0;
        while (walk.advance() && walk.rank() != rank) {
            implied = Math.max(implied, this.lcp.commonPrefix(idx, walk.position(), position));
        }
        return implied;
    }
}
