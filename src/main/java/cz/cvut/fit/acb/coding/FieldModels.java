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

    private final LengthFrequencies lengthFrequencies;
    private final List<AdaptiveFrequencyModel> models = new ArrayList<>();

    FieldModels(LengthFrequencies lengthFrequencies) {
        this.lengthFrequencies = lengthFrequencies;
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
        if (field.isLength()) {
            return this.lengthFrequencies.startingTable(symbols);
        }
        int[] flat = new int[symbols];
        Arrays.fill(flat, 1);
        return flat;
    }
}
