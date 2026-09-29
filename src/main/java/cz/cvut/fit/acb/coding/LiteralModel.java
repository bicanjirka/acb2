package cz.cvut.fit.acb.coding;

import java.util.Arrays;

/**
 * The model of the literals of a block: one adaptive model of all bytes, and one for each byte that
 * can come before a literal. A model for a byte is made when the byte is first seen, from the
 * general model less its weight, so that a young context starts from what is known of all of them.
 */
final class LiteralModel {

    private static final int SYMBOLS = 256;
    /** How much lighter than the general model a new context model starts; a heavier start is slow to specialise. */
    private static final int SEED_DIVISOR = 16;

    private final AdaptiveFrequencyModel general = flat();
    private final AdaptiveFrequencyModel[] byPrevious = new AdaptiveFrequencyModel[SYMBOLS];

    /** The model to code a literal with, given the byte before it or -1 if there is none. */
    AdaptiveFrequencyModel modelFor(int previous) {
        if (previous < 0) {
            return this.general;
        }
        AdaptiveFrequencyModel model = this.byPrevious[previous];
        if (model == null) {
            int[] seed = new int[SYMBOLS];
            for (int symbol = 0; symbol < SYMBOLS; symbol++) {
                seed[symbol] = Math.max(1, this.general.frequency(symbol) / SEED_DIVISOR);
            }
            model = new AdaptiveFrequencyModel(seed);
            this.byPrevious[previous] = model;
        }
        return model;
    }

    /** Counts a literal that was coded with {@link #modelFor(int)}. */
    void update(int previous, int symbol) {
        this.general.increment(symbol);
        if (previous >= 0) {
            this.modelFor(previous).increment(symbol);
        }
    }

    private static AdaptiveFrequencyModel flat() {
        int[] frequencies = new int[SYMBOLS];
        Arrays.fill(frequencies, 1);
        return new AdaptiveFrequencyModel(frequencies);
    }
}
