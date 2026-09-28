package cz.cvut.fit.acb.coding;

import java.util.Arrays;

/** One field's symbols, range coded against an adaptive model, ended by an end-of-field symbol. */
class AdaptiveArithmeticCompress {

    private final RangeEncoder encoder = new RangeEncoder();
    private final AdaptiveFrequencyModel model;
    private final int endSymbol;

    AdaptiveArithmeticCompress(int bitSize) {
        this(bitSize, new int[0]);
    }

    /** Symbols past {@code startingFrequencies} start at 1. */
    AdaptiveArithmeticCompress(int bitSize, int[] startingFrequencies) {
        this.endSymbol = 1 << bitSize;
        this.model = new AdaptiveFrequencyModel(startingFrequencies(this.endSymbol + 1, startingFrequencies));
    }

    static int[] startingFrequencies(int symbols, int[] given) {
        int[] frequencies = Arrays.copyOf(given, symbols);
        if (given.length < symbols) {
            Arrays.fill(frequencies, given.length, symbols, 1);
        }
        return frequencies;
    }

    void compress(int symbol) {
        this.code(symbol);
        this.model.increment(symbol);
    }

    void terminate() {
        this.code(this.endSymbol);
        this.encoder.finish();
    }

    byte[] array() {
        return this.encoder.toArray();
    }

    private void code(int symbol) {
        this.encoder.encode(this.model.cumulative(symbol), this.model.frequency(symbol), this.model.total());
    }
}
