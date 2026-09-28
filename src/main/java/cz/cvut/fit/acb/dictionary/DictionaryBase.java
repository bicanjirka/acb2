package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

public sealed class DictionaryBase implements Dictionary permits DictionaryLCP {

    private final ContextIndex index;
    private final SegmentBuffer segment;
    private final int maxDistance;
    private final int maxLength;

    /** {@code index} must order the positions of {@code segment}, which the dictionary reads but never changes. */
    public DictionaryBase(ContextIndex index, SegmentBuffer segment, int maxDistance, int maxLength) {
        this.index = index;
        this.segment = segment;
        this.maxDistance = maxDistance;
        this.maxLength = maxLength;
    }

    protected final ContextIndex index() {
        return this.index;
    }

    protected final SegmentBuffer segment() {
        return this.segment;
    }

    protected final int maxLength() {
        return this.maxLength;
    }

    @Override
    public int size() {
        return this.index.size();
    }

    @Override
    public DictionaryInfo search(int idx) {
        int ctx = this.searchContext(idx);
        return this.searchContent(ctx, idx);
    }

    @Override
    public DictionaryInfo searchContent(int ctx, int idx) {
        int first = Math.max(0, ctx - this.maxDistance + 1);
        int last = Math.min(this.index.size() - 1, ctx + this.maxDistance);
        return this.searchContent(ctx, idx, first, last);
    }

    /**
     * Scans the ranks {@code first .. last} outward from the context, so the first longest match is
     * the nearest; at equal distance the rank above the context wins, as in ExCom.
     */
    protected DictionaryInfo searchContent(int ctx, int idx, int first, int last) {
        int bestIdx = -1;
        int bestLen = 0;
        if (first <= last) {
            ContextCursor above = this.index.cursorAt(Math.max(ctx, first));
            ContextCursor below = ctx - 1 >= first ? this.index.cursorAt(ctx - 1) : null;
            for (int offset = 0; bestLen < this.maxLength && (above != null || below != null); offset++) {
                if (above != null && above.rank() == ctx + offset) {
                    int len = this.segment.commonLength(idx, above.position(), this.maxLength);
                    if (len > bestLen) {
                        bestLen = len;
                        bestIdx = above.rank();
                    }
                    above = above.rank() < last && above.moveUp() ? above : null;
                }
                if (below != null && offset > 0) {
                    int len = this.segment.commonLength(idx, below.position(), this.maxLength);
                    if (len > bestLen) {
                        bestLen = len;
                        bestIdx = below.rank();
                    }
                    below = below.rank() > first && below.moveDown() ? below : null;
                }
            }
        }
        return new DictionaryInfo(ctx, bestIdx, bestLen);
    }

    @Override
    public int searchContext(int idx) {
        return this.index.rank(idx) - 1;
    }

    @Override
    public void update(int idx, int count) {
        for (int i = 0; i < count; i++) {
            this.index.insert(idx + i);
        }
    }

    @Override
    public int select(int idx) throws MalformedStreamException {
        if (idx < 0 || idx >= this.index.size()) {
            throw new MalformedStreamException("Rank " + idx + " is outside a dictionary of " + this.index.size());
        }
        return this.index.cursorAt(idx).position();
    }
}
