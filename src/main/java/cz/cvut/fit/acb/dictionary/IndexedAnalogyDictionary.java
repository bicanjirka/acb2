package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** An {@link AnalogyDictionary} over a {@link ContextIndex}. */
public final class IndexedAnalogyDictionary implements AnalogyDictionary {

    /** A step of at least this many bytes per bit of the dictionary's size is worth one position. */
    private static final int LONG_STEP_FACTOR = 3;

    private final ContextIndex index;
    private final FunnelWalk walk;
    private final Funnel funnel;
    private final Funnel forecast;

    /**
     * {@code index} must order the positions of {@code segment}, the bytes known so far, by
     * {@code depth} bytes of context.
     *
     * @param reach the most candidates a funnel takes from either side of a context
     */
    public IndexedAnalogyDictionary(ContextIndex index, SegmentBuffer segment, int reach, int depth) {
        this.index = index;
        this.walk = new FunnelWalk(index, segment, reach, depth);
        this.funnel = new Funnel(this.walk.capacity());
        this.forecast = new Funnel(this.walk.capacity());
    }

    @Override
    public int size() {
        return this.index.size();
    }

    @Override
    public void update(int idx, int count) {
        boolean longStep = count >= LONG_STEP_FACTOR * Weighting.floorLog2(this.index.size());
        if (longStep) {
            this.index.insert(idx);
            return;
        }
        for (int i = 0; i < count; i++) {
            this.index.insert(idx + i);
        }
    }

    @Override
    public int select(int rank) throws MalformedStreamException {
        if (rank < 0 || rank >= this.index.size()) {
            throw new MalformedStreamException("Rank " + rank + " is outside a dictionary of " + this.index.size());
        }
        return this.index.cursorAt(rank).position();
    }

    @Override
    public Funnel funnel(int position) {
        this.walk.fill(position, Weighting.POSITION, this.funnel);
        return this.funnel;
    }

    @Override
    public Funnel forecast(int position) {
        this.walk.fill(position, Weighting.FORECAST, this.forecast);
        return this.forecast;
    }
}
