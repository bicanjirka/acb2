package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** Supplies the fields a layout reads, in the order the sink took them. */
public interface FieldSource {

    /** @throws MalformedStreamException if the stream cannot supply the field */
    int read(TripletFieldId field) throws MalformedStreamException;

    /** Whether {@link #read(TripletFieldId, LiteralContext)} makes use of the context it is given. */
    default boolean wantsLiteralContext() {
        return false;
    }

    /** Reads a literal field given what both sides know of the literal; the context may be ignored. */
    default int read(TripletFieldId field, LiteralContext context) throws MalformedStreamException {
        return this.read(field);
    }
}
