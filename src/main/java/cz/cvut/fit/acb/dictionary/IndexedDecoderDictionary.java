package cz.cvut.fit.acb.dictionary;

/** A {@link DecoderDictionary} over a {@link ContextIndex}. */
public final class IndexedDecoderDictionary extends ContextIndexDictionary implements DecoderDictionary {

    /**
     * {@code index} must order the positions of {@code segment}, the bytes decoded so far, by
     * {@code contextDepth} bytes of context.
     */
    public IndexedDecoderDictionary(ContextIndex index, SegmentBuffer segment, int contextDepth) {
        super(index, segment, contextDepth);
    }

    @Override
    public int contextRank(int idx) {
        return this.contextRankOf(idx);
    }
}
