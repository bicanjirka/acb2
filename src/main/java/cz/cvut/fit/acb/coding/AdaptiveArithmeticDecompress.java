/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.coding;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import nayuki.arithcode.ArithmeticDecoder;
import nayuki.arithcode.BitInputStream;
import nayuki.arithcode.FlatFrequencyTable;
import nayuki.arithcode.FrequencyTable;
import nayuki.arithcode.SimpleFrequencyTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jiri.bican
 */
class AdaptiveArithmeticDecompress {

    private static final Logger logger = LogManager.getLogger();
    private ArithmeticDecoder dec;
    private final FrequencyTable freq;
    private final int eof;

    public AdaptiveArithmeticDecompress(int bitSize, byte[] array) {
        try {
            dec = new ArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(array)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        eof = 1 << bitSize; // last symbol is EOF flag
        int numSymbols = eof + 1;
        // Initialize with all symbol frequencies at 1
        freq = new SimpleFrequencyTable(new FlatFrequencyTable(numSymbols));
    }

    public int decompress() {
        int symbol = 0;
        try {
            symbol = dec.read(freq);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (symbol == eof)
            return -1;
        freq.increment(symbol);
        return symbol;
    }
}
