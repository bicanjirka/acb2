/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb;

import cz.cvut.fit.acb.utils.ACBTestProviderGenerator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
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
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel(Level.TRACE);
		ctx.updateLoggers();
		ACBProviderParameters par = new ACBProviderParameters();
		par.tc = ACBProviderParameters.TripletCoderE.LCP;
		ACBProvider provider = new ACBProviderImpl(par);
//		for (ACBProvider provider : ACBTestProviderGenerator.generate(ACBProviderParameters.TripletCoderE.LCP)) {
			testDictionaryConsistency(provider);
//			testSegmentedExecution(provider);
//		}
	}
}
