package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.util.Arrays;
import java.util.List;

/**
 * Codes every field of a block into one range-coded stream, each against the adaptive model of its
 * own field, in the order the fields are written.
 */
public final class RangeTripletWriter implements TripletWriter {

    private static final double LN2 = Math.log(2);

    private final RangeEncoder encoder = new RangeEncoder();
    private final FieldModels models;
    private final long[] symbols = new long[TripletFieldKind.values().length];
    private final double[] bits = new double[TripletFieldKind.values().length];

    /** Length fields start from {@code lengthFrequencies}; symbols past it start at 1. */
    public RangeTripletWriter(int[] lengthFrequencies) {
        this.models = new FieldModels(lengthFrequencies);
    }

    @Override
    public void write(TripletFieldId field, int value) {
        AdaptiveFrequencyModel model = this.models.of(field);
        int frequency = model.frequency(value);
        int total = model.total();
        this.encoder.encode(model.cumulative(value), frequency, total);
        int kind = field.kind().ordinal();
        this.symbols[kind]++;
        this.bits[kind] += Math.log((double) total / frequency) / LN2;
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
        return Arrays.stream(TripletFieldKind.values())
                .filter(kind -> this.symbols[kind.ordinal()] > 0)
                .map(kind -> new FieldCost(kind, this.symbols[kind.ordinal()],
                        Math.round(this.bits[kind.ordinal()])))
                .toList();
    }
}
