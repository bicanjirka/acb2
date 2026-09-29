package cz.cvut.fit.acb.triplets;

/** Takes the fields a layout writes, in order. */
public interface FieldSink {

    void write(TripletFieldId field, int value);
}
