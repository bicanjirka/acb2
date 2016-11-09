/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.coding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import cz.cvut.fit.acb.triplets.BitStreamOutputStream;
import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public class BitArrayComposer extends TripletToByteConverter<BitArrayComposer.BitArrayComposerInner> {
	
	ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
	BitStreamOutputStream bitOutputStream = new BitStreamOutputStream(arrayOutputStream);
	private boolean doReturn = false;
	
	@Override
	protected byte[] getArray(BitArrayComposerInner object) {
		byte[] bytes = null;
		if (doReturn) {
			bytes = arrayOutputStream.toByteArray();
			doReturn = false;
		}
		return bytes;
	}
	
	@Override
	protected BitArrayComposerInner createNew(TripletFieldId index) {
		return new BitArrayComposerInner(bitOutputStream, index.getBitSize());
	}
	
	@Override
	protected void compress(BitArrayComposerInner object, int value) {
		try {
			object.write(value);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void terminate() {
		try {
			bitOutputStream.flush();
			doReturn = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.terminate();
	}
	
	public static class BitArrayComposerInner {
		private BitStreamOutputStream outputStream;
		private int bitSize;
		
		public BitArrayComposerInner(BitStreamOutputStream outputStream, int bitSize) {
			this.outputStream = outputStream;
			this.bitSize = bitSize;
		}
		
		public void write(int value) throws IOException {
			outputStream.write(value, bitSize);
		}
	}
}
