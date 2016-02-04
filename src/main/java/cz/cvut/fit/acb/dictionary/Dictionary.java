package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class Dictionary {

	private final class ReverseIndexComparator implements Comparator<Integer> {
		ByteSequence s;

		public ReverseIndexComparator(ByteSequence s) {
			this.s = s;
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			int len1 = o1;
			int len2 = o2;
			int lim = Math.min(len1, len2);

			int k = 1;
			while (k <= lim) {
				byte b1 = s.byteAt(len1 - k);
				byte b2 = s.byteAt(len2 - k);
				if (b1 != b2) {
					return b1 - b2;
				}
				k++;
			}
			return len1 - len2;
		}
	}
	private RedBlackBST<Integer> bst;
	private ByteSequence seq;
	private int maxDistance;
	private int maxLength;

	public Dictionary(ByteSequence sequence, int maxDistance, int maxLength) {
		seq = sequence;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		bst = new RedBlackBST<Integer>(new ReverseIndexComparator(seq));
	}

	public byte[] copy(int cnt, int leng) {
		ByteBuilder bb = new ByteBuilder(leng);
		int start = bst.select(cnt);
		while (bb.length() < leng) {
			int end = start + leng - bb.length();
			end = Math.min(seq.length(), end);
			byte[] arr = seq.array(start, end);
			bb.append(arr);
		}
		return bb.array();
	}

	private boolean match(int i, int j) {
		if (i >= seq.length())
			return false;
		byte b1 = seq.byteAt(i);
		byte b2 = seq.byteAt(j);
		return b1 == b2;
	}

	public int[] searchContent(int ctx, int idx) {
		int lo = Math.max(0, ctx - maxDistance);
		int hi = Math.min(bst.size() - 1, ctx + maxDistance - 1);
		return searchContent(ctx, idx, lo, hi);
	}

	private int[] searchContent(int cnt, int idx, int lo, int hi) {
		int bestIdx = -1;
		int bestLen = 0;
		Integer loKey = bst.select(lo);
		Integer hiKey = bst.select(hi);
		int i = lo;
		for (Iterator<Integer> iterator = bst.keys(loKey, hiKey).iterator(); iterator.hasNext(); i++) {
			Integer key = iterator.next();
			int cntIdx = key;
			int comLen = 0;
			while (match(idx + comLen, cntIdx + comLen) && comLen < maxLength) {
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
		return new int[] { bestIdx, bestLen };
	}

	public Integer searchContext(int idx) {
		int rank = bst.rank(idx);
		return Math.min(rank, bst.size() - 1);
	};

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int index = bst.size();
		if (index == 0)
			return "[]";
		for (Integer key : bst.keys()) {
			int val = key.intValue();
			int rank = bst.rank(key);
			sb.append(rank);
			int indent = bst.size() - val + (String.valueOf(bst.size()).length() - String.valueOf(rank).length());
			char[] ch = new char[indent];
			Arrays.fill(ch, ' ');
			byte[] ctx = seq.array(0, val);
			byte[] cnt = seq.array(val, index);
			sb.append(ch).append(new String(ctx)).append('|').append(new String(cnt)).append('\n');
		}
		return sb.toString();
	}

	public void update(int idx, int count) {
		for (int i = 0; i < count; i++) {
			int key = idx + i;
			bst.put(key);
		}
	}

}