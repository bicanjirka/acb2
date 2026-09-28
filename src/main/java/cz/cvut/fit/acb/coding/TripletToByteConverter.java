package cz.cvut.fit.acb.coding;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public abstract class TripletToByteConverter<T> implements TripletWriter {
	
	private final Map<Integer, T> map = new HashMap<>();
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
		T object = map.computeIfAbsent(fieldId.getIndex(), k -> createNew(fieldId));
		compress(object, value);
	}
	
	@Override
	public int read(TripletFieldId fieldId) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<byte[]> finish() {
		int fieldCount = map.keySet().stream().max(Integer::compareTo).map(max -> max + 1).orElse(0);
		List<byte[]> ret = new ArrayList<>(fieldCount + 1);
		ret.add(ByteBuffer.allocate(Integer.BYTES).putInt(segmentSize).array());
		for (int i = 0; i < fieldCount; i++) {
			T object = map.get(i);
			byte[] bytes = object != null ? getArray(object) : new byte[0];
			if (bytes != null) {
				ret.add(bytes);
			}
		}
		return ret;
	}
	
	protected abstract byte[] getArray(T object);
	
	protected abstract T createNew(TripletFieldId index);
	
	protected abstract void compress(T object, int value);
}