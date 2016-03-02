package cz.cvut.fit.acb.utils;

public class BitUtils {

	public static boolean isNegative(int val, int offset) {
		return val >> offset - 1 == 1;
	}

	public static int fillHighBits(int val) {
		// http://stackoverflow.com/questions/25688855/how-to-fill-high-end-bits-in-a-java-byte-with-1-without-knowing-the-last-1-in
		return (~Integer.highestOneBit(val) + 1) | val;
	}
	
	public static int log2(int value) {
		return Integer.SIZE - Integer.numberOfLeadingZeros(value);
	}
	
}
