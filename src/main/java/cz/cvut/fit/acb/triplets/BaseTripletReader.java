package cz.cvut.fit.acb.triplets;

import java.io.IOException;
import java.io.InputStream;

public abstract class BaseTripletReader implements TripletReader {

	protected BitStreamInputStream in;
	protected int distBits;
	protected int lengBits;
	protected int cnt = 0;
	protected static boolean print = false;
	
	public BaseTripletReader(int distBits, int lengBits, InputStream in) {
		this.distBits = distBits;
		this.lengBits = lengBits;
		this.in = new BitStreamInputStream(in);
	}

	protected static boolean eof(int... is) {
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