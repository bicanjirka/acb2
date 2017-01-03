/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.dictionary;

import java.util.Comparator;

/**
 * @author jiri.bican
 */
public interface Dictionary {
	
	Dictionary clone();
	
	byte[] copy(int cnt, int leng);
	
	DictionaryInfo search(int idx);
	
	DictionaryInfo searchContent(int ctx, int idx);
	
	int searchContext(int idx);
	
	void update(int idx, int count);
	
	int select(int idx);
	
	class ReverseIndexComparator implements Comparator<Integer> {
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
