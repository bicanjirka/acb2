/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.coding;

import java.util.List;

import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public class AdaptiveArithmeticDecoder extends ByteToTripletConverter<AdaptiveArithmeticDecompress> {
	
	private final int[] lengthFreq = {45, 13, 10, 7, 5, 4};
	
	@Override
	protected AdaptiveArithmeticDecompress createNew(TripletFieldId index, List<byte[]> bytes) {
		return index.isLength() ?
				new AdaptiveArithmeticDecompress(index.getBitSize(), bytes.get(index.getIndex()), lengthFreq) :
				new AdaptiveArithmeticDecompress(index.getBitSize(), bytes.get(index.getIndex()));
	}
	
	@Override
	protected int decompress(AdaptiveArithmeticDecompress object) {
		return object.decompress();
	}
}
