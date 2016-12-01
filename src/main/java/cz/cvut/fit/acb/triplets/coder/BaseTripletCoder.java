/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.triplets.coder;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.BitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jiri.bican
 */
public abstract class BaseTripletCoder implements TripletCoder {
	
	private static int tCount = 0;
	protected final Logger logger = LogManager.getLogger(getClass());
	protected final Dictionary dictionary;
	protected final ByteSequence sequence;
	protected final int distanceMask;
	protected final IntUnaryOperator distFunc;
	
	public BaseTripletCoder(ByteSequence sequence, Dictionary dictionary, int distanceBits) {
		this.sequence = sequence;
		this.dictionary = dictionary;
		this.distanceMask = (1 << distanceBits) - 1;
		this.distFunc = value -> BitUtils.isNegative(value, distanceBits) ? BitUtils.fillHighBits(value) : value;
	}
	
	@Override
	public void encode(Consumer<TripletSupplier> output) {
		int idx = 0, cnt = 0;
		int ceiling = sequence.length();
		
		while (idx < ceiling) {
			DictionaryInfo info = dictionary.search(idx);
			idx = encodeStep(idx, info, output);
			cnt++;
			tCount++;
		}
		logger.info("Count of triplets in partition: " + cnt);
		// TODO log total count of triplets tCount
	}
	
	protected abstract int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output);
	
	@Override
	public DecodeFlag decode(TripletProcessor input) {
		int idx = 0, cnt = 0;
		int ceiling = input.getSize();
		
		while (idx < ceiling) {
			idx = decodeStep(idx, input);
			cnt++;
			tCount++;
		}
		if (idx == ceiling) {
			logger.info("Count of triplets in partition: " + cnt);
			return DecodeFlag.END_OF_PARTITION;
		} else {
			assert idx == Integer.MAX_VALUE;
			logger.info("Count of triplets in partition: " + --cnt);
			logger.info("Total count of triplets: " + --tCount);
			return DecodeFlag.EOF;
		}
	}
	
	protected abstract int decodeStep(int idx, TripletProcessor input);
}