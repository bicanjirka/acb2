package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.LiteralContext;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.util.List;

/**
 * Codes the fields of a block into one range-coded stream like {@link RangeTripletWriter}, except
 * that a literal is coded against the model of the byte before it, and never as the byte it cannot be.
 */
public final class ContextRangeTripletWriter implements TripletWriter {

    private final RangeEncoder encoder = new RangeEncoder();
    private final FieldModels models;
    private final LiteralModel literals = new LiteralModel();
    private final CostTally costs = new CostTally();

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
            if (context.excluded().contains(value)) {
                throw new IllegalArgumentException("A literal cannot be a byte it excludes: " + value);
            }
            AdaptiveFrequencyModel model = this.literals.modelFor(context.previous());
            int total = model.totalWithout(context.excluded());
            int frequency = model.frequency(value);
            this.encoder.encode(model.cumulativeWithout(value, context.excluded()), frequency, total);
            this.costs.add(field.kind(), total, frequency);
            this.literals.update(context.previous(), value);
        } else {
            AdaptiveFrequencyModel model = this.models.of(field);
            int frequency = model.frequency(value);
            int total = model.total();
            this.encoder.encode(model.cumulative(value), frequency, total);
            this.costs.add(field.kind(), total, frequency);
            model.increment(value);
        }
    }

    @Override
    public byte[] finish() {
        this.encoder.finish();
        return this.encoder.toArray();
    }

    /** The ideal cost of each field, which the coded bytes exceed by the few bytes of flush. */
    @Override
    public List<FieldCost> costs() {
        return this.costs.costs();
    }
}
