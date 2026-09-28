package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.dictionary.core.OrderStatisticTree;
import cz.cvut.fit.acb.format.MalformedStreamException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

public sealed class DictionaryBase implements Dictionary permits DictionaryLCP {

    private final OrderStatisticTree<Integer> ost;
    private final ByteSequence seq;
    private final int maxDistance;
    private final int maxLength;

    /** {@code trees} builds the order-statistic tree the dictionary sorts its contexts in. */
    public DictionaryBase(Function<Comparator<Integer>, OrderStatisticTree<Integer>> trees, ByteSequence sequence,
                          int maxDistance, int maxLength) {
        this.seq = sequence;
        this.maxDistance = maxDistance;
        this.maxLength = maxLength;
        this.ost = trees.apply(new ReverseIndexComparator(sequence));
    }

    private DictionaryBase(DictionaryBase dictionary) {
        this.ost = dictionary.ost.clone();
        this.seq = dictionary.seq.clone();
        this.maxDistance = dictionary.maxDistance;
        this.maxLength = dictionary.maxLength;
    }

    @Override
    public Dictionary clone() {
        return new DictionaryBase(this);
    }

    protected final OrderStatisticTree<Integer> ost() {
        return this.ost;
    }

    protected final ByteSequence seq() {
        return this.seq;
    }

    protected final int maxLength() {
        return this.maxLength;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DictionaryBase)) return false;
        DictionaryBase that = (DictionaryBase) o;
        return maxDistance == that.maxDistance &&
                maxLength == that.maxLength &&
                Objects.equals(ost, that.ost) &&
                (Objects.equals(seq, that.seq) ||
                        Arrays.equals(seq.array(0, ost.size()), that.seq.array(0, ost.size())));
    }

    @Override
    public int hashCode() {
        return Objects.hash(ost, seq, maxDistance, maxLength);
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
        int lo = Math.max(0, ctx - maxDistance);
        int hi = Math.min(ost.size() - 1, ctx + maxDistance);
        return searchContent(ctx, idx, lo, hi);
    }

    protected DictionaryInfo searchContent(int ctx, int idx, int lo, int hi) {
        int bestIdx = -1;
        int bestLen = 0;
        for (int i = lo + 1; i <= hi; i++) {
            int cnt = ost.select(i);
            int comLen = 0;
            while (match(idx + comLen, cnt + comLen) && comLen < maxLength) {
                comLen++;
            }
            if (comLen > bestLen) {
                bestLen = comLen;
                bestIdx = i;
                if (bestLen == maxLength) {
                    break;
                }
            }
        }
        return new DictionaryInfo(ctx, bestIdx, bestLen);
    }

    @Override
    public int searchContext(int idx) {
        int rank = ost.rank(idx);
        return rank - 1;
    }

    @Override
    public void update(int idx, int count) {
        for (int i = 0; i < count; i++) {
            int key = idx + i;
            ost.put(key);
        }
    }

    @Override
    public int select(int idx) throws MalformedStreamException {
        if (idx < 0 || idx >= ost.size()) {
            throw new MalformedStreamException("Rank " + idx + " is outside a dictionary of " + ost.size());
        }
        return ost.select(idx);
    }

}
