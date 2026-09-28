package cz.cvut.fit.acb;

import cz.cvut.fit.acb.utils.ACBTestProviderGenerator;
import org.junit.Test;

/**
 * @author Jiri.Bican
 */
public class ACBTestDictionaryStructures extends ACBTest {
	
	@Test
	public void testRedBlackBinarySearchTree() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.OrderStatisticTreeE.RED_BLACK)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
	
	@Test
	public void testBinarySearchTree() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.OrderStatisticTreeE.ST)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
	
	@Test
	public void testBinarySearchArray() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.OrderStatisticTreeE.BST)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
}
