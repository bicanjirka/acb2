/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.dictionary.core;

/**
 * @author jiri.bican
 */
public interface OrderStatisticTree<Key> extends BinarySearchTree<Key> {
	
	OrderStatisticTree<Key> clone();
	
	int rank(Key key);
	
	Key select(int k);
	
}
