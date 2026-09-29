package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.TripletFieldId;

/** Reads the fields {@link RangeTripletWriter} coded, from the block's bytes. */
public final class RangeTripletReader implements FieldSource {

    private final RangeDecoder decoder;
    private final FieldModels models;

    public RangeTripletReader(byte[] block, LengthFrequencies lengthFrequencies) throws MalformedStreamException {
        this.decoder = new RangeDecoder(block);
        this.models = new FieldModels(lengthFrequencies);
    }

    @Override
    public int read(TripletFieldId field) throws MalformedStreamException {
        AdaptiveFrequencyModel model = this.models.of(field);
        int symbol = model.symbolAt(this.decoder.target(model.total()));
        this.decoder.consume(model.cumulative(symbol), model.frequency(symbol));
        model.increment(symbol);
        return symbol;
    }
}
