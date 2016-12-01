package cz.cvut.fit.acb.coding;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.utils.Chainable;

/**
 * @author jiri.bican
 */
public abstract class ByteToTripletConverter<T> implements Chainable<List<byte[]>, TripletProcessor>, TripletProcessor {
	
	private Consumer<TripletProcessor> consumer;
	private Map<Integer, T> map = new HashMap<>();
	private List<byte[]> bytes;
	private int segmentSize;
	
	@Override
	public void setConsumer(Consumer<TripletProcessor> consumer) {
		this.consumer = consumer;
	}
	
	@Override
	public void accept(List<byte[]> bytes) {
		segmentSize = ByteBuffer.wrap(bytes.get(0)).getInt();
		this.bytes = bytes.subList(1, bytes.size());
		consumer.accept(this);
	}
	
	@Override
	public void write(TripletFieldId fieldId, int value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int read(TripletFieldId fieldId) {
		T object = map.computeIfAbsent(fieldId.getIndex(), k -> createNew(fieldId, bytes));
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