package cz.cvut.fit.acb.triplets;

import java.io.Closeable;
import java.io.IOException;

public interface TripletWriter extends Closeable {
	
	public void write(Triplet t) throws IOException;
}
