package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.InputStream;

import cz.cvut.fit.acb.utils.BitUtils;
import cz.cvut.fit.acb.utils.TripletUtils;

public class ValachTripletReader extends BaseTripletReader {

	public ValachTripletReader(int distBits, int lengBits, InputStream in) {
		super(distBits, lengBits, in);
	}

	@Override
	public Triplet read() throws IOException {
		
		int length = in.read(lengBits);
		Triplet t;
		
		if (length == 0) {
			int ch = in.read();
			t = eof(length, ch) ? null : new Triplet(0, 0, ch);
		} else {
			int read = in.read(distBits);
			int distance = BitUtils.isNegative(read, distBits) ? BitUtils.negateLeadingZeros(read) : read;
			int ch = in.read();
			t = eof(read, length, ch) ? null : new Triplet(distance, length, ch);
		}
		if (print) System.out.println(cnt++ + ") read " + TripletUtils.printValach(t));
		return t;
	}
}
