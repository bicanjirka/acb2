package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.InputStream;

import cz.cvut.fit.acb.utils.BitUtils;

public class DefaultTripletReader extends BaseTripletReader {

	public DefaultTripletReader(int distBits, int lengBits, InputStream in) {
		super(distBits, lengBits, in);
	}

	@Override
	public Triplet read() throws IOException {
		
		int read = in.read(distBits);
		int distance = BitUtils.isNegative(read, distBits) ? BitUtils.negateLeadingZeros(read) : read;
		int length = in.read(lengBits);
		int ch = in.read(); // TODO different encoding

		Triplet t = eof(read, length, ch) ? null : new Triplet(distance, length, (char) ch);
		if (print) System.out.println(cnt++ + ") read " + t);
		return t;
	}
}
