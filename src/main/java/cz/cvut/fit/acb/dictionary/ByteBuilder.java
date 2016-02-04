package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;

public class ByteBuilder implements ByteSequence {

	private byte[] value;
	private int count;

	public ByteBuilder() {
		this(16);
	}

	public ByteBuilder(byte[] arr) {
		this(arr.length + 16);
		append(arr);
	}

	ByteBuilder(int capacity) {
		value = new byte[capacity];
	}

	public ByteBuilder append(byte b) {
		ensureCapacityInternal(count + 1);
		value[count++] = b;
		return this;
	}

	public ByteBuilder append(byte[] arr) {
		int len = arr.length;
		ensureCapacityInternal(count + len);
		System.arraycopy(arr, 0, value, count, len);
		count += len;
		return this;
	}

	@Override
	public byte[] array() {
		return array(0, count);
	}

	@Override
	public byte[] array(int start, int end) {
		return Arrays.copyOfRange(value, start, end);
	}

	@Override
	public byte byteAt(int index) {
		return value[index];
	}

	public int capacity() {
		return value.length;
	}

	private void ensureCapacityInternal(int minimumCapacity) {
		// overflow-conscious code
		if (minimumCapacity - value.length > 0)
			expandCapacity(minimumCapacity);
	}

	void expandCapacity(int minimumCapacity) {
		int newCapacity = value.length * 2 + 2;
		if (newCapacity - minimumCapacity < 0)
			newCapacity = minimumCapacity;
		if (newCapacity < 0) {
			if (minimumCapacity < 0) // overflow
				throw new OutOfMemoryError();
			newCapacity = Integer.MAX_VALUE;
		}
		value = Arrays.copyOf(value, newCapacity);
	}

	@Override
	public int length() {
		return count;
	}

	@Override
	public String toString() {
		return new String(array());
	}
}
