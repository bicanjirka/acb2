package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** Supplies the fields a layout reads, in the order the sink took them. */
public interface FieldSource {

    /** @throws MalformedStreamException if the stream cannot supply the field */
    int read(TripletFieldId field) throws MalformedStreamException;
}
