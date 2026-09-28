package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;

import java.util.ArrayList;
import java.util.List;

/** Records the fields an encoder writes, in order. */
final class FieldRecorder implements TripletProcessor {

    private final List<Field> written = new ArrayList<>();

    List<Field> written() {
        return List.copyOf(this.written);
    }

    @Override
    public void write(TripletFieldId fieldId, int value) {
        this.written.add(new Field(fieldId.index(), value));
    }

    @Override
    public int read(TripletFieldId fieldId) {
        throw new UnsupportedOperationException("Encoders only write");
    }

    @Override
    public int getSize() {
        throw new UnsupportedOperationException("Encoders only write");
    }

    @Override
    public void setSize(int size) {
        throw new UnsupportedOperationException("Coders never announce the size");
    }

    record Field(int index, int value) {
    }
}
