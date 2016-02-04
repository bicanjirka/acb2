package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;

public class ByteArray implements ByteSequence {

	private final byte value[];

	public ByteArray(byte value[]) {
		this.value = value;
	}

	public ByteArray(ByteSequence original) {
		this.value = original.array();
	}

	@Override
	public byte[] array() {
		return value;
	}

	@Override
	public byte[] array(int start, int end) {
		return Arrays.copyOfRange(value, start, end);
	}

	@Override
	public byte byteAt(int index) {
		return value[index];
	}

	@Override
	public int length() {
		return value.length;
	}
}