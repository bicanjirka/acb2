package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** A {@link DecoderDictionary} over a {@link ContextIndex}. */
public final class IndexedDecoderDictionary extends ContextIndexDictionary implements DecoderDictionary {

    /**
     * {@code index} must order the positions of {@code segment}, the bytes decoded so far, by
     * {@code window.contextDepth()} bytes of context.
     */
    public IndexedDecoderDictionary(ContextIndex index, SegmentBuffer segment, SearchWindow window) {
        super(index, segment, window);
    }

    @Override
    public int contextRank(int idx) {
        return this.contextRankOf(idx);
    }

    @Override
    public int impliedLength(int idx, int distance) throws MalformedStreamException {
        return this.impliedOf(idx, distance);
    }
}
