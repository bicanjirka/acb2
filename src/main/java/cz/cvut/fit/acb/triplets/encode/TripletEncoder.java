package cz.cvut.fit.acb.triplets.encode;

import java.io.Closeable;
import java.io.IOException;

import cz.cvut.fit.acb.dictionary.DictionaryInfo;

public interface TripletEncoder extends Closeable {
	int proccess(int idx, DictionaryInfo info) throws IOException;
}
