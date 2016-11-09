package cz.cvut.fit.acb.triplets.decode;

import java.util.function.IntUnaryOperator;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.utils.BitUtils;
import cz.cvut.fit.acb.utils.TripletUtils;

public class DefaultTripletDecoder extends BaseTripletDecoder {

	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	private final IntUnaryOperator distFunc;

	public DefaultTripletDecoder(ByteBuilder sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.distFunc = value -> BitUtils.isNegative(value, distanceBits) ? BitUtils.fillHighBits(value) : value;
		this.distField = new TripletFieldId(0, distanceBits);
		this.lengField = new TripletFieldId(1, lengthBits);
		this.byteField = new TripletFieldId(2, Byte.SIZE);
//		this.reader = new BaseTripletReader<TripletSupplier>(distanceBits, lengthBits, in) {
//			@Override
//			public TripletSupplier read() throws IOException {
//				int read = in.read(distBits);
//				int distance = BitUtils.isNegative(read, distBits) ? BitUtils.fillHighBits(read) : read;
//				int length = in.read(lengBits);
//				int ch = in.read();
//
////				TripletSupplier t = eof(read, length, ch) ? null : new TripletSupplier(distance, length, ch);
////				if (print) System.out.println(cnt++ + ") read " + t);
//				TripletSupplier t = null;
//				return t;
//			}
//		};
	}

	@Override
	public void process(TripletProcessor input) {
		int idx = 0;
		while (true) {
			int tempDist = input.get(distField);
			int dist = distFunc.applyAsInt(tempDist);
			int leng = input.get(lengField);
			byte b = (byte) input.get(byteField);

			if (tempDist == leng && leng == b && b == -1) {
				break; // eof
			}
			logger.debug("Triplet {}", TripletUtils.tripletString(dist, leng, b));

			int ctx = dictionary.searchContext(idx);
			int cnt = ctx - dist;

			if (leng > 0) {
				byte[] seq = dictionary.copy(cnt, leng);
				sequence.append(seq);
			}

			sequence.append(b);
			leng++;
			dictionary.update(idx, leng);
			idx += leng;
		}
	}
}
