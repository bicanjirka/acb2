/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.triplets.coder;

import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;

/**
 * @author jiri.bican
 */
public interface TripletCoder {
	void encode(Consumer<TripletSupplier> output);
	
	DecodeFlag decode(TripletProcessor input);
	
	enum DecodeFlag {
		EOF,
		END_OF_PARTITION
	}
}
