package cz.cvut.fit.acb.triplets.coder;

import java.util.function.Consumer;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.TripletUtils;

/**
 * @author jiri.bican
 */
public class ValachTripletCoder extends BaseTripletCoder {
	
	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	
	public ValachTripletCoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary, distanceBits);
		this.lengField = new TripletFieldId(0, lengthBits, true);
		this.distField = new TripletFieldId(1, distanceBits);
		this.byteField = new TripletFieldId(2, Byte.SIZE);
	}
	
	@Override
	protected int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
		int ctx = info.getContext();
		int cnt = info.getContent();
		int leng = info.getLength();
		
		int dist = cnt == -1 ? 0 : ctx - cnt;
		if (leng == 0) {
			dictionary.update(idx, 1);
			byte b = sequence.byteAt(idx);
			logger.debug("Triplet {}", TripletUtils.tripletString(0, b));
			output.accept(visitor -> {
				visitor.set(lengField, 0);
				visitor.set(byteField, b);
			});
		} else {
			int leng2 = leng + idx == sequence.length() ? leng - 1 : leng;
			dictionary.update(idx, leng2 + 1);
			idx += leng2;
			byte b = sequence.byteAt(idx);
			logger.debug("Triplet {}", TripletUtils.tripletString(leng2, dist, b));
			output.accept(visitor -> {
				visitor.set(lengField, leng2);
				visitor.set(distField, dist & distanceMask);
				visitor.set(byteField, b);
			});
		}
		return idx + 1;
	}
	
	@Override
	protected int decodeStep(int idx, TripletProcessor input) {
		ByteBuilder builder = ((ByteBuilder) sequence);
		int leng = input.get(lengField);
		if (leng == -1) {
			return -1;
		}
		if (leng == 0) {
			byte b = (byte) input.get(byteField);
			logger.debug("Triplet {}", TripletUtils.tripletString(0, b));
			builder.append(b);
			dictionary.update(idx, 1);
			return idx + 1;
		} else {
			int tempDist = input.get(distField);
			int dist = distFunc.applyAsInt(tempDist);
			byte b = (byte) input.get(byteField);
			logger.debug("Triplet {}", TripletUtils.tripletString(leng, dist, b));
			
			int ctx = dictionary.searchContext(idx);
			int cnt = ctx - dist;
			byte[] seq = dictionary.copy(cnt, leng);
			builder.append(seq).append(b);
			leng++;
			dictionary.update(idx, leng);
			return idx + leng;
		}
	}
	
}