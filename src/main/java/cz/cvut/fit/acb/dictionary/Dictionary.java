package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author jiri.bican
 */
public class Dictionary {
	
	protected RedBlackBST<Integer> bst;
	protected ByteSequence seq;
	protected int maxDistance;
	protected int maxLength;
	
	public Dictionary(ByteSequence sequence, int maxDistance, int maxLength) {
		seq = sequence;
		this.maxDistance = maxDistance;
		this.maxLength = maxLength;
		bst = new RedBlackBST<>(new ReverseIndexComparator(seq));
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
	
	protected boolean match(int i, int j) {
		if (i >= seq.length()/* || i >= bst.size()*/) // is this if needed?
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
	
	public DictionaryInfo search(int idx) {
		int ctx = searchContext(idx);
		return searchContent(ctx, idx);
	}
	
	public DictionaryInfo searchContent(int ctx, int idx) {
		int lo = Math.max(0, ctx - maxDistance);
		int hi = Math.min(bst.size() - 1, ctx + maxDistance);
		return searchContent(ctx, idx, lo, hi);
	}
	
	protected DictionaryInfo searchContent(int ctx, int idx, int lo, int hi) {
		int bestIdx = -1;
		int bestLen = 0;
		for (int i = lo; i < hi; i++) {
			int cnt = bst.select(i); // TODO do not select for every node, utilize neighbour links
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
	
	public int searchContext(int idx) {
		int rank = bst.rank(idx);
//		return Math.min(rank, bst.size() - 1);
		return rank - 1;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int index = bst.size();
		if (index == 0)
			return "[]";
		for (Integer key : bst.keys()) {
			int val = key;
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
			bst.put(key); // TODO put all values at once, do less shifts in BST
		}
	}
	
	public int select(int idx) {
		return bst.select(idx);
	}
	
	private static class ReverseIndexComparator implements Comparator<Integer> {
		private static final int MAGIC_CONST = 10;
		ByteSequence s;
		
		public ReverseIndexComparator(ByteSequence s) {
			this.s = s;
		}
		
		@Override
		public int compare(Integer o1, Integer o2) {
			int len1 = o1;
			int len2 = o2;
			int lim = Math.min(len1, len2);
			lim = Math.min(lim, MAGIC_CONST);
			
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
	
}