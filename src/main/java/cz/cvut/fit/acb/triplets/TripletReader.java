package cz.cvut.fit.acb.triplets;

import java.io.Closeable;
import java.io.IOException;

public interface TripletReader extends Closeable {

	public Triplet read() throws IOException;
}
