package cz.cvut.fit.acb.dictionary.core;

/**
 * @author jiri.bican
 */
public interface OrderStatisticTree<Key> extends BinarySearchTree<Key> {
	
	OrderStatisticTree<Key> clone();
	
	int rank(Key key);
	
	Key select(int k);
	
}
