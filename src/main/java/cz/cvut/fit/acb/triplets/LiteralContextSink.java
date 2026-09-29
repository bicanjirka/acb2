package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** Gives a sink that wants it the context of every literal, worked out from the fields before it. */
final class LiteralContextSink implements FieldSink {

    private final FieldSink delegate;
    private final LiteralTracker tracker;

    LiteralContextSink(FieldSink delegate, LiteralTracker tracker) {
        this.delegate = delegate;
        this.tracker = tracker;
    }


    @Override
    public void write(TripletFieldId field, int value) {
        if (field.kind() == TripletFieldKind.LITERAL) {
            try {
                this.delegate.write(field, value, this.tracker.context());
            } catch (MalformedStreamException e) {
                throw new IllegalStateException("The encoder wrote a match its dictionary cannot resolve", e);
            }
        } else {
            this.tracker.observe(field, value);
            this.delegate.write(field, value);
        }
    }
}
