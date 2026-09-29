package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.LiteralContext;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.util.Arrays;
import java.util.List;

/**
 * Codes the fields of a block into one range-coded stream like {@link RangeTripletWriter}, except
 * that a literal is coded against the model of the byte before it, and never as the byte it cannot be.
 */
public final class ContextRangeTripletWriter implements TripletWriter {

    private static final double LN2 = Math.log(2);

    private final RangeEncoder encoder = new RangeEncoder();
    private final FieldModels models;
    private final LiteralModel literals = new LiteralModel();
    private final long[] symbols = new long[TripletFieldKind.values().length];
    private final double[] bits = new double[TripletFieldKind.values().length];

    public ContextRangeTripletWriter(LengthFrequencies lengthFrequencies) {
        this.models = new FieldModels(lengthFrequencies);
    }

    @Override
    public boolean wantsLiteralContext() {
        return true;
    }

    @Override
    public void write(TripletFieldId field, int value) {
        this.write(field, value, LiteralContext.none());
    }

    @Override
    public void write(TripletFieldId field, int value, LiteralContext context) {
        if (field.kind() == TripletFieldKind.LITERAL) {
            if (value == context.excluded()) {
                throw new IllegalArgumentException("A literal cannot be the byte it excludes: " + value);
            }
            AdaptiveFrequencyModel model = this.literals.modelFor(context.previous());
            int total = model.totalWithout(context.excluded());
            int frequency = model.frequency(value);
            this.encoder.encode(model.cumulativeWithout(value, context.excluded()), frequency, total);
            this.count(field, total, frequency);
            this.literals.update(context.previous(), value);
        } else {
            AdaptiveFrequencyModel model = this.models.of(field);
            int frequency = model.frequency(value);
            int total = model.total();
            this.encoder.encode(model.cumulative(value), frequency, total);
            this.count(field, total, frequency);
            model.increment(value);
        }
    }

    private void count(TripletFieldId field, int total, int frequency) {
        int kind = field.kind().ordinal();
        this.symbols[kind]++;
        this.bits[kind] += Math.log((double) total / frequency) / LN2;
    }

    @Override
    public byte[] finish() {
        this.encoder.finish();
        return this.encoder.toArray();
    }

    /** The ideal cost of each field, which the coded bytes exceed by the few bytes of flush. */
    @Override
    public List<FieldCost> costs() {
        return Arrays.stream(TripletFieldKind.values())
                .filter(kind -> this.symbols[kind.ordinal()] > 0)
                .map(kind -> new FieldCost(kind, this.symbols[kind.ordinal()],
                        Math.round(this.bits[kind.ordinal()])))
                .toList();
    }
}
