/******************************************************************************
 *  Copyright 2002-2016, Robert Sedgewick and Kevin Wayne.
 *
 *  This file is part of algs4.jar, which accompanies the textbook
 *
 *      Algorithms, 4th edition by Robert Sedgewick and Kevin Wayne,
 *      Addison-Wesley Professional, 2011, ISBN 0-321-57351-X.
 *      http://algs4.cs.princeton.edu
 *
 *
 *  algs4.jar is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  algs4.jar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with algs4.jar.  If not, see http://www.gnu.org/licenses.
 ******************************************************************************/

package cz.cvut.fit.acb.dictionary.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The {@code BST} class represents an ordered symbol table of generic
 * key-value pairs.
 * It supports the usual <em>put</em>, <em>read</em>, <em>contains</em>,
 * <em>delete</em>, <em>size</em>, and <em>is-empty</em> methods.
 * It also provides ordered methods for finding the <em>minimum</em>,
 * <em>maximum</em>, <em>floor</em>, <em>select</em>, and <em>ceiling</em>.
 * It also provides a <em>keys</em> method for iterating over all of the keys.
 * A symbol table implements the <em>associative array</em> abstraction:
 * when associating a value with a key that is already in the symbol table,
 * the convention is to replace the old value with the new value.
 * Unlike {@link java.util.Map}, this class uses the convention that
 * values cannot be {@code null}—setting the
 * value associated with a key to {@code null} is equivalent to deleting the key
 * from the symbol table.
 * <p>
 * This implementation uses a sorted array. It requires that
 * the key type implements the {@code Comparable} interface and calls the
 * {@code compareTo()} and method to compare two keys. It does not call either
 * {@code equals()} or {@code hashCode()}.
 * The <em>put</em> and <em>remove</em> operations each take linear time in
 * the worst case; the <em>contains</em>, <em>ceiling</em>, <em>floor</em>,
 * and <em>rank</em> operations take logarithmic time; the <em>size</em>,
 * <em>is-empty</em>, <em>minimum</em>, <em>maximum</em>, and <em>select</em>
 * operations take constant time. Construction takes constant time.
 * <p>
 * For additional documentation, see <a href="http://algs4.cs.princeton.edu/31elementary">Section 3.1</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 * For other implementations, see {@link ST}, {@link BST},
 * {@link SequentialSearchST}, {@link RedBlackBST},
 * {@link SeparateChainingHashST}, and {@link LinearProbingHashST},
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class BinarySearchST<Key> implements OrderStatisticTree<Key> {
	private static final int INIT_CAPACITY = 2;
	private final Comparator<Key> cmpt;
	private Key[] keys;
	//	private Value[] vals;
	private int n = 0;
	
	/**
	 * Initializes an empty symbol table.
	 *
	 * @param cmpt
	 */
	public BinarySearchST(Comparator<Key> cmpt) {
		this(INIT_CAPACITY, cmpt);
	}
	
	/**
	 * Initializes an empty symbol table with the specified initial capacity.
	 *
	 * @param capacity the maximum capacity
	 * @param cmpt
	 */
	public BinarySearchST(int capacity, Comparator<Key> cmpt) {
		keys = (Key[]) new Object[capacity];
//		vals = (Value[]) new Object[capacity];
		this.cmpt = cmpt;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BinarySearchST)) return false;
		BinarySearchST<?> that = (BinarySearchST<?>) o;
		return n == that.n &&
				Arrays.equals(keys, that.keys);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(keys, n);
	}
	
	// resize the underlying arrays
	private void resize(int capacity) {
		assert capacity >= n;
		Key[] tempk = (Key[]) new Object[capacity];
//		Value[] tempv = (Value[]) new Object[capacity];
		for (int i = 0; i < n; i++) {
			tempk[i] = keys[i];
//			tempv[i] = vals[i];
		}
//		vals = tempv;
		keys = tempk;
	}
	
	/**
	 * Returns the number of key-value pairs in this symbol table.
	 *
	 * @return the number of key-value pairs in this symbol table
	 */
	public int size() {
		return n;
	}
	
	/**
	 * Returns true if this symbol table is empty.
	 *
	 * @return {@code true} if this symbol table is empty;
	 * {@code false} otherwise
	 */
	public boolean isEmpty() {
		return size() == 0;
	}
	
	
	/**
	 * Does this symbol table contain the given key?
	 *
	 * @param key the key
	 * @return {@code true} if this symbol table contains {@code key} and
	 * {@code false} otherwise
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public boolean contains(Key key) {
		if (key == null) throw new IllegalArgumentException("argument to contains() is null");
		return get(key) != null;
	}
	
	/**
	 * Returns the value associated with the given key in this symbol table.
	 *
	 * @param key the key
	 * @return the value associated with the given key if the key is in the symbol table
	 * and {@code null} if the key is not in the symbol table
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	private Object get(Key key) {
		if (key == null) throw new IllegalArgumentException("argument to read() is null");
		if (isEmpty()) return null;
		int i = rank(key);
		if (i < n && cmpt.compare(keys[i], key) == 0) return this/*vals[i]*/;
		return null;
	}
	
	@Override
	public OrderStatisticTree<Key> clone() {
		BinarySearchST<Key> bsst = new BinarySearchST<>(keys.length, cmpt);
		System.arraycopy(keys, 0, bsst.keys, 0, keys.length);
		assert equals(bsst);
		return bsst;
	}
	
	/**
	 * Returns the number of keys in this symbol table strictly less than {@code key}.
	 *
	 * @param key the key
	 * @return the number of keys in the symbol table strictly less than {@code key}
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public int rank(Key key) {
		if (key == null) throw new IllegalArgumentException("argument to rank() is null");
		
		int lo = 0, hi = n - 1;
		while (lo <= hi) {
			int mid = lo + (hi - lo) / 2;
			int cmp = cmpt.compare(key, keys[mid]);
			if (cmp < 0) hi = mid - 1;
			else if (cmp > 0) lo = mid + 1;
			else return mid;
		}
		return lo;
	}
	
	
	/**
	 * Removes the specified key and its associated value from this symbol table
	 * (if the key is in this symbol table).
	 *
	 * @param key the key
	 * @param val the value
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	@Override
	public void put(Key key/*, Value val*/) {
		if (key == null) throw new IllegalArgumentException("first argument to put() is null");

//		if (val == null) {
//			delete(key);
//			return;
//		}
		
		int i = rank(key);
		
		// key is already in table
		if (i < n && cmpt.compare(keys[i], key) == 0) {
//			vals[i] = val;
			return;
		}
		
		// insert new key-value pair
		if (n == keys.length) resize(2 * keys.length);
		
		for (int j = n; j > i; j--) {
			keys[j] = keys[j - 1];
//			vals[j] = vals[j-1];
		}
		keys[i] = key;
//		vals[i] = val;
		n++;
		
		assert check();
	}
	
	/**
	 * Removes the specified key and associated value from this symbol table
	 * (if the key is in the symbol table).
	 *
	 * @param key the key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public void delete(Key key) {
		if (key == null) throw new IllegalArgumentException("argument to delete() is null");
		if (isEmpty()) return;
		
		// compute rank
		int i = rank(key);
		
		// key not in table
		if (i == n || cmpt.compare(keys[i], key) != 0) {
			return;
		}
		
		for (int j = i; j < n - 1; j++) {
			keys[j] = keys[j + 1];
//			vals[j] = vals[j+1];
		}
		
		n--;
		keys[n] = null;  // to avoid loitering
//		vals[n] = null;
		
		// resize if 1/4 full
		if (n > 0 && n == keys.length / 4) resize(keys.length / 2);
		
		assert check();
	}
	
	/**
	 * Removes the smallest key and associated value from this symbol table.
	 *
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	public void deleteMin() {
		if (isEmpty()) throw new NoSuchElementException("Symbol table underflow error");
		delete(min());
	}
	
	/**
	 * Removes the largest key and associated value from this symbol table.
	 *
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	public void deleteMax() {
		if (isEmpty()) throw new NoSuchElementException("Symbol table underflow error");
		delete(max());
	}
	
	
	/***************************************************************************
	 *  Ordered symbol table methods.
	 ***************************************************************************/
	
	/**
	 * Returns the smallest key in this symbol table.
	 *
	 * @return the smallest key in this symbol table
	 * @throws NoSuchElementException if this symbol table is empty
	 */
	public Key min() {
		if (isEmpty()) return null;
		return keys[0];
	}
	
	/**
	 * Returns the largest key in this symbol table.
	 *
	 * @return the largest key in this symbol table
	 * @throws NoSuchElementException if this symbol table is empty
	 */
	public Key max() {
		if (isEmpty()) return null;
		return keys[n - 1];
	}
	
	/**
	 * Return the kth smallest key in this symbol table.
	 *
	 * @param k the order statistic
	 * @return the kth smallest key in this symbol table
	 * @throws IllegalArgumentException unless {@code k} is between 0 and
	 *                                  <em>n</em> &minus; 1
	 */
	public Key select(int k) {
		if (k < 0 || k >= n) return null;
		return keys[k];
	}
	
	/**
	 * Returns the largest key in this symbol table less than or equal to {@code key}.
	 *
	 * @param key the key
	 * @return the largest key in this symbol table less than or equal to {@code key}
	 * @throws NoSuchElementException   if there is no such key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public Key floor(Key key) {
		if (key == null) throw new IllegalArgumentException("argument to floor() is null");
		int i = rank(key);
		if (i < n && cmpt.compare(key, keys[i]) == 0) return keys[i];
		if (i == 0) return null;
		else return keys[i - 1];
	}
	
	@Override
	public int height() {
		return 0;
	}
	
	/**
	 * Returns the smallest key in this symbol table greater than or equal to {@code key}.
	 *
	 * @param key the key
	 * @return the smallest key in this symbol table greater than or equal to {@code key}
	 * @throws NoSuchElementException   if there is no such key
	 * @throws IllegalArgumentException if {@code key} is {@code null}
	 */
	public Key ceiling(Key key) {
		if (key == null) throw new IllegalArgumentException("argument to ceiling() is null");
		int i = rank(key);
		if (i == n) return null;
		else return keys[i];
	}
	
	/**
	 * Returns the number of keys in this symbol table in the specified range.
	 *
	 * @param lo minimum endpoint
	 * @param hi maximum endpoint
	 * @return the number of keys in this symbol table between {@code lo}
	 * (inclusive) and {@code hi} (inclusive)
	 * @throws IllegalArgumentException if either {@code lo} or {@code hi}
	 *                                  is {@code null}
	 */
	public int size(Key lo, Key hi) {
		if (lo == null) throw new IllegalArgumentException("first argument to size() is null");
		if (hi == null) throw new IllegalArgumentException("second argument to size() is null");
		
		if (cmpt.compare(lo, hi) > 0) return 0;
		if (contains(hi)) return rank(hi) - rank(lo) + 1;
		else return rank(hi) - rank(lo);
	}
	
	/**
	 * Returns all keys in this symbol table as an {@code Iterable}.
	 * To iterate over all of the keys in the symbol table named {@code st},
	 * use the foreach notation: {@code for (Key key : st.keys())}.
	 *
	 * @return all keys in this symbol table
	 */
	public Iterable<Key> keys() {
		return keys(min(), max());
	}
	
	/**
	 * Returns all keys in this symbol table in the given range,
	 * as an {@code Iterable}.
	 *
	 * @param lo minimum endpoint
	 * @param hi maximum endpoint
	 * @return all keys in this symbol table between {@code lo}
	 * (inclusive) and {@code hi} (inclusive)
	 * @throws IllegalArgumentException if either {@code lo} or {@code hi}
	 *                                  is {@code null}
	 */
	public Iterable<Key> keys(Key lo, Key hi) {
		if (lo == null) throw new IllegalArgumentException("first argument to keys() is null");
		if (hi == null) throw new IllegalArgumentException("second argument to keys() is null");
		
		Queue<Key> queue = new LinkedBlockingQueue<Key>();
		if (cmpt.compare(lo, hi) > 0) return queue;
		for (int i = rank(lo); i < rank(hi); i++)
			queue.add(keys[i]);
		if (contains(hi)) queue.add(keys[rank(hi)]);
		return queue;
	}
	
	
	/***************************************************************************
	 *  Check internal invariants.
	 ***************************************************************************/
	
	private boolean check() {
		return isSorted() && rankCheck();
	}
	
	// are the items in the array in ascending order?
	private boolean isSorted() {
		for (int i = 1; i < size(); i++)
			if (cmpt.compare(keys[i], keys[i - 1]) < 0) return false;
		return true;
	}
	
	// check that rank(select(i)) = i
	private boolean rankCheck() {
		for (int i = 0; i < size(); i++)
			if (i != rank(select(i))) return false;
		for (int i = 0; i < size(); i++)
			if (cmpt.compare(keys[i], select(rank(keys[i]))) != 0) return false;
		return true;
	}
}