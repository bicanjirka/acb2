package cz.cvut.fit.acb.coding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.Chainable;

public abstract class TripletToByteConverter<T> implements Chainable<TripletSupplier, List<byte[]>>, TripletProcessor {

    private Consumer<List<byte[]>> consumer;
    private Map<Integer, T> map = new HashMap<>();

    @Override
    public void setConsumer(Consumer<List<byte[]>> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(TripletSupplier triplet) {
        if (triplet == null) {
            terminate();
            return;
        }
        triplet.visit(this);
    }

    @Override
    public void set(TripletFieldId fieldId, int value) {
        T object = ensure(fieldId);
        compress(object, value);
    }

    @Override
    public int get(TripletFieldId fieldId) {
        throw new UnsupportedOperationException();
    }

    protected void terminate() {
        List<byte[]> ret = new ArrayList<>(map.size());
        for (T object : map.values()) {
            byte[] bytes = getArray(object);
            if (bytes != null) {
                ret.add(bytes);
            }
        }
        consumer.accept(ret);
    }

    private T ensure(TripletFieldId fieldId) {
        T val = map.get(fieldId.getIndex());
        if (val == null) {
            val = createNew(fieldId);
            map.put(fieldId.getIndex(), val);
        }
        return val;
    }

    protected abstract byte[] getArray(T object);

    protected abstract T createNew(TripletFieldId index);

    protected abstract void compress(T object, int value);
}