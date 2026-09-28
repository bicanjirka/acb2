package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jiri.bican
 */
public abstract class ByteToTripletConverter<T> implements TripletProcessor {

    private final Map<Integer, T> map = new HashMap<>();
    private List<byte[]> bytes;
    private int segmentSize;

    /** Reads the payload a {@link TripletWriter} finished with; returns this reader. */
    public TripletProcessor open(List<byte[]> bytes) {
        segmentSize = ByteBuffer.wrap(bytes.get(0)).getInt();
        this.bytes = bytes.subList(1, bytes.size());
        return this;
    }

    @Override
    public void write(TripletFieldId fieldId, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int read(TripletFieldId fieldId) {
        T object = map.computeIfAbsent(fieldId.index(), k -> createNew(fieldId, bytes));
        return decompress(object);
    }

    @Override
    public int getSize() {
        return segmentSize;
    }

    @Override
    public void setSize(int size) {
        throw new UnsupportedOperationException();
    }

    protected abstract T createNew(TripletFieldId index, List<byte[]> bytes);

    protected abstract int decompress(T object);
}
