package cz.cvut.fit.acb;

import java.util.Comparator;

import cz.cvut.fit.acb.coding.ByteToTripletConverter;
import cz.cvut.fit.acb.coding.TripletToByteConverter;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.core.OrderStatisticTree;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;

/**
 * @author jiri.bican
 */
public interface ACBProvider {
	Dictionary getDictionary(ByteSequence sequence);
	
	TripletCoder getCoder(ByteSequence sequence, Dictionary dictionary);
	
	TripletToByteConverter<?> getT2BConverter();
	
	ByteToTripletConverter<?> getB2TConverter();
	
	<T> OrderStatisticTree<T> getOrderStatisticTree(Comparator<T> comparator);
}
