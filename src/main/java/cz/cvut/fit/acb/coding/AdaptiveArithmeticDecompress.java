package cz.cvut.fit.acb.coding;

import nayuki.arithcode.ArithmeticDecoder;
import nayuki.arithcode.BitInputStream;
import nayuki.arithcode.FlatFrequencyTable;
import nayuki.arithcode.FrequencyTable;
import nayuki.arithcode.SimpleFrequencyTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author jiri.bican
 */
class AdaptiveArithmeticDecompress {

    private static final Logger logger = LogManager.getLogger();
    private final FrequencyTable freq;
    private final int eof;
    private ArithmeticDecoder dec;

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

    public AdaptiveArithmeticDecompress(int bitSize, byte[] array, int[] freqVal) {
        try {
            dec = new ArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(array)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        eof = 1 << bitSize; // last symbol is EOF flag
        int numSymbols = eof + 1;
        int[] freq1 = Arrays.copyOf(freqVal, numSymbols);
        if (freqVal.length < numSymbols) {
            Arrays.fill(freq1, freqVal.length, numSymbols, 1);
        }
        freq = new SimpleFrequencyTable(freq1);
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
