package cz.cvut.fit.acb.dictionary.core;

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

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * The <tt>BST</tt> class represents an ordered symbol table of generic key-value pairs. It supports the usual <em>put</em>, <em>read</em>,
 * <em>contains</em>, <em>delete</em>, <em>size</em>, and <em>is-empty</em> methods. It also provides ordered methods for finding the
 * <em>minimum</em>, <em>maximum</em>, <em>floor</em>, and <em>ceiling</em>. It also provides a <em>keys</em> method for iterating over all
 * of the keys. A symbol table implements the <em>associative array</em> abstraction: when associating a value with a key that is already in
 * the symbol table, the convention is to replace the old value with the new value. Unlike {@link java.util.Map}, this class uses the
 * convention that values cannot be <tt>null</tt>&mdash;setting the value associated with a key to <tt>null</tt> is equivalent to deleting
 * the key from the symbol table.
 * <p>
 * This implementation uses a left-leaning red-black BST. It requires that the key type implements the <tt>Comparable</tt> interface and
 * calls the <tt>compareTo()</tt> and method to compare two keys. It does not call either <tt>equals()</tt> or <tt>hashCode()</tt>. The
 * <em>put</em>, <em>contains</em>, <em>remove</em>, <em>minimum</em>, <em>maximum</em>, <em>ceiling</em>, and <em>floor</em> operations
 * each take logarithmic time in the worst case, if the tree becomes unbalanced. The <em>size</em>, and <em>is-empty</em> operations take
 * constant time. Construction takes constant time.
 * <p>
 * For additional documentation, see <a href="http://algs4.cs.princeton.edu/33balanced">Section 3.3</a> of <i>Algorithms, 4th Edition</i> by
 * Robert Sedgewick and Kevin Wayne. For other implementations, see {@link ST}, {@link BinarySearchST}, {@link SequentialSearchST},
 * {@link BST}, {@link SeparateChainingHashST}, and {@link LinearProbingHashST}, <i>Algorithms, 4th Edition</i> by Robert Sedgewick and
 * Kevin Wayne.
 */

public class RedBlackBST<Key> implements OrderStatisticTree<Key> {
	
	private static final boolean RED = true;
	private static final boolean BLACK = false;
	private final Comparator<Key> cmpt;
	private Node root; // root of the BST
	
