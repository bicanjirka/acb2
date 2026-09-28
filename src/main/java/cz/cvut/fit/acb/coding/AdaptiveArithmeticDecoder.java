package cz.cvut.fit.acb.coding;

import java.util.List;

import cz.cvut.fit.acb.triplets.TripletFieldId;

/**
 * @author jiri.bican
 */
public class AdaptiveArithmeticDecoder extends ByteToTripletConverter<AdaptiveArithmeticDecompress> {
	
	private final int[] lengthFreq;
	
	public AdaptiveArithmeticDecoder(int[] lengthFreq) {
		this.lengthFreq = lengthFreq.clone();
	}
	
	@Override
	protected AdaptiveArithmeticDecompress createNew(TripletFieldId index, List<byte[]> bytes) {
		return index.isLength() ?
				new AdaptiveArithmeticDecompress(index.getBitSize(), bytes.get(index.getIndex()), lengthFreq) :
				new AdaptiveArithmeticDecompress(index.getBitSize(), bytes.get(index.getIndex()));
	}
	
	@Override
	protected int decompress(AdaptiveArithmeticDecompress object) {
		return object.decompress();
	}
}
