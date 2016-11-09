package cz.cvut.fit.acb.triplets.encode;

import java.util.function.Consumer;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.TripletUtils;

public class SalomonTripletEncoder extends BaseTripletEncoder {
	
	private static final int BIT_FLAG = 1;
	private final TripletFieldId flagField;
	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	private final int distanceMask;
	
	public SalomonTripletEncoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.distanceMask = (1 << distanceBits) - 1;
		this.flagField = new TripletFieldId(0, BIT_FLAG);
		this.distField = new TripletFieldId(1, distanceBits);
		this.lengField = new TripletFieldId(2, lengthBits);
		this.byteField = new TripletFieldId(3, Byte.SIZE);
	}
	
	@Override
	public int step(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
		int ctx = info.getContext();
		int cnt = info.getContent();
		int leng = info.getLength();
		
		int dist = cnt == -1 ? 0 : ctx - cnt;
		
		if (dist == 0 && leng == 0) {
			// flag 0
			dictionary.update(idx, 1);
			byte b = sequence.byteAt(idx);
			logger.debug("Triplet {}", TripletUtils.tripletString(0, b));
			output.accept(visitor -> {
				visitor.set(flagField, 0);
				visitor.set(byteField, b);
			});
			return idx + 1;
		} else {
			// flag 1
			dictionary.update(idx, leng);
			logger.debug("Triplet {}", TripletUtils.tripletString(1, dist, leng));
			output.accept(visitor -> {
				visitor.set(flagField, 1);
				visitor.set(distField, dist & distanceMask); // TODO add bit mask to TripletFieldId
				visitor.set(lengField, leng);
			});
			return idx + leng;
		}
	}
	
}
