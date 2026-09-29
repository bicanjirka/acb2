package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * How one coder lays a {@link Triplet} out as fields, and back. Pure: it holds no stream state, and
 * reading what was written gives the triplet back, except that a distance means nothing on a
 * literal and is dropped.
 */
public interface TripletLayout {

    void write(Triplet triplet, FieldSink sink);

    /** @throws MalformedStreamException if the source cannot supply the fields of a triplet */
    Triplet read(FieldSource source) throws MalformedStreamException;
}
