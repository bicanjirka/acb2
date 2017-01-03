/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.utils;

import java.util.ArrayList;
import java.util.Collection;

import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.ACBProviderImpl;
import cz.cvut.fit.acb.ACBProviderParameters;

/**
 * @author Jiri.Bican
 */
public class ACBTestProviderGenerator {
	
	public static Collection<ACBProvider> generate(ACBProviderParameters.TripletCoderE tc) {
		Collection<ACBProvider> ret = new ArrayList<>();
//		for (ACBProviderParameters.CoderE coderE : ACBProviderParameters.CoderE.values()) {
			for (ACBProviderParameters.OrderStatisticTreeE treeE : ACBProviderParameters.OrderStatisticTreeE.values()) {
				ACBProviderParameters param = new ACBProviderParameters();
//				param.cd = coderE;
				param.tr = treeE;
				param.tc = tc;
				ret.add(new ACBProviderImpl(param));
			}
//		}
		return ret;
	}
	
	public static Collection<ACBProvider> generate(ACBProviderParameters.OrderStatisticTreeE tr) {
		Collection<ACBProvider> ret = new ArrayList<>();
//		for (ACBProviderParameters.CoderE coderE : ACBProviderParameters.CoderE.values()) {
			for (ACBProviderParameters.TripletCoderE tripE : ACBProviderParameters.TripletCoderE.values()) {
				ACBProviderParameters param = new ACBProviderParameters();
//				param.cd = coderE;
				param.tr = tr;
				param.tc = tripE;
				ret.add(new ACBProviderImpl(param));
			}
//		}
		return ret;
	}
	
	public static Collection<ACBProvider> generate(ACBProviderParameters.CoderE cd) {
		Collection<ACBProvider> ret = new ArrayList<>();
		for (ACBProviderParameters.TripletCoderE tripE : ACBProviderParameters.TripletCoderE.values()) {
			for (ACBProviderParameters.OrderStatisticTreeE treeE : ACBProviderParameters.OrderStatisticTreeE.values()) {
				ACBProviderParameters param = new ACBProviderParameters();
//				param.cd = cd;
				param.tr = treeE;
				param.tc = tripE;
				ret.add(new ACBProviderImpl(param));
			}
		}
		return ret;
	}
}
