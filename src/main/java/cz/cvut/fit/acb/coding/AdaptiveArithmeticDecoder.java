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
				new AdaptiveArithmeticDecompress(index.bitSize(), bytes.get(index.index()), lengthFreq) :
				new AdaptiveArithmeticDecompress(index.bitSize(), bytes.get(index.index()));
	}
	
	@Override
	protected int decompress(AdaptiveArithmeticDecompress object) {
		return object.decompress();
	}
}
