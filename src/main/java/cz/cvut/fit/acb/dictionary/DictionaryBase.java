package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;
import java.util.Objects;

import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.dictionary.core.OrderStatisticTree;

/**
 * @author jiri.bican
 */
public class DictionaryBase implements Dictionary {
	
	protected OrderStatisticTree<Integer> ost;
	protected ByteSequence seq;
	protected int maxDistance;
	protected int maxLength;
	
	public DictionaryBase(ACBProvider provider, ByteSequence sequence, int maxDistance, int maxLength) {
		this.seq = sequence;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		this.ost = provider.getOrderStatisticTree(new ReverseIndexComparator(seq));
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
	
	@Override
	public byte[] copy(int cnt, int leng) {
		ByteBuilder bb = new ByteBuilder(leng);
		int start = ost.select(cnt);
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
		if (i >= seq.length()/* || i >= ost.size()*/) // is this if needed?
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
		for (int i = lo; i < hi; i++) {
			int cnt = ost.select(i); // TODO do not select for every node, utilize neighbour links
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
//		return Math.min(rank, ost.size() - 1);
		return rank - 1;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int index = ost.size();
		if (index == 0)
			return "[]";
		for (Integer key : ost.keys()) {
			int val = key;
			int rank = ost.rank(key);
			sb.append(rank);
			int indent = ost.size() - val + (String.valueOf(ost.size()).length() - String.valueOf(rank).length());
			char[] ch = new char[indent];
			Arrays.fill(ch, ' ');
			byte[] ctx = seq.array(0, val);
			byte[] cnt = seq.array(val, index);
			sb.append(ch).append(new String(ctx)).append('|').append(new String(cnt)).append('\n');
		}
		return sb.toString();
	}
	
	@Override
	public void update(int idx, int count) {
		for (int i = 0; i < count; i++) {
			int key = idx + i;
			ost.put(key); // TODO put all values at once, do less shifts in BST
		}
	}
	
	@Override
	public int select(int idx) {
		return ost.select(idx);
	}
	
}