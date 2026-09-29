package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** Gives a source that wants it the context of every literal, worked out from the fields before it. */
final class LiteralContextSource implements FieldSource {

    private final FieldSource delegate;
    private final LiteralTracker tracker;

    LiteralContextSource(FieldSource delegate, LiteralTracker tracker) {
        this.delegate = delegate;
        this.tracker = tracker;
    }


    @Override
    public int read(TripletFieldId field) throws MalformedStreamException {
        if (field.kind() == TripletFieldKind.LITERAL) {
            return this.delegate.read(field, this.tracker.context());
        }
        int value = this.delegate.read(field);
        this.tracker.observe(field, value);
        return value;
    }
}
