package cz.cvut.fit.acb.triplets.decode;

import java.util.function.IntUnaryOperator;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.utils.BitUtils;
import cz.cvut.fit.acb.utils.TripletUtils;

public class SalomonTripletDecoder extends BaseTripletDecoder {
	
	private static final int BIT_FLAG = 1;
	private final TripletFieldId flagField;
	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	private final IntUnaryOperator distFunc;

	public SalomonTripletDecoder(ByteBuilder sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.distFunc = value -> BitUtils.isNegative(value, distanceBits) ? BitUtils.fillHighBits(value) : value;
		this.flagField = new TripletFieldId(0, BIT_FLAG);
		this.distField = new TripletFieldId(1, distanceBits);
		this.lengField = new TripletFieldId(2, lengthBits);
		this.byteField = new TripletFieldId(3, Byte.SIZE);
	}

	@Override
	public void process(TripletProcessor input) {
		int idx = 0;
		while (true) {
			int flag = input.get(flagField);
			if (flag == 0) {
				byte b = (byte) input.get(byteField);
				if (b == -1) {
					break; // eof
				}
				logger.debug("Triplet {}", TripletUtils.tripletString(0, b));
				sequence.append(b);
				dictionary.update(idx, 1);
				idx++;
				
			} else {
				int tempDist = input.get(distField);
				int dist = distFunc.applyAsInt(tempDist);
				int leng = input.get(lengField);
				
				if (tempDist == leng && leng == -1) {
					break; // eof
				}
				logger.debug("Triplet {}", TripletUtils.tripletString(1, dist, leng));
				
				int ctx = dictionary.searchContext(idx);
				int cnt = ctx - dist;
				byte[] seq = dictionary.copy(cnt, leng);
				sequence.append(seq);
				dictionary.update(idx, leng);
				idx += leng;
			}
		}
			
			
//		int dist = t.getDistance();
//		int leng = t.getLenght();
//		byte b = t.getSymbol();
//		int ctx = dictionary.searchContext(idx);
//		int cnt = ctx - dist;
//
//		if (leng > 0) {
//			byte[] seq = dictionary.copy(cnt, leng);
//			sequence.append(seq);
//		}
//
//		if (dist == 0 && leng == 0) {
//			// flag 0
//			sequence.append(b);
//			dictionary.update(idx, 1);
//			idx++;
//		} else {
//			// flag 1
//			dictionary.update(idx, leng);
//			idx += leng;
//		}
	}

}
