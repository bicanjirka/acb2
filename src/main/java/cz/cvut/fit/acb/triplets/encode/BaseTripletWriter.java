package cz.cvut.fit.acb.triplets.encode;

import java.io.IOException;
import java.io.OutputStream;

import cz.cvut.fit.acb.ACB;
import cz.cvut.fit.acb.triplets.BitStreamOutputStream;

public abstract class BaseTripletWriter implements TripletWriter {

	protected BitStreamOutputStream out;
	protected int distBits;
	protected int lengBits;
	protected int cnt = 0;
	protected static boolean print = ACB.print_trip;
	
	public BaseTripletWriter(int distBits, int lengBits, OutputStream out) {
		this.distBits = distBits;
		this.lengBits = lengBits;
		this.out = new BitStreamOutputStream(out);
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

}