package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.InputStream;

import cz.cvut.fit.acb.utils.BitUtils;

public class DefaultTripletReader implements TripletReader {

	private BitStreamInputStream in;
	private int distBits;
	private int lengBits;
//	private int debugCnt = 0;

	public DefaultTripletReader(int distBits, int lengBits, InputStream in) {
		this.distBits = distBits;
		this.lengBits = lengBits;
		this.in = new BitStreamInputStream(in);
	}

	@Override
	public Triplet read() throws IOException {
		
		int read = in.read(distBits);
		int distance = BitUtils.isNegative(read, distBits) ? BitUtils.negateLeadingZeros(read) : read;
		int length = in.read(lengBits);
		int ch = in.read(); // TODO different encoding

		Triplet t = eof(read, length, ch) ? null : new Triplet(distance, length, (char) ch);
//		System.out.println(debugCnt++ + ") read " + t);
		return t;
	}

	private static boolean eof(int... is) {
		for (int i : is) {
			if (i == -1) return true;
		}
		return false;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
