package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldId;

import java.util.List;

/**
 * Codes every field of a block into one range-coded stream, each against the adaptive model of its
 * own field, in the order the fields are written.
 */
public final class RangeTripletWriter implements TripletWriter {

    private final RangeEncoder encoder = new RangeEncoder();
    private final FieldModels models;
    private final CostTally costs = new CostTally();

    /** Length fields start from {@code lengthFrequencies}. */
    public RangeTripletWriter(LengthFrequencies lengthFrequencies) {
        this.models = new FieldModels(lengthFrequencies);
    }

    @Override
    public void write(TripletFieldId field, int value) {
        AdaptiveFrequencyModel model = this.models.of(field);
        int frequency = model.frequency(value);
        int total = model.total();
        this.encoder.encode(model.cumulative(value), frequency, total);
        this.costs.add(field.kind(), total, frequency);
        model.increment(value);
    }

    @Override
    public byte[] finish() {
        this.encoder.finish();
        return this.encoder.toArray();
    }

    /** The ideal cost of each field's symbols, which the coded bytes exceed by the coder's few bytes of flush. */
    @Override
    public List<FieldCost> costs() {
        return this.costs.costs();
    }
}
