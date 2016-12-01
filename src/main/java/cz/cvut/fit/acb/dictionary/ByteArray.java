package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;

/**
 * @author jiri.bican
 */
public class ByteArray implements ByteSequence {
	
	private final byte value[];
	
	public ByteArray(byte value[]) {
		this.value = value;
	}
	
	public ByteArray(ByteSequence original) {
		this.value = original.array().clone();
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
	public ByteSequence clone() {
		return new ByteArray(this);
	}
	
	@Override
	public int length() {
		return value.length;
	}
	
	@Override
	public String toString() {
		return new String(value);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ByteArray)) return false;
		ByteArray byteArray = (ByteArray) o;
		return Arrays.equals(value, byteArray.value);
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(value);
	}
}