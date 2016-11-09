package cz.cvut.fit.acb.triplets.encode;

import java.util.function.Consumer;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.TripletUtils;

public class Salomon2TripletEncoder extends BaseTripletEncoder {
	
	private static final int BIT_FLAG = 1;
	private final TripletFieldId flagField;
	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	private final int distanceMask;

	public Salomon2TripletEncoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
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
			byte b = sequence.byteAt(idx);
			dictionary.update(idx, 1);
			logger.debug("Triplet {}", TripletUtils.tripletString(0, b));
			output.accept(visitor -> {
				visitor.set(flagField, 0);
				visitor.set(byteField, b);
			});
			return idx + 1;
		} else {
			// flag 1
			int leng2 = leng + idx == sequence.length() ? leng-1 : leng;
			dictionary.update(idx, leng2 + 1);
			byte b = sequence.byteAt(idx + leng2);
			logger.debug("Triplet {}", TripletUtils.tripletString(1, dist, leng2, b));
			output.accept(visitor -> {
				visitor.set(flagField, 1);
				visitor.set(distField, dist & distanceMask);
				visitor.set(lengField, leng2);
				visitor.set(byteField, b);
			});
			return idx + leng2 + 1;
		}
	}
}
