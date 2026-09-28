package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** Reads one field written by {@link AdaptiveArithmeticCompress}. */
class AdaptiveArithmeticDecompress {

    private final RangeDecoder decoder;
    private final AdaptiveFrequencyModel model;
    private final int endSymbol;
    private boolean ended;

    AdaptiveArithmeticDecompress(int bitSize, byte[] array) throws MalformedStreamException {
        this(bitSize, array, new int[0]);
    }

    AdaptiveArithmeticDecompress(int bitSize, byte[] array, int[] startingFrequencies)
            throws MalformedStreamException {
        this.endSymbol = 1 << bitSize;
        this.model = new AdaptiveFrequencyModel(
                AdaptiveArithmeticCompress.startingFrequencies(this.endSymbol + 1, startingFrequencies));
        this.decoder = new RangeDecoder(array);
    }

    /** @return the next symbol, or -1 at the end of the field, and at every read after it */
    int decompress() throws MalformedStreamException {
        if (this.ended) {
            return -1;
        }
        int symbol = this.model.symbolAt(this.decoder.target(this.model.total()));
        this.decoder.consume(this.model.cumulative(symbol), this.model.frequency(symbol));
        if (symbol == this.endSymbol) {
            this.ended = true;
            return -1;
        }
        this.model.increment(symbol);
        return symbol;
    }
}