	/**
	 * Initializes an empty symbol table.
	 */
	public RedBlackBST(Comparator<Key> comparator) {
		cmpt = comparator;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RedBlackBST)) return false;
		RedBlackBST<?> that = (RedBlackBST<?>) o;
		return Objects.equals(root, that.root);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(root);
	}
	
	// restore red-black tree invariant
	private Node balance(Node h) {
		assert (h != null);
		
		if (isRed(h.right))
			h = rotateLeft(h);
		if (isRed(h.left) && isRed(h.left.left))
			h = rotateRight(h);
		if (isRed(h.left) && isRed(h.right))
			flipColors(h);
		
		h.N = size(h.left) + size(h.right) + 1;
		return h;
	}
	
	/**
	 * Returns the smallest key in the symbol table greater than or equal to <tt>key</tt>.
	 *
	 * @param key the key
	 * @return the smallest key in the symbol table greater than or equal to <tt>key</tt>
	 * @throws NoSuchElementException if there is no such key
	 * @throws NullPointerException   if <tt>key</tt> is <tt>null</tt>
	 */
	@Override
	public Key ceiling(Key key) {
		if (isEmpty())
			throw new NoSuchElementException("called ceiling() with empty symbol table");
		Node x = ceiling(root, key);
		if (x == null)
			return null;
		else
			return x.key;
	}
	
	// the smallest key in the subtree rooted at x greater than or equal to the given key
	private Node ceiling(Node x, Key key) {
		if (x == null)
			return null;
		int cmp = cmpt.compare(key, x.key);
		if (cmp == 0)
			return x;
		if (cmp > 0)
			return ceiling(x.right, key);
		Node t = ceiling(x.left, key);
		if (t != null)
			return t;
		else
			return x;
	}
	
	/***************************************************************************
	 * Check integrity of red-black tree data structure.
	 ***************************************************************************/
	private boolean check() {
		if (!isBST())
			System.out.println("Not in symmetric order");
		if (!isSizeConsistent())
			System.out.println("Subtree counts not consistent");
		if (!isRankConsistent())
			System.out.println("Ranks not consistent");
		if (!is23())
			System.out.println("Not a 2-3 tree");
		if (!isBalanced())
			System.out.println("Not balanced");
		return isBST() && isSizeConsistent() && isRankConsistent() && is23() && isBalanced();
	}
	
	/**
	 * Does this symbol table contain the given key?
	 *
	 * @param key the key
	 * @return <tt>true</tt> if this symbol table contains <tt>key</tt> and <tt>false</tt> otherwise
	 * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
	 */
	@Override
	public boolean contains(Key key) {
		return get(root, key) != null;
	}
	
	/**
	 * Removes the key and associated value from the symbol table (if the key is in the symbol table).
	 *
	 * @param key the key
	 * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
	 */
	@Override
	public void delete(Key key) {
		if (!contains(key)) {
			System.err.println("symbol table does not contain " + key);
			return;
		}
		
		// if both children of root are black, write root to red
		if (!isRed(root.left) && !isRed(root.right))
			root.color = RED;
		
		root = delete(root, key);
		if (!isEmpty())
			root.color = BLACK;
		assert check();
	}
	
	/***************************************************************************
	 * Red-black tree insertion.
	 ***************************************************************************/
	
	// delete the key-value pair with the given key rooted at h
	private Node delete(Node h, Key key) {
		assert get(h, key) != null;
		
		if (cmpt.compare(key, h.key) < 0) {
			if (!isRed(h.left) && !isRed(h.left.left))
				h = moveRedLeft(h);
			h.left = delete(h.left, key);
		} else {
			if (isRed(h.left))
				h = rotateRight(h);
			if (cmpt.compare(key, h.key) == 0 && (h.right == null))
				return null;
			if (!isRed(h.right) && !isRed(h.right.left))
				h = moveRedRight(h);
			if (cmpt.compare(key, h.key) == 0) {
				Node x = min(h.right);
				h.key = x.key;
				// h.val = x.val;
				// h.val = read(h.right, min(h.right).key);
				// h.key = min(h.right).key;
				h.right = deleteMin(h.right);
			} else
				h.right = delete(h.right, key);
		}
		return balance(h);
	}
	
	/**
	 * Removes the largest key and associated value from the symbol table.
	 *
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	@Override
	public void deleteMax() {
		if (isEmpty())
			throw new NoSuchElementException("BST underflow");
		
		// if both children of root are black, write root to red
		if (!isRed(root.left) && !isRed(root.right))
			root.color = RED;
		
		root = deleteMax(root);
		if (!isEmpty())
			root.color = BLACK;
		assert check();
	}
	
	/***************************************************************************
	 * Red-black tree deletion.
	 ***************************************************************************/
	
	// delete the key-value pair with the maximum key rooted at h
	private Node deleteMax(Node h) {
		if (isRed(h.left))
			h = rotateRight(h);
		
		if (h.right == null)
			return null;
		
		if (!isRed(h.right) && !isRed(h.right.left))
			h = moveRedRight(h);
		
		h.right = deleteMax(h.right);
		
		return balance(h);
	}
	
	/**
	 * Removes the smallest key and associated value from the symbol table.
	 *
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	@Override
	public void deleteMin() {
		if (isEmpty())
			throw new NoSuchElementException("BST underflow");
		
		// if both children of root are black, write root to red
		if (!isRed(root.left) && !isRed(root.right))
			root.color = RED;
		
		root = deleteMin(root);
		if (!isEmpty())
			root.color = BLACK;
		assert check();
	}
	
	// delete the key-value pair with the minimum key rooted at h
	private Node deleteMin(Node h) {
		if (h.left == null)
			return null;
		
		if (!isRed(h.left) && !isRed(h.left.left))
			h = moveRedLeft(h);
		
		h.left = deleteMin(h.left);
		return balance(h);
	}
	
	// flip the colors of a node and its two children
	private void flipColors(Node h) {
		// h must have opposite color of its two children
		assert (h != null) && (h.left != null) && (h.right != null);
		assert (!isRed(h) && isRed(h.left) && isRed(h.right)) || (isRed(h) && !isRed(h.left) && !isRed(h.right));
		h.color = !h.color;
		h.left.color = !h.left.color;
		h.right.color = !h.right.color;
	}
	
	/**
	 * Returns the largest key in the symbol table less than or equal to <tt>key</tt>.
	 *
	 * @param key the key
	 * @return the largest key in the symbol table less than or equal to <tt>key</tt>
	 * @throws NoSuchElementException if there is no such key
	 * @throws NullPointerException   if <tt>key</tt> is <tt>null</tt>
	 */
	@Override
	public Key floor(Key key) {
		if (isEmpty())
			throw new NoSuchElementException("called floor() with empty symbol table");
		Node x = floor(root, key);
		if (x == null)
			return null;
		else
			return x.key;
	}
	
	// the largest key in the subtree rooted at x less than or equal to the given key
	private Node floor(Node x, Key key) {
		if (x == null)
			return null;
		int cmp = cmpt.compare(key, x.key);
		if (cmp == 0)
			return x;
		if (cmp < 0)
			return floor(x.left, key);
		Node t = floor(x.right, key);
		if (t != null)
			return t;
		else
			return x;
	}
	
	/***************************************************************************
	 * Standard BST search without read() method
	 ***************************************************************************/
	
	// value associated with the given key in subtree rooted at x; null if no such key
	private Node get(Node x, Key key) {
		while (x != null) {
			int cmp = cmpt.compare(key, x.key);
			if (cmp < 0)
				x = x.left;
			else if (cmp > 0)
				x = x.right;
			else
				return x;
		}
		return null;
	}
	
	/**
	 * Returns the height of the BST (for debugging).
	 *
	 * @return the height of the BST (a 1-node tree has height 0)
	 */
	@Override
	public int height() {
		return height(root);
	}
	
	private int height(Node x) {
		if (x == null)
			return -1;
		return 1 + Math.max(height(x.left), height(x.right));
	}
	
	// Does the tree have no red right links, and at most one (left)
	// red links in a row on any path?
	private boolean is23() {
		return is23(root);
	}
	
	private boolean is23(Node x) {
		if (x == null)
			return true;
		if (isRed(x.right))
			return false;
		if (x != root && isRed(x) && isRed(x.left))
			return false;
		return is23(x.left) && is23(x.right);
	}
	
	// do all paths from root to leaf have same number of black edges?
	private boolean isBalanced() {
		int black = 0; // number of black links on path from root to min
		Node x = root;
		while (x != null) {
			if (!isRed(x))
				black++;
			x = x.left;
		}
		return isBalanced(root, black);
	}
	
	/***************************************************************************
	 * Utility functions.
	 ***************************************************************************/
	
	// does every path from the root to a leaf have the given number of black links?
	private boolean isBalanced(Node x, int black) {
		if (x == null)
			return black == 0;
		if (!isRed(x))
			black--;
		return isBalanced(x.left, black) && isBalanced(x.right, black);
	}
	
	// does this binary tree satisfy symmetric order?
	// Note: this test also ensures that data structure is a binary tree since order is strict
	private boolean isBST() {
		return isBST(root, null, null);
	}
	
	/***************************************************************************
	 * Ordered symbol table methods.
	 ***************************************************************************/
	
	// is the tree rooted at x a BST with all keys strictly between min and max
	// (if min or max is null, treat as empty constraint)
	// Credit: Bob Dondero's elegant solution
	private boolean isBST(Node x, Key min, Key max) {
		if (x == null)
			return true;
		if (min != null && cmpt.compare(x.key, min) <= 0)
			return false;
		if (max != null && cmpt.compare(x.key, max) >= 0)
			return false;
		return isBST(x.left, min, x.key) && isBST(x.right, x.key, max);
	}
	
	/**
	 * Is this symbol table empty?
	 *
	 * @return <tt>true</tt> if this symbol table is empty and <tt>false</tt> otherwise
	 */
	@Override
	public boolean isEmpty() {
		return root == null;
	}
	
	// check that ranks are consistent
	private boolean isRankConsistent() {
		for (int i = 0; i < size(); i++)
			if (i != rank(select(i)))
				return false;
		for (Key key : keys())
			if (cmpt.compare(key, select(rank(key))) != 0)
				return false;
		return true;
	}
	
	/***************************************************************************
	 * Node helper methods.
	 ***************************************************************************/
	// is node x red; false if x is null ?
	private boolean isRed(Node x) {
		if (x == null)
			return false;
		return x.color == RED;
	}
	
	// are the size fields correct?
	private boolean isSizeConsistent() {
		return isSizeConsistent(root);
	}
	
	private boolean isSizeConsistent(Node x) {
		if (x == null)
			return true;
		if (x.N != size(x.left) + size(x.right) + 1)
			return false;
		return isSizeConsistent(x.left) && isSizeConsistent(x.right);
	}
	
	/**
	 * Returns all keys in the symbol table as an <tt>Iterable</tt>. To iterate over all of the keys in the symbol table named <tt>st</tt>,
	 * use the foreach notation: <tt>for (Key key : st.keys())</tt>.
	 *
	 * @return all keys in the sybol table as an <tt>Iterable</tt>
	 */
	@Override
	public Iterable<Key> keys() {
		if (isEmpty())
			return new SynchronousQueue<>();
		return keys(min(), max());
	}
	
	/**
	 * Returns all keys in the symbol table in the given range, as an <tt>Iterable</tt>.
	 *
	 * @return all keys in the sybol table between <tt>lo</tt> (inclusive) and <tt>hi</tt> (exclusive) as an <tt>Iterable</tt>
	 * @throws NullPointerException if either <tt>lo</tt> or <tt>hi</tt> is <tt>null</tt>
	 */
	@Override
	public Iterable<Key> keys(Key lo, Key hi) {
		Queue<Key> queue = new LinkedBlockingQueue<>();
		if (isEmpty() || cmpt.compare(lo, hi) > 0) return queue;
		keys(root, queue, lo, hi);
		return queue;
	}
	
	// add the keys between lo and hi in the subtree rooted at x
	// to the queue
	private void keys(Node x, Queue<Key> queue, Key lo, Key hi) {
		if (x == null)
			return;
		int cmplo = cmpt.compare(lo, x.key);
		int cmphi = cmpt.compare(hi, x.key);
		if (cmplo < 0)
			keys(x.left, queue, lo, hi);
		if (cmplo <= 0 && cmphi >= 0)
			queue.add(x.key);
		if (cmphi > 0)
			keys(x.right, queue, lo, hi);
	}
	
	/**
	 * Returns the largest key in the symbol table.
	 *
	 * @return the largest key in the symbol table
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	@Override
	public Key max() {
		if (isEmpty())
			throw new NoSuchElementException("called max() with empty symbol table");
		return max(root).key;
	}
	
	// the largest key in the subtree rooted at x; null if no such key
	private Node max(Node x) {
		assert x != null;
		if (x.right == null)
			return x;
		else
			return max(x.right);
	}
	
	/**
	 * Returns the smallest key in the symbol table.
	 *
	 * @return the smallest key in the symbol table
	 * @throws NoSuchElementException if the symbol table is empty
	 */
	@Override
	public Key min() {
		if (isEmpty())
			throw new NoSuchElementException("called min() with empty symbol table");
		return min(root).key;
	}
	
	/***************************************************************************
	 * Range count and range search.
	 ***************************************************************************/
	
	// the smallest key in subtree rooted at x; null if no such key
	private Node min(Node x) {
		assert x != null;
		if (x.left == null)
			return x;
		else
			return min(x.left);
	}
	
	// Assuming that h is red and both h.left and h.left.left
	// are black, make h.left or one of its children red.
	private Node moveRedLeft(Node h) {
		assert (h != null);
		assert isRed(h) && !isRed(h.left) && !isRed(h.left.left);
		
		flipColors(h);
		if (isRed(h.right.left)) {
			h.right = rotateRight(h.right);
			h = rotateLeft(h);
			flipColors(h);
		}
		return h;
	}
	
	// Assuming that h is red and both h.right and h.right.left
	// are black, make h.right or one of its children red.
	private Node moveRedRight(Node h) {
		assert (h != null);
		assert isRed(h) && !isRed(h.right) && !isRed(h.right.left);
		flipColors(h);
		if (isRed(h.left.left)) {
			h = rotateRight(h);
			flipColors(h);
		}
		return h;
	}
	
	/**
	 * Inserts the key-value pair into the symbol table, overwriting the old value with the new value if the key is already in the symbol
	 * table. If the value is <tt>null</tt>, this effectively deletes the key from the symbol table.
	 *
	 * @param key the key
	 * @param val the value
	 * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
	 */
	@Override
	public void put(Key key) {
		if (key == null)
			throw new NullPointerException("first argument to put() is null");
		root = put(root, key);
		root.color = BLACK;
		assert check();
	}
	
	// insert the key-value pair in the subtree rooted at h
	private Node put(Node h, Key key) {
		if (h == null)
			return new Node(key, RED, 1);
		
		int cmp = cmpt.compare(key, h.key);
		if (cmp < 0)
			h.left = put(h.left, key);
		else if (cmp > 0)
			h.right = put(h.right, key);
		// else do nothing on duplicate keys, TODO throw error
		
		// fix-up any right-leaning links
		if (isRed(h.right) && !isRed(h.left))
			h = rotateLeft(h);
		if (isRed(h.left) && isRed(h.left.left))
			h = rotateRight(h);
		if (isRed(h.left) && isRed(h.right))
			flipColors(h);
		h.N = size(h.left) + size(h.right) + 1;
		
		return h;
	}
	
	@Override
	public OrderStatisticTree<Key> clone() {
		RedBlackBST<Key> bst = new RedBlackBST<>(cmpt);
		if (root != null)
			bst.root = new Node(root);
		assert equals(bst);
		return bst;
	}
	
	/**
	 * Return the number of keys in the symbol table strictly less than <tt>key</tt>.
	 *
	 * @param key the key
	 * @return the number of keys in the symbol table strictly less than <tt>key</tt>
	 * @throws NullPointerException if <tt>key</tt> is <tt>null</tt>
	 */
	@Override
	public int rank(Key key) {
		return rank(key, root);
	}
	
	// number of keys less than key in the subtree rooted at x
	private int rank(Key key, Node x) {
		if (x == null)
			return 0;
		int cmp = cmpt.compare(key, x.key);
		if (cmp < 0)
			return rank(key, x.left);
		else if (cmp > 0)
			return 1 + size(x.left) + rank(key, x.right);
		else
			return size(x.left);
	}
	
	// make a right-leaning link lean to the left
	private Node rotateLeft(Node h) {
		assert (h != null) && isRed(h.right);
		Node x = h.right;
		h.right = x.left;
		x.left = h;
		x.color = x.left.color;
		x.left.color = RED;
		x.N = h.N;
		h.N = size(h.left) + size(h.right) + 1;
		return x;
	}
	
	/***************************************************************************
	 * Red-black tree helper functions.
	 ***************************************************************************/
	
	// make a left-leaning link lean to the right
	private Node rotateRight(Node h) {
		assert (h != null) && isRed(h.left);
		Node x = h.left;
		h.left = x.right;
		x.right = h;
		x.color = x.right.color;
		x.right.color = RED;
		x.N = h.N;
		h.N = size(h.left) + size(h.right) + 1;
		return x;
	}
	
	/**
	 * Return the kth smallest key in the symbol table.
	 *
	 * @param k the order statistic
	 * @return the kth smallest key in the symbol table
	 * @throws IllegalArgumentException unless <tt>k</tt> is between 0 and <em>N</em> &minus; 1
	 */
	@Override
	public Key select(int k) {
		if (k < 0 || k >= size())
			throw new IllegalArgumentException("Bad argument for select: " + k);
		Node x = select(root, k);
		return x.key;
	}
	
	// the key of rank k in the subtree rooted at x
	private Node select(Node x, int k) {
		assert x != null;
		assert k >= 0 && k < size(x);
		int t = size(x.left);
		if (t > k)
			return select(x.left, k);
		else if (t < k)
			return select(x.right, k - t - 1);
		else
			return x;
	}
	
	/**
	 * Returns the number of key-value pairs in this symbol table.
	 *
	 * @return the number of key-value pairs in this symbol table
	 */
	@Override
	public int size() {
		return size(root);
	}
	
	/**
	 * Returns the number of keys in the symbol table in the given range.
	 *
	 * @return the number of keys in the sybol table between <tt>lo</tt> (inclusive) and <tt>hi</tt> (exclusive)
	 * @throws NullPointerException if either <tt>lo</tt> or <tt>hi</tt> is <tt>null</tt>
	 */
	@Override
	public int size(Key lo, Key hi) {
		if (cmpt.compare(lo, hi) > 0)
			return 0;
		if (contains(hi))
			return rank(hi) - rank(lo) + 1;
		else
			return rank(hi) - rank(lo);
	}
	
	// number of node in subtree rooted at x; 0 if x is null
	private int size(Node x) {
		if (x == null)
			return 0;
		return x.N;
	}
	
	/**
	 * User readable display of <tt>LinkedRedBlackBST</tt> data type.
	 */
	@Override
	public String toString() {
		// return keys().toString();
		return keys().toString() + "\n" + toString("", root);
	}
	
	private String toString(String prefix, Node node) {
		if (node == null)
			return "";
		String string = prefix + node.key + (node.color ? "|" : "");
		if (node.right != null)
			string = toString("\t" + prefix, node.right) + "\n" + string;
		if (node.left != null)
			string = string + "\n" + toString("    " + prefix, node.left);
		return string;
	}
	
	// BST helper node data type
	private class Node {
		private Key key; // key
		private Node left, right; // links to left and right subtrees
		private boolean color; // color of parent link
		private int N; // subtree count
		
		public Node(Key key, boolean color, int N) {
			this.key = key;
			this.color = color;
			this.N = N;
		}
		
		public Node(Node node) {
			this(node.key, node.color, node.N);
			if (node.left != null) left = new Node(node.left);
			if (node.right != null) right = new Node(node.right);
		}
		
		@Override
		public String toString() {
			return String.valueOf(key) + " [" + String.valueOf(left.key) + "<>" + String.valueOf(right.key) + " "
					+ (color ? "red" : "black") + ", N=" + N + "]";
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
//			if (!(o instanceof Node)) return false;
			Node node = (Node) o;
			return color == node.color &&
					N == node.N &&
					Objects.equals(key, node.key) &&
					Objects.equals(left, node.left) &&
					Objects.equals(right, node.right);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(key, left, right, color, N);
		}
	}
	
}
