package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.FieldSink;
import cz.cvut.fit.acb.triplets.FieldSource;
import cz.cvut.fit.acb.triplets.TripletFieldId;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Fields written are read back in order, so a layout can be tested without any coding. */
final class FieldQueue implements FieldSink, FieldSource {

    private final Deque<Field> fields = new ArrayDeque<>();
    private final List<Field> written = new ArrayList<>();

    /** Every field written so far, read or not. */
    List<Field> written() {
        return List.copyOf(this.written);
    }

    @Override
    public void write(TripletFieldId field, int value) {
        Field entry = new Field(field.index(), value);
        this.fields.add(entry);
        this.written.add(entry);
    }

    @Override
    public int read(TripletFieldId field) throws MalformedStreamException {
        Field next = this.fields.poll();
        if (next == null) {
            throw new MalformedStreamException("The stream ends inside a triplet");
        }
        if (next.index() != field.index()) {
            throw new IllegalStateException("Field " + field.index() + " read where field " + next.index()
                    + " was written");
        }
        return next.value();
    }

    record Field(int index, int value) {
    }
}
