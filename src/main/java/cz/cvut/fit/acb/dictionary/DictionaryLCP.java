package cz.cvut.fit.acb.dictionary;

public class DictionaryLCP extends Dictionary {

	public DictionaryLCP(ByteSequence sequence, int maxDistance, int maxLength) {
		super(sequence, maxDistance, maxLength);
	}

	@Override
	protected DictionaryInfo searchContent(int ctx, int idx, int lo, int hi) {
		int bestIdx = -1;
		int bestLen = 0;
		int lcp = 0; // longest common prefix with second best content
		for (int i = lo; i < hi; i++) {
			Integer key = bst.select(i); // TODO get siblings by link, not select which is O(log n)
			int cnt = key;
			int idxPos = idx;
			int cntPos = cnt;
			while (match(idxPos, cntPos)/* && cntPos < bst.size() */) {
				idxPos++;
				cntPos++;
			}
			int comLen = idxPos - idx;
			if (comLen > lcp) {
				// lcp = max lcp min spolecna delka a delka ke konci retezce
				// lcp = Math.max(lcp, Math.min(comLen, Math.min(cntPos, bst.size()) - cnt));
				if (comLen > bestLen) {
					lcp = bestLen;
					bestLen = comLen;
					bestIdx = i;
				} else if (comLen == bestLen) {
					// update best index to lexicographically lowest content with same common length
					bestIdx = compare(bestIdx, i, comLen) < 0 ? bestIdx : i;
				} else {
					lcp = compare(bestIdx, i, comLen) < 0 ? lcp : comLen;
				}
			}/* else if (comLen == lcp) {
				
			}*/
		}
		return new DictionaryInfo(ctx, bestIdx, Math.min(maxLength, bestLen - lcp), lcp);
	}

	private int compare(int i, int j, int offset) {
		int cmp = 0;
		int pos1 = bst.select(i); // TODO do not query, remember from key (select above)
		int pos2 = bst.select(j);
		while (cmp == 0) {
			offset++;
			cmp = compare(pos1 + offset, pos2 + offset);
		}
//		System.out.println("comparing "+new String(seq.array(pos1, pos1+offset))+" ["+cmp+"] "+new String(seq.array(pos2, pos2+offset)));
		return cmp;
	}

	public int searchLcp(int ctx, int idx, int cnt) {
		int lo = Math.max(0, ctx - maxDistance);
		int hi = Math.min(bst.size() - 1, ctx + maxDistance);
		return searchLcp(ctx, idx, cnt, lo, hi);
	}

	private int searchLcp(int ctx, int idx, int cnt2, int lo, int hi) {
		int bestIdx = cnt2;
		int bestLen = 0;
		int lcp = 0; // longest common prefix with second best content
		for (int i = lo; i < hi; i++) {
			Integer key = bst.select(i);
			int cnt = key;
			int idxPos = idx;
			int cntPos = cnt;
			while (match(idxPos, cntPos)/* && cntPos < bst.size() */) {
				idxPos++;
				cntPos++;
			}
			int comLen = idxPos - idx;
			if (comLen > lcp) {
				// lcp = max lcp min spolecna delka a delka ke konci retezce
				// lcp = Math.max(lcp, Math.min(comLen, Math.min(cntPos, bst.size()) - cnt));
				if (comLen > bestLen) {
					lcp = bestLen;
					bestLen = comLen;
					bestIdx = i;
				} else if (comLen == bestLen) {
					// update best index to lexicographically lowest content with same common length
					bestIdx = compare(bestIdx, i, comLen) < 0 ? bestIdx : i;
				} else {
					lcp = compare(bestIdx, i, comLen) < 0 ? lcp : comLen;
				}
			}/* else if (comLen == lcp) {
				
			}*/
		}
		return lcp;
	}
}
