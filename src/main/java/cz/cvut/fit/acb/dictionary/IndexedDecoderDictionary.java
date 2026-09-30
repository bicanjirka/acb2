package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** A {@link DecoderDictionary} over a {@link ContextIndex}. */
public final class IndexedDecoderDictionary implements DecoderDictionary {

    private final ContextRanking ranking;
    private final Matcher matcher;

    /**
     * {@code index} must order the positions of {@code segment}, the bytes decoded so far, by
     * {@code window.contextDepth()} bytes of context.
     */
    public IndexedDecoderDictionary(ContextIndex index, SegmentBuffer segment, SearchWindow window) {
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
    public int impliedLength(int idx, int distance) throws MalformedStreamException {
        return this.matcher.impliedLength(idx, distance);
    }
}
