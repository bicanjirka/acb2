package cz.cvut.fit.acb.triplets.decode;

import java.io.Closeable;
import java.io.IOException;

public interface TripletReader<T> extends Closeable {
	public T read() throws IOException;
}
