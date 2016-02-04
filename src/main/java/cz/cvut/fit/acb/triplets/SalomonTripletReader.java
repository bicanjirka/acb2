package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.InputStream;

import cz.cvut.fit.acb.utils.BitUtils;
import cz.cvut.fit.acb.utils.TripletUtils;

public class SalomonTripletReader extends BaseTripletReader {

	public SalomonTripletReader(int distBits, int lengBits, InputStream in) {
		super(distBits, lengBits, in);
	}

	@Override
	public Triplet read() throws IOException {
		
		int flag = in.read(1);
		Triplet t;
		
		if (flag == 0) {
			int ch = in.read();
			t = eof(flag, ch) ? null : new Triplet(0, 0, ch);
		} else {
			int read = in.read(distBits);
			int distance = BitUtils.isNegative(read, distBits) ? BitUtils.negateLeadingZeros(read) : read;
			int length = in.read(lengBits);
			t = eof(flag, read, length) ? null : new Triplet(distance, length, -1);
		}
		if (print) System.out.println(cnt++ + ") read " + TripletUtils.printSalomon(t));
		return t;
	}
}
