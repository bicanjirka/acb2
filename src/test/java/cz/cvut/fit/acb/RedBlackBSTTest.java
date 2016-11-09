package cz.cvut.fit.acb;

import cz.cvut.fit.acb.dictionary.RedBlackBST;
import junit.framework.TestCase;
import org.junit.Test;

public class RedBlackBSTTest extends TestCase {

	@Test
	public void testInsertAscendingOrder() {
		RedBlackBST<Integer> bst = new RedBlackBST<>(Integer::compareTo);
		
		for (int i = 0; i < 256; i++) {
			bst.put(i);
		}
	}
	
}
