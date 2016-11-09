package cz.cvut.fit.acb.triplets.encode;

import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletSupplier;

public interface TripletEncoder {
    void process(Consumer<TripletSupplier> output);
}
