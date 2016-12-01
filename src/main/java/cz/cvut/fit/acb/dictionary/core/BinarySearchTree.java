/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.dictionary.core;

import java.io.Serializable;
import java.util.Iterator;

/**
 * @author jiri.bican
 */
public interface BinarySearchTree<Key> extends Iterable<Key>, Serializable {
	Key ceiling(Key key);
	
	boolean contains(Key key);
	
	void delete(Key key);
	
	void deleteMax();
	
	void deleteMin();
	
	Key floor(Key key);
	
	int height();
	
	boolean isEmpty();
	
	default Iterator<Key> iterator() {
		return keys().iterator();
	}
	
	Iterable<Key> keys();
	
	Iterable<Key> keys(Key lo, Key hi);
	
	Key max();
	
	Key min();
	
	void put(Key key);
	
	int size();
	
	int size(Key lo, Key hi);
}
