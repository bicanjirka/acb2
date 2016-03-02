package cz.cvut.fit.acb.triplets.decode;

import java.io.Closeable;
import java.io.IOException;

public interface TripletDecoder extends Closeable {
	boolean proccess() throws IOException;
}
