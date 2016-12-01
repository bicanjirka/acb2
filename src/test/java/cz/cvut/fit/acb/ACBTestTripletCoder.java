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
public class ACBTestTripletCoder extends ACBTest {
	
	@Test
	public void testSimpleCoder() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.TripletCoderE.SIMPLE)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
	
	@Test
	public void testSalomonBytelessCoder() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.TripletCoderE.SALOMON)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
	
	@Test
	public void testSalomonBytefulCoder() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.TripletCoderE.SALOMON2)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
	
	@Test
	public void testValachCoder() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.TripletCoderE.VALACH)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
	
	@Test
	public void testLCPCoder() throws Exception {
		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.TripletCoderE.LCP)) {
			testDictionaryConsistency(provider);
			testSegmentedExecution(provider);
		}
	}
}
