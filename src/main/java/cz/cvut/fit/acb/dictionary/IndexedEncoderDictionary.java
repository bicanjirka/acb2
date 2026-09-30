package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** An {@link EncoderDictionary} over a {@link ContextIndex}, searching a window of ranks around the context. */
public final class IndexedEncoderDictionary implements EncoderDictionary {

    private final ContextRanking ranking;
    private final Matcher matcher;

    /**
     * {@code index} must order the positions of {@code segment}, which the dictionary reads but never
     * changes, by {@code window.contextDepth()} bytes of context.
     */
    public IndexedEncoderDictionary(ContextIndex index, SegmentBuffer segment, SearchWindow window) {
        this.ranking = new ContextRanking(index, segment, window);
        this.matcher = window.rule().over(this.ranking);
    }

    @Override
    public int size() {
        return this.ranking.size();
    }

    @Override
    public void update(int idx, int count) {
        this.ranking.update(idx, count);
    }

    @Override
    public int select(int rank) throws MalformedStreamException {
        return this.ranking.select(rank);
    }

    @Override
    public ByteSet continuations(int idx, int content, int length) {
        return this.ranking.continuations(idx, content, length);
    }

    @Override
    public int contextRank(int idx) {
        return this.ranking.contextRankOf(idx);
    }

    @Override
    public SearchResult search(int idx) {
        return this.matcher.search(idx);
    }

    @Override
    public int impliedLength(int idx, int distance) {
        try {
            return this.matcher.impliedLength(idx, distance);
        } catch (MalformedStreamException e) {
            throw new IllegalArgumentException("Not a distance found by this dictionary: " + distance, e);
        }
    }
}
