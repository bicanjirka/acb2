package cz.cvut.fit.acb.coding;

import java.util.ArrayList;
import java.util.Collection;

import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public class AdaptiveArithmeticEncoder extends TripletToByteConverter<AdaptiveArithmeticCompress> {
	
	private final Collection<Runnable> onTerminate = new ArrayList<>();
	private final int[] lengthFreq = {45, 13, 10, 7, 5, 4};
	
	@Override
	protected byte[] getArray(AdaptiveArithmeticCompress object) {
		return object.array();
	}
	
	@Override
	protected AdaptiveArithmeticCompress createNew(TripletFieldId fieldId) {
		AdaptiveArithmeticCompress inst = fieldId.isLength() ?
				new AdaptiveArithmeticCompress(fieldId.getBitSize(), lengthFreq) :
				new AdaptiveArithmeticCompress(fieldId.getBitSize());
		onTerminate.add(inst::terminate);
		return inst;
	}
	
	@Override
	protected void compress(AdaptiveArithmeticCompress object, int value) {
		object.compress(value);
	}
	
	@Override
	protected void terminate() {
		onTerminate.forEach(Runnable::run);
		super.terminate();
	}
}
