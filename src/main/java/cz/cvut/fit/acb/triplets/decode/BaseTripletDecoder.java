/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.triplets.decode;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseTripletDecoder implements TripletDecoder {
	
	protected final Logger logger = LogManager.getLogger(getClass());
	protected Dictionary dictionary;
	protected ByteBuilder sequence;

	public BaseTripletDecoder(ByteBuilder sequence, Dictionary dictionary) {
		this.sequence = sequence;
		this.dictionary = dictionary;
	}
}