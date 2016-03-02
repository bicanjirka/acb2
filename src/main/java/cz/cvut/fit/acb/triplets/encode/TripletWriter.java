package cz.cvut.fit.acb.triplets.encode;

import java.io.Closeable;
import java.io.IOException;

import cz.cvut.fit.acb.triplets.Triplet;

public interface TripletWriter extends Closeable {
	public void write(Triplet t) throws IOException;
}
