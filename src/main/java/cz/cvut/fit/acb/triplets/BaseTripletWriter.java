package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.OutputStream;

public abstract class BaseTripletWriter implements TripletWriter {

	protected BitStreamOutputStream out;
	protected int distBits;
	protected int lengBits;
	protected int cnt = 0;
	protected static boolean print = false;
	
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