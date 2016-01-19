package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.OutputStream;

public class DefaultTripletWriter implements TripletWriter {

	private BitStreamOutputStream out;
	private int distBits;
	private int lengBits;
//	private int debugCnt = 0;

	public DefaultTripletWriter(int distBits, int lengBits, OutputStream out) {
		this.distBits = distBits;
		this.lengBits = lengBits;
		this.out = new BitStreamOutputStream(out);
	}

	@Override
	public void write(Triplet t) throws IOException {
//		System.out.println(debugCnt++ + ") write " + t);
		out.write(t.getDistance(), distBits);
		out.write(t.getLenght(), lengBits);
		out.write(t.getSymbol());
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

}
