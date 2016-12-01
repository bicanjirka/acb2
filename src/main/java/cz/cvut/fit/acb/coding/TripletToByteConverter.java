package cz.cvut.fit.acb.coding;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.Chainable;

/**
 * @author jiri.bican
 */
public abstract class TripletToByteConverter<T> implements Chainable<TripletSupplier, List<byte[]>>, TripletProcessor {
	
	private Consumer<List<byte[]>> consumer;
	private Map<Integer, T> map = new HashMap<>();
	private int segmentSize;
	
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
	public int getSize() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setSize(int size) {
		segmentSize = size;
	}
	
	@Override
	public void write(TripletFieldId fieldId, int value) {
		T object = map.computeIfAbsent(fieldId.getIndex(), k -> createNew(fieldId));
		compress(object, value);
	}
	
	@Override
	public int read(TripletFieldId fieldId) {
		throw new UnsupportedOperationException();
	}
	
	protected void terminate() {
		List<byte[]> ret = new ArrayList<>(map.size() + 1);
		ret.add(ByteBuffer.allocate(Integer.BYTES).putInt(segmentSize).array());
		for (T object : map.values()) {
			byte[] bytes = getArray(object);
			if (bytes != null) {
				ret.add(bytes);
			}
		}
		consumer.accept(ret);
	}
	
	protected abstract byte[] getArray(T object);
	
	protected abstract T createNew(TripletFieldId index);
	
	protected abstract void compress(T object, int value);
}