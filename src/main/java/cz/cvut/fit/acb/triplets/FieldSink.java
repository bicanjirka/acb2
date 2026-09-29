package cz.cvut.fit.acb.triplets;

/** Takes the fields a layout writes, in order. */
public interface FieldSink {

    void write(TripletFieldId field, int value);

    /** Whether {@link #write(TripletFieldId, int, LiteralContext)} makes use of the context it is given. */
    default boolean wantsLiteralContext() {
        return false;
    }

    /** Writes a literal field with what both sides know of the literal; the context may be ignored. */
    default void write(TripletFieldId field, int value, LiteralContext context) {
        this.write(field, value);
    }
}
