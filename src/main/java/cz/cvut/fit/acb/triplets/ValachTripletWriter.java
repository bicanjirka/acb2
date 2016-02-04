package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.OutputStream;

import cz.cvut.fit.acb.utils.TripletUtils;

public class ValachTripletWriter extends BaseTripletWriter implements TripletWriter {

	public ValachTripletWriter(int distBits, int lengBits, OutputStream out) {
		super(distBits, lengBits, out);
	}

	@Override
	public void write(Triplet t) throws IOException {
		if (print) System.out.println(cnt++ + ") write " + TripletUtils.printValach(t));
		int dist = t.getDistance();
		int leng = t.getLenght();
		byte ch = t.getSymbol();
		out.write(leng, lengBits);
		if (leng != 0) {
			out.write(dist, distBits);
		}
		out.write(ch);
	}
}
