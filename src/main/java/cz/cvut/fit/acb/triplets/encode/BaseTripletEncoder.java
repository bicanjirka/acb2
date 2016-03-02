/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.triplets.encode;

import java.io.IOException;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;

public abstract class BaseTripletEncoder implements TripletEncoder {

	protected Dictionary dictionary;
	protected TripletWriter writer;
	protected ByteSequence sequence;

	public BaseTripletEncoder(ByteSequence sequence, Dictionary dictionary) {
		this.sequence = sequence;
		this.dictionary = dictionary;
	}
	
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
		}
	}

}