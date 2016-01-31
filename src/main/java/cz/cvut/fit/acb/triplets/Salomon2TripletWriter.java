package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.OutputStream;

import cz.cvut.fit.acb.utils.TripletUtils;

public class Salomon2TripletWriter extends BaseTripletWriter implements TripletWriter {

	public Salomon2TripletWriter(int distBits, int lengBits, OutputStream out) {
		super(distBits, lengBits, out);
	}

	@Override
	public void write(Triplet t) throws IOException {
		if (print) System.out.println(cnt++ + ") write " + TripletUtils.printSalomon2(t));
		int dist = t.getDistance();
		int leng = t.getLenght();
		char ch = t.getSymbol();
		if (dist == 0 && leng == 0) {
			out.write(0, 1);
			out.write(ch);
		} else {
			out.write(1, 1);
			out.write(dist, distBits);
			out.write(leng, lengBits);
			out.write(ch);
		}
	}
}
