/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.triplets.decode;

import java.io.Closeable;
import java.io.IOException;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;

public abstract class BaseTripletDecoder implements TripletDecoder {

	protected Dictionary dictionary;
	protected ByteBuilder sequence;
	protected int idx = 0;
	private Closeable closeable;

	public BaseTripletDecoder(ByteBuilder sequence, Dictionary dictionary) {
		this.sequence = sequence;
		this.dictionary = dictionary;
	}
	
	protected void setCloseable(Closeable closeable) {
		this.closeable = closeable;
	}

	@Override
	public void close() throws IOException {
		if (closeable != null) {
			closeable.close();
		}
	}

}