package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.dictionary.ByteSet;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.LiteralContext;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

/** Reads the fields {@link ContextRangeTripletWriter} coded, from the block's bytes. */
public final class ContextRangeTripletReader implements FieldSource {

    private final RangeDecoder decoder;
    private final FieldModels models;
    private final LiteralModel literals = new LiteralModel();

    public ContextRangeTripletReader(byte[] block, LengthFrequencies lengthFrequencies)
            throws MalformedStreamException {
        this.decoder = new RangeDecoder(block);
        this.models = new FieldModels(lengthFrequencies);
    }

    @Override
    public boolean wantsLiteralContext() {
        return true;
    }

    @Override
    public int read(TripletFieldId field) throws MalformedStreamException {
        return this.read(field, LiteralContext.none());
    }

    @Override
    public int read(TripletFieldId field, LiteralContext context) throws MalformedStreamException {
        if (field.kind() != TripletFieldKind.LITERAL) {
            AdaptiveFrequencyModel model = this.models.of(field);
            int symbol = model.symbolAt(this.decoder.target(model.total()));
            this.decoder.consume(model.cumulative(symbol), model.frequency(symbol));
            model.increment(symbol);
            return symbol;
        }
        AdaptiveFrequencyModel model = this.literals.modelFor(context.previous());
        ByteSet excluded = context.excluded();
        int symbol = model.symbolAtWithout(this.decoder.target(model.totalWithout(excluded)), excluded);
        this.decoder.consume(model.cumulativeWithout(symbol, excluded), model.frequency(symbol));
        this.literals.update(context.previous(), symbol);
        return symbol;
    }
}
