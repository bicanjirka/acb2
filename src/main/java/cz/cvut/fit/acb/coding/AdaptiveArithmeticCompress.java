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

import nayuki.arithcode.ArithmeticEncoder;
import nayuki.arithcode.BitOutputStream;
import nayuki.arithcode.FlatFrequencyTable;
import nayuki.arithcode.FrequencyTable;
import nayuki.arithcode.SimpleFrequencyTable;

/**
 * @author jiri.bican
 */
class AdaptiveArithmeticCompress {
	
	private final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
	private final BitOutputStream bitOut = new BitOutputStream(byteOut);
	private final ArithmeticEncoder enc = new ArithmeticEncoder(bitOut);
	private final FrequencyTable freq;
	private final int eof;
	
	public AdaptiveArithmeticCompress(int bitSize) {
		eof = 1 << bitSize; // last symbol is EOF flag
		int numSymbols = eof + 1;
		// Initialize with all symbol frequencies at 1
		freq = new SimpleFrequencyTable(new FlatFrequencyTable(numSymbols));
	}
	
	public void compress(int b) {
		try {
			enc.write(freq, b);
		} catch (IOException e) {
			e.printStackTrace();
		}
		freq.increment(b);
	}
	
	public void terminate() {
		try {
			enc.write(freq, eof);
			enc.finish();
			bitOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] array() {
		return byteOut.toByteArray();
	}
}
