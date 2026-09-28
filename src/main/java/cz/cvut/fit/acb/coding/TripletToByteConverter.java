package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class TripletToByteConverter<T> implements TripletWriter {

    private final List<Field<T>> fields = new ArrayList<>();
    private int segmentSize;

    @Override
    public int getSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSize(int size) {
        segmentSize = size;
    }

    @Override
    public void write(TripletFieldId fieldId, int value) {
        while (fields.size() <= fieldId.index()) {
            fields.add(null);
        }
        Field<T> field = fields.get(fieldId.index());
        if (field == null) {
            field = new Field<>(fieldId.kind(), createNew(fieldId));
            fields.set(fieldId.index(), field);
        }
        field.symbols++;
        compress(field.object, value);
    }

    @Override
    public int read(TripletFieldId fieldId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<byte[]> finish() {
        List<byte[]> ret = new ArrayList<>(fields.size() + 1);
        ret.add(ByteBuffer.allocate(Integer.BYTES).putInt(segmentSize).array());
        for (Field<T> field : fields) {
            byte[] bytes = field != null ? getArray(field.object) : new byte[0];
            if (bytes != null) {
                ret.add(bytes);
            }
        }
        return ret;
    }

    @Override
    public List<FieldCost> costs() {
        return fields.stream()
                .filter(Objects::nonNull)
                .map(field -> new FieldCost(field.kind, field.symbols, bitsOf(field.object)))
                .toList();
    }

    /** Bits the field's symbols took in the finished payload. */
    protected abstract long bitsOf(T object);

    protected abstract byte[] getArray(T object);

    protected abstract T createNew(TripletFieldId index);

    protected abstract void compress(T object, int value);

    private static final class Field<T> {
        private final TripletFieldKind kind;
        private final T object;
        private long symbols;

        private Field(TripletFieldKind kind, T object) {
            this.kind = kind;
            this.object = object;
        }
    }
}
