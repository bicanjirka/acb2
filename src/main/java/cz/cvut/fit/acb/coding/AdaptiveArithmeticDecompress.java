package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import nayuki.arithcode.ArithmeticDecoder;
import nayuki.arithcode.BitInputStream;
import nayuki.arithcode.FlatFrequencyTable;
import nayuki.arithcode.FrequencyTable;
import nayuki.arithcode.SimpleFrequencyTable;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class AdaptiveArithmeticDecompress {

    /**
     * Zero bytes the decoder may read past the payload. It runs 32 bits ahead of the encoder, so a
     * sound stream needs 4 or 5; a corrupt one would otherwise decode zeros without end.
     */
    private static final int PADDING = 8;

    private final FrequencyTable freq;
    private final int eof;
    private final ArithmeticDecoder dec;
    private boolean ended;

    public AdaptiveArithmeticDecompress(int bitSize, byte[] array) throws MalformedStreamException {
        this(bitSize, array, new SimpleFrequencyTable(new FlatFrequencyTable((1 << bitSize) + 1)));
    }

    public AdaptiveArithmeticDecompress(int bitSize, byte[] array, int[] freqVal) throws MalformedStreamException {
        this(bitSize, array, new SimpleFrequencyTable(startingFrequencies(bitSize, freqVal)));
    }

    private AdaptiveArithmeticDecompress(int bitSize, byte[] array, FrequencyTable freq)
            throws MalformedStreamException {
        this.freq = freq;
        this.eof = 1 << bitSize; // last symbol is EOF flag
        try {
            this.dec = new ArithmeticDecoder(new BitInputStream(new PaddedInput(array)));
        } catch (IOException e) {
            throw new MalformedStreamException("An arithmetic-coded field ends before it starts");
        }
    }

    /** @return the next symbol, or -1 at the end of the field, and at every read after it */
    public int decompress() throws MalformedStreamException {
        if (this.ended) {
            return -1;
        }
        int symbol;
        try {
            symbol = this.dec.read(this.freq);
        } catch (IOException e) {
            throw new MalformedStreamException("An arithmetic-coded field ends inside its data");
        }
        if (symbol == this.eof) {
            this.ended = true;
            return -1;
        }
        this.freq.increment(symbol);
        return symbol;
    }

    private static int[] startingFrequencies(int bitSize, int[] freqVal) {
        int numSymbols = (1 << bitSize) + 1;
        int[] frequencies = Arrays.copyOf(freqVal, numSymbols);
        if (freqVal.length < numSymbols) {
            Arrays.fill(frequencies, freqVal.length, numSymbols, 1);
        }
        return frequencies;
    }

    /** The payload followed by a few zero bytes, then an {@link EOFException}. */
    private static final class PaddedInput extends InputStream {

        private final byte[] array;
        private int position;

        private PaddedInput(byte[] array) {
            this.array = array;
        }

        @Override
        public int read() throws IOException {
            if (this.position < this.array.length) {
                return this.array[this.position++] & 0xFF;
            }
            if (++this.position > this.array.length + PADDING) {
                throw new EOFException("Read past the end of the payload");
            }
            return 0;
        }
    }
}
