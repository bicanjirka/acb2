package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One adaptive model per triplet field, made when the field is first coded. The encoder and the
 * decoder ask for the same fields in the same order, so they make the same models. Length fields
 * start from the tuned frequencies, every other field flat.
 */
final class FieldModels {

    private final int[] lengthFrequencies;
    private final List<AdaptiveFrequencyModel> models = new ArrayList<>();

    /** Symbols past {@code lengthFrequencies} start at 1. */
    FieldModels(int[] lengthFrequencies) {
        this.lengthFrequencies = lengthFrequencies.clone();
    }

    AdaptiveFrequencyModel of(TripletFieldId field) {
        while (this.models.size() <= field.index()) {
            this.models.add(null);
        }
        AdaptiveFrequencyModel model = this.models.get(field.index());
        if (model == null) {
            model = new AdaptiveFrequencyModel(this.startingFrequencies(field));
            this.models.set(field.index(), model);
        }
        return model;
    }

    private int[] startingFrequencies(TripletFieldId field) {
        int symbols = 1 << field.bitSize();
        int[] frequencies = new int[symbols];
        if (field.isLength()) {
            int given = Math.min(symbols, this.lengthFrequencies.length);
            System.arraycopy(this.lengthFrequencies, 0, frequencies, 0, given);
            Arrays.fill(frequencies, given, symbols, 1);
        } else {
            Arrays.fill(frequencies, 1);
        }
        return frequencies;
    }
}
