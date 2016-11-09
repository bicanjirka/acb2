package cz.cvut.fit.acb.coding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.utils.Chainable;

public abstract class ByteToTripletConverter<T> implements Chainable<List<byte[]>, TripletProcessor>, TripletProcessor {

    private Consumer<TripletProcessor> consumer;
    private Map<Integer, T> map = new HashMap<>();
    private List<byte[]> bytes;

    @Override
    public void setConsumer(Consumer<TripletProcessor> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(List<byte[]> bytes) {
        this.bytes = bytes;
        consumer.accept(this);
    }

    @Override
    public void set(TripletFieldId fieldId, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int get(TripletFieldId fieldId) {
        T object = ensure(fieldId);
        return decompress(object);
    }

    private T ensure(TripletFieldId fieldId) {
        T val = map.get(fieldId.getIndex());
        if (val == null) {
            val = createNew(fieldId, bytes);
            map.put(fieldId.getIndex(), val);
        }
        return val;
    }

    protected abstract T createNew(TripletFieldId index, List<byte[]> bytes);

    protected abstract int decompress(T object);
}