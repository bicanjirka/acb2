package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

public sealed class DictionaryBase implements Dictionary permits DictionaryLCP {

    private final ContextIndex index;
    private final ByteSequence seq;
    private final int maxDistance;
    private final int maxLength;

    /** {@code index} must order the positions of {@code sequence}, which the dictionary reads but never changes. */
    public DictionaryBase(ContextIndex index, ByteSequence sequence, int maxDistance, int maxLength) {
        this.index = index;
        this.seq = sequence;
        this.maxDistance = maxDistance;
        this.maxLength = maxLength;
    }

    protected final ContextIndex index() {
        return this.index;
    }

    protected final ByteSequence seq() {
        return this.seq;
    }

    protected final int maxLength() {
        return this.maxLength;
    }

    @Override
    public int size() {
        return this.index.size();
    }

    @Override
    public byte[] copy(int cnt, int leng) throws MalformedStreamException {
        if (leng == 0) {
            return new byte[0];
        }
        ByteBuilder bb = new ByteBuilder(leng);
        int start = select(cnt);
        if (start >= seq.length()) {
            throw new MalformedStreamException("Content at position " + start + " has nothing to copy");
        }
        while (bb.length() < leng) {
            int end = start + leng - bb.length();
            end = Math.min(seq.length(), end);
            byte[] arr = seq.array(start, end);
            bb.append(arr);
        }
        return bb.array();
    }

    protected boolean match(int i, int j) {
        if (i >= seq.length())
            return false;
        byte b1 = seq.byteAt(i);
        byte b2 = seq.byteAt(j);
        return b1 == b2;
    }

    protected int compare(int i, int j) {
        if (i >= seq.length())
            return Byte.MAX_VALUE;
        if (j >= seq.length())
            return Byte.MIN_VALUE;
        byte b1 = seq.byteAt(i);
        byte b2 = seq.byteAt(j);
        return Byte.compare(b1, b2);
    }

    @Override
    public DictionaryInfo search(int idx) {
        int ctx = searchContext(idx);
        return searchContent(ctx, idx);
    }

    @Override
    public DictionaryInfo searchContent(int ctx, int idx) {
        int first = Math.max(0, ctx - maxDistance + 1);
        int last = Math.min(index.size() - 1, ctx + maxDistance);
        return searchContent(ctx, idx, first, last);
    }

    /**
     * Scans the ranks {@code first .. last} outward from the context, so the first longest match is
     * the nearest; at equal distance the rank above the context wins, as in ExCom.
     */
    protected DictionaryInfo searchContent(int ctx, int idx, int first, int last) {
        int bestIdx = -1;
        int bestLen = 0;
        if (first <= last) {
            ContextCursor above = index.cursorAt(Math.max(ctx, first));
            ContextCursor below = ctx - 1 >= first ? index.cursorAt(ctx - 1) : null;
            for (int offset = 0; bestLen < maxLength && (above != null || below != null); offset++) {
                if (above != null && above.rank() == ctx + offset) {
                    int len = matchLength(idx, above.position());
                    if (len > bestLen) {
                        bestLen = len;
                        bestIdx = above.rank();
                    }
                    above = above.rank() < last && above.moveUp() ? above : null;
                }
                if (below != null && offset > 0) {
                    int len = matchLength(idx, below.position());
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

    private int matchLength(int idx, int cnt) {
        int length = 0;
        while (match(idx + length, cnt + length) && length < maxLength) {
            length++;
        }
        return length;
    }

    @Override
    public int searchContext(int idx) {
        return index.rank(idx) - 1;
    }

    @Override
    public void update(int idx, int count) {
        for (int i = 0; i < count; i++) {
            index.insert(idx + i);
        }
    }

    @Override
    public int select(int idx) throws MalformedStreamException {
        if (idx < 0 || idx >= index.size()) {
            throw new MalformedStreamException("Rank " + idx + " is outside a dictionary of " + index.size());
        }
        return index.cursorAt(idx).position();
    }

}
