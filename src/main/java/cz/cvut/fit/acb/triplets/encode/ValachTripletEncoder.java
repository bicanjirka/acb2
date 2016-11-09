package cz.cvut.fit.acb.triplets.encode;

import java.util.function.Consumer;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.TripletUtils;

public class ValachTripletEncoder extends BaseTripletEncoder {
	
	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	private final int distanceMask;

	public ValachTripletEncoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.distanceMask = (1 << distanceBits) - 1;
		this.lengField = new TripletFieldId(0, lengthBits);
		this.distField = new TripletFieldId(1, distanceBits);
		this.byteField = new TripletFieldId(2, Byte.SIZE);
		
		/*this.writer = new BaseTripletWriter(distanceBits, lengthBits, out) {
			@Override
			public void write(TripletSupplier t) throws IOException {
				if (print) System.out.println(cnt++ + ") write " + TripletUtils.printValach(t));
				int dist = t.getDistance();
				int leng = t.getLenght();
				byte ch = t.getSymbol();
				out.write(leng, lengBits);
				if (leng != 0) {
					out.write(dist, distBits);
				}
				out.write(ch);
			}
		};*/
	}
	
	@Override
	protected int step(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
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
//			writer.write(new TripletSupplier(0, 0, sequence.byteAt(idx)));
		} else {
			int leng2 = leng + idx == sequence.length() ? leng-1 : leng;
			dictionary.update(idx, leng2 + 1);
			idx += leng2;
			byte b = sequence.byteAt(idx);
			logger.debug("Triplet {}", TripletUtils.tripletString(leng2, dist, b));
			output.accept(visitor -> {
				visitor.set(lengField, leng2);
				visitor.set(distField, dist & distanceMask);
				visitor.set(byteField, b);
			});
//			writer.write(new TripletSupplier(dist, leng, sequence.byteAt(idx)));
		}
		return idx + 1;
	}
	
}
