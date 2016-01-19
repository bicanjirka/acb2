package cz.cvut.fit.acb;

import java.util.Comparator;

import org.junit.Test;

import cz.cvut.fit.acb.dictionary.RedBlackBST;
import junit.framework.TestCase;

public class RedBlackBSTTest extends TestCase {

	@Test
	public void testInsertAscendingOrder() {
		RedBlackBST<Integer> bst = new RedBlackBST<>(new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return o1.compareTo(o2);
			}
		});
		
		for (int i = 0; i < 256; i++) {
			bst.put(i);
		}
	}
	
}
