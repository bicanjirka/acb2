package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** The part of a dictionary the two sides share, over a {@link ContextIndex}. */
abstract sealed class ContextIndexDictionary implements Dictionary
        permits IndexedEncoderDictionary, IndexedDecoderDictionary {

    private final ContextIndex index;

    ContextIndexDictionary(ContextIndex index) {
        this.index = index;
    }

    protected final ContextIndex index() {
        return this.index;
    }

    @Override
    public final int size() {
        return this.index.size();
    }

    @Override
    public final void update(int idx, int count) {
        for (int i = 0; i < count; i++) {
            this.index.insert(idx + i);
        }
    }

    @Override
    public final int select(int rank) throws MalformedStreamException {
        if (rank < 0 || rank >= this.index.size()) {
            throw new MalformedStreamException("Rank " + rank + " is outside a dictionary of " + this.index.size());
        }
        return this.index.cursorAt(rank).position();
    }

    /** The rank of the entry just below {@code idx}: the context every distance is counted from. */
    protected final int contextRankOf(int idx) {
        return this.index.rank(idx) - 1;
    }
}
