package cz.cvut.fit.acb.dictionary;

/** A {@link DecoderDictionary} over a {@link ContextIndex}. */
public final class IndexedDecoderDictionary extends ContextIndexDictionary implements DecoderDictionary {

    /** {@code index} must order the positions of the segment being decoded. */
    public IndexedDecoderDictionary(ContextIndex index) {
        super(index);
    }

    @Override
    public int contextRank(int idx) {
        return this.contextRankOf(idx);
    }
}
