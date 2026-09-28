package cz.cvut.fit.acb.dictionary;

public final class DictionaryLCP extends DictionaryBase {

    public DictionaryLCP(ContextIndex index, SegmentBuffer segment, int maxDistance, int maxLength) {
        super(index, segment, maxDistance, maxLength);
    }

    @Override
    protected DictionaryInfo searchContent(int ctx, int idx, int first, int last) {
        int bestIdx = -1;
        int bestLen = 0;
        int lcp = 0; // longest common prefix with second best content
        int lcpIdx = 0; // index of the second best content
        ContextCursor cursor = first <= last ? index().cursorAt(first) : null;
        for (int i = first; cursor != null && i <= last; i++, cursor = cursor.moveUp() ? cursor : null) {
            int cnt = cursor.position();
            int comLen = 0; // common length
            while ((cnt + comLen) < index().size() && match(idx + comLen, cnt + comLen) && comLen < maxLength()) {
                comLen++;
            }
            if (comLen > lcp) {
                if (comLen > bestLen) { // if longer common prefix found, set old best to 2nd best and update the best one
                    lcp = bestLen;
                    bestLen = comLen;
                    lcpIdx = bestIdx;
                    bestIdx = i;
                } else if (comLen == bestLen) {
                    // update best index to lexicographically lower content with same common length
                    int compare = compare(cnt + comLen, bestIdx + bestLen, 1);
                    if (compare < 0) {
                        bestIdx = cnt;
                    }
                    // else do nothing, first uncommon symbol of actual best content is lower
                } else {
                    lcp = comLen;
                    lcpIdx = cnt;
                }
            }
        }
        return new DictionaryInfo(ctx, bestIdx, Math.min(maxLength(), bestLen - lcp), lcp);
    }

    private int compare(int i, int j, int offset) {
        int cmp = 0;
        while (cmp == 0) {
            offset++;
            cmp = compare(i + offset, j + offset);
        }
        return cmp;
    }

    private int compare(int i, int j) {
        if (i >= index().size())
            return Byte.MAX_VALUE;
        if (j >= index().size())
            return Byte.MIN_VALUE;
        byte b1 = segment().byteAt(i);
        byte b2 = segment().byteAt(j);
        return Byte.compare(b1, b2);
    }

    private boolean match(int i, int j) {
        if (i >= segment().length())
            return false;
        return segment().byteAt(i) == segment().byteAt(j);
    }

}
