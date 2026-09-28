package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.dictionary.core.OrderStatisticTree;

import java.util.Comparator;
import java.util.function.Function;

public final class DictionaryLCP extends DictionaryBase {

    public DictionaryLCP(Function<Comparator<Integer>, OrderStatisticTree<Integer>> trees, ByteSequence sequence,
                         int maxDistance, int maxLength) {
        super(trees, sequence, maxDistance, maxLength);
    }

    @Override
    protected DictionaryInfo searchContent(int ctx, int idx, int lo, int hi) {
        int bestIdx = -1;
        int bestLen = 0;
        int lcp = 0; // longest common prefix with second best content
        int lcpIdx = 0; // index of the second best content
        for (int i = lo + 1; i <= hi; i++) {
            int cnt = ost().select(i);
            int comLen = 0; // common length
            while ((cnt + comLen) < ost().size() && match(idx + comLen, cnt + comLen) && comLen < maxLength()) {
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

    @Override
    protected int compare(int i, int j) {
        if (i >= ost().size())
            return Byte.MAX_VALUE;
        if (j >= ost().size())
            return Byte.MIN_VALUE;
        byte b1 = seq().byteAt(i);
        byte b2 = seq().byteAt(j);
        return Byte.compare(b1, b2);
    }

}
