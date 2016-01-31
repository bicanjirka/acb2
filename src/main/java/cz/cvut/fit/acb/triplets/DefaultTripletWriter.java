package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.OutputStream;

public class DefaultTripletWriter extends BaseTripletWriter implements TripletWriter {

	public DefaultTripletWriter(int distBits, int lengBits, OutputStream out) {
		super(distBits, lengBits, out);
	}

	@Override
	public void write(Triplet t) throws IOException {
		if (print) System.out.println(cnt++ + ") write " + t);
		out.write(t.getDistance(), distBits);
		out.write(t.getLenght(), lengBits);
		out.write(t.getSymbol());
	}
}
