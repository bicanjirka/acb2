package cz.cvut.fit.acb.triplets.decode;

import java.util.function.IntUnaryOperator;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.utils.BitUtils;
import cz.cvut.fit.acb.utils.TripletUtils;

public class ValachTripletDecoder extends BaseTripletDecoder {
	
	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	private final IntUnaryOperator distFunc;

	public ValachTripletDecoder(ByteBuilder sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.distFunc = value -> BitUtils.isNegative(value, distanceBits) ? BitUtils.fillHighBits(value) : value;
		this.lengField = new TripletFieldId(0, lengthBits);
		this.distField = new TripletFieldId(1, distanceBits);
		this.byteField = new TripletFieldId(2, Byte.SIZE);
//		this.reader = new BaseTripletReader<TripletSupplier>(distanceBits, lengthBits, in) {
//			@Override
//			public TripletSupplier read() throws IOException {
//				int length = in.read(lengBits);
//				TripletSupplier t = null;
//
//				if (length == 0) {
//					int ch = in.read();
////					t = eof(length, ch) ? null : new TripletSupplier(0, 0, ch);
//				} else {
//					int read = in.read(distBits);
//					int distance = BitUtils.isNegative(read, distBits) ? BitUtils.fillHighBits(read) : read;
//					int ch = in.read();
////					t = eof(read, length, ch) ? null : new TripletSupplier(distance, length, ch);
//				}
////				if (print) System.out.println(cnt++ + ") read " + TripletUtils.printValach(t));
//				return t;
//			}
//		};
	}
	
	@Override
	public void process(TripletProcessor input) {
		int idx = 0;
		while (true) {
			int leng = input.get(lengField);
			if (leng == -1) {
				break; // eof
			}
			if (leng == 0) {
				byte b = (byte) input.get(byteField);
				logger.debug("Triplet {}", TripletUtils.tripletString(0, b));
				sequence.append(b);
				dictionary.update(idx, 1);
				idx++;
			} else {
				int tempDist = input.get(distField);
				int dist = distFunc.applyAsInt(tempDist);
				byte b = (byte) input.get(byteField);
				logger.debug("Triplet {}", TripletUtils.tripletString(leng, dist, b));
				
				int ctx = dictionary.searchContext(idx);
				int cnt = ctx - dist;
				byte[] seq = dictionary.copy(cnt, leng);
				sequence.append(seq);
				sequence.append(b);
				leng++;
				dictionary.update(idx, leng);
				idx += leng;
			}
		}
	}

	/*public boolean process() throws IOException {
		Triplet t = reader.read();
		if (t == null)
			return false;
		int dist = t.getDistance();
		int leng = t.getLenght();
		byte b = t.getSymbol();
		int ctx = dictionary.searchContext(idx);
		int cnt = ctx - dist;

		if (leng > 0) {
			byte[] seq = dictionary.copy(cnt, leng);
			sequence.append(seq);
		}

		if (leng == 0) {
			sequence.append(b);
			dictionary.update(idx, 1);
			idx++;
		} else {
			sequence.append(b);
			leng++;
			dictionary.update(idx, leng);
			idx += leng;
		}
		return true;
	}*/
}
