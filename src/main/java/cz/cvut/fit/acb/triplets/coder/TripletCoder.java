package cz.cvut.fit.acb.triplets.coder;

import java.util.function.Consumer;

import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;

/**
 * @author jiri.bican
 */
public sealed interface TripletCoder permits BaseTripletCoder {
	void encode(Consumer<TripletSupplier> output);
	
	DecodeFlag decode(TripletProcessor input);
	
	enum DecodeFlag {
		EOF,
		END_OF_PARTITION
	}
}
