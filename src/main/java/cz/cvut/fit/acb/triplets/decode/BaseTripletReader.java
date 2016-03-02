package cz.cvut.fit.acb.triplets.decode;

import java.io.IOException;
import java.io.InputStream;

import cz.cvut.fit.acb.ACB;
import cz.cvut.fit.acb.triplets.BitStreamInputStream;

public abstract class BaseTripletReader<T> implements TripletReader<T> {

	protected BitStreamInputStream in;
	protected int distBits;
	protected int lengBits;
	protected int cnt = 0;
	protected static boolean print = ACB.print_trip;
	
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