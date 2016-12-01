/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb;

import cz.cvut.fit.acb.utils.ACBTestProviderGenerator;
import org.junit.Test;

/**
 * @author Jiri.Bican
 */
public class ACBTestCoder extends ACBTest {
	
	@Test
	public void testAdaptiveArithmeticCoder() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.CoderE.ADAPTIVE_ARITHMETIC)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
	
	@Test
	public void testBitArrayCoder() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.CoderE.BIT_ARRAY)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
//	@Test
//	public void testRangeCoder() throws Exception {
//		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.CoderE.SIMPLE)) {
//			testDictionaryConsistency(provider);
//			testSegmentedExecution(provider);
//		}
//	}
}
