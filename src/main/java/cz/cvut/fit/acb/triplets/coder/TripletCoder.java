package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;

import java.util.function.Consumer;

public sealed interface TripletCoder permits BaseTripletCoder {
    void encode(Consumer<TripletSupplier> output);

    DecodeFlag decode(TripletProcessor input) throws MalformedStreamException;

    enum DecodeFlag {
        EOF,
        END_OF_PARTITION
    }
}
