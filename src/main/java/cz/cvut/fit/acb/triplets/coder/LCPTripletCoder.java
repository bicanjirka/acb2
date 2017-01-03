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

public class LCPTripletCoder extends BaseTripletCoder {
	
	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	private final int distanceMask;
	
	public LCPTripletCoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary, distanceBits);
		this.distanceMask = (1 << distanceBits) - 1;
		this.distField = new TripletFieldId(0, distanceBits);
		this.lengField = new TripletFieldId(1, lengthBits, true);
		this.byteField = new TripletFieldId(2, Byte.SIZE);
	}
	
	@Override
	protected int encodeStep(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
		int ctx = info.getContext();
		int cnt = info.getContent();
		int leng2 = info.getLength();
		int leng = leng2 + idx == sequence.length() ? leng2 - 1 : leng2;
		int lcp = info.getLcp();
		
		dictionary.update(idx, leng + lcp + 1);
		idx += leng + lcp;
		int dist = cnt == -1 ? 0 : ctx - cnt;
		byte b = sequence.byteAt(idx);
		
		logger.debug("Triplet {}", TripletUtils.tripletString(dist, leng, b));
		output.accept(visitor -> {
			visitor.write(distField, dist & distanceMask);
			visitor.write(lengField, leng);
			visitor.write(byteField, b);
		});
		
		idx++;
		return idx;
	}
	
	@Override
	protected int decodeStep(int idx, TripletProcessor input) {
		int tempDist = input.read(distField);
		int dist = distFunc.applyAsInt(tempDist);
		int leng = input.read(lengField);
		byte b = (byte) input.read(byteField);
		ByteBuilder builder = ((ByteBuilder) sequence);
		
		if (tempDist == leng && leng == b && b == -1) {
			return Integer.MAX_VALUE;
		}
		logger.debug("Triplet {}", TripletUtils.tripletString(dist, leng, b));
		
		int ctx = dictionary.searchContext(idx);
		int cnt = ctx - dist;
		int lcp = 0;
		
		if (ctx > 0) {
			int i = dictionary.select(cnt);
			lcp = dictionary.searchContent(ctx, i).getLcp();
			leng += lcp;
		}
		
		if (leng > 0) {
			byte[] seq = dictionary.copy(cnt, leng);
			builder.append(seq);
		}
		
		builder.append(b);
		leng++;
		dictionary.update(idx, leng);
		return idx + leng;
	}
}
