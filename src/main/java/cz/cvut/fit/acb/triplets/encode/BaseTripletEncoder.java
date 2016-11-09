/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.triplets.encode;

import java.util.function.Consumer;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseTripletEncoder implements TripletEncoder {
	
	protected final Logger logger = LogManager.getLogger(getClass());
	protected static int cnt = 0;
	protected Dictionary dictionary;
	protected ByteSequence sequence;

	public BaseTripletEncoder(ByteSequence sequence, Dictionary dictionary) {
		this.sequence = sequence;
		this.dictionary = dictionary;
	}
	
	@Override
	public void process(Consumer<TripletSupplier> output) {
		int idx = cnt = 0;
		int ceiling = sequence.length();

		while (idx < ceiling) {
			DictionaryInfo info = dictionary.search(idx);
//			if (ACB.print_dict) ACB.print(idx, sequence, info.getContext(), info.getContent(), info.getLength(), dictionary);
			idx = step(idx, info, output);
			cnt++;
		}
		logger.info("Total count of triplet outputted: "+cnt);
		output.accept(null); // to indicate end
	}

	protected abstract int step(int idx, DictionaryInfo info, Consumer<TripletSupplier> output);
}