package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class Dictionary {
	
	private RedBlackBST<Integer> bst;
	private CharSequence seq;
	private int maxDistance;
	private int maxLength;

	public Dictionary(CharSequence sequence, int maxDistance, int maxLength) {
		seq = sequence;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		bst = new RedBlackBST<Integer>(new ReverseIndexComparator(seq));
	}
	
	public Integer searchContext(int idx) {
		int rank = bst.rank(idx);
		return Math.min(rank, bst.size()-1);
	}

	public int[] searchContent(int ctx, int idx) {
		int lo = Math.max(0, ctx - maxDistance);
		int hi = Math.min(bst.size()-1, ctx + maxDistance - 1);
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
			while (matchChar(idx + comLen, cntIdx + comLen) && comLen < maxLength) {
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
		return new int[] {bestIdx, bestLen};
	}

	private boolean matchChar(int i, int j) {
		if (i >= seq.length())
			return false;
		char ch1 = seq.charAt(i);
		char ch2 = seq.charAt(j);
		return ch1 == ch2;
	}

	public void update(int idx, int count) {
		for (int i = 0; i < count; i++) {
			int key = idx + i;
			bst.put(key);
		}
	}

	private final class ReverseIndexComparator implements Comparator<Integer> {
		CharSequence s;
		public ReverseIndexComparator(CharSequence s) {
			this.s = s;
		}
		@Override
		public int compare(Integer o1, Integer o2) {
			int len1 = o1;
	        int len2 = o2;
	        int lim = Math.min(len1, len2);
	
	        int k = 1;
	        while (k <= lim) {
	            char c1 = s.charAt(len1 - k);
	            char c2 = s.charAt(len2 - k);
	            if (c1 != c2) {
	                return c1 - c2;
	            }
	            k++;
	        }
	        return len1 - len2;
		}
	};
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int index = bst.size();
		if (index == 0) return "[]";
		for (Integer key : bst.keys()) {
			int val = key.intValue();
			int rank = bst.rank(key);
			sb.append(rank);
			int indent = bst.size() - val + (String.valueOf(bst.size()).length() - String.valueOf(rank).length());
			char[] ch = new char[indent];
			Arrays.fill(ch, ' ');
			CharSequence ctx = seq.subSequence(0, val);
			CharSequence cnt = seq.subSequence(val, index);
			sb.append(ch).append(ctx).append('|').append(cnt).append('\n');
		}
		return sb.toString();
	}

	public CharSequence copy(int cnt, int leng) {
		StringBuffer sb = new StringBuffer(leng);
		int start = bst.select(cnt);
		while (sb.length() < leng) {
			int end = start + leng - sb.length();
			end = Math.min(seq.length(), end);
			CharSequence tempSeq = seq.subSequence(start, end);
			sb.append(tempSeq);
		}
		return sb;
	}

}