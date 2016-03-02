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
			Integer key = bst.select(i);
			int cnt = key.intValue();
			int idxPos = idx;
			int cntPos = cnt;
			while (match(idxPos, cntPos)/* && cntPos < bst.size()*/) { // problem, pri zakodovavani mi skonci match, protoze prvni argument, index vstupniho retezce, dobehne na konec. Ale pri rozkodovavani nevim kde je konec a tak najdu lcp mnohem delsi.
				idxPos++;
				cntPos++;
			}
			int comLen = idxPos - idx;
			if (comLen > lcp) {
				if (comLen > bestLen) {
					lcp = Math.max(lcp, Math.min(comLen, Math.min(cntPos, bst.size()) - cnt));
					bestLen = comLen;
					bestIdx = i;
					/*if (bestLen == maxLength) {
						break;
					}*/
				} else {
					lcp = Math.max(lcp, Math.min(comLen, Math.min(cntPos, bst.size()) - cnt));
				}
			}
		}
		return new DictionaryInfo(ctx, bestIdx, Math.min(maxLength, bestLen - lcp), lcp);
	}
}
