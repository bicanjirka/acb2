package cz.cvut.fit.acb.triplets.encode;

import java.util.function.Consumer;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.utils.TripletUtils;

public class DefaultTripletEncoder extends BaseTripletEncoder {

	private final TripletFieldId distField;
	private final TripletFieldId lengField;
	private final TripletFieldId byteField;
	private final int distanceMask;

	public DefaultTripletEncoder(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.distanceMask = (1 << distanceBits) - 1;
		this.distField = new TripletFieldId(0, distanceBits);
		this.lengField = new TripletFieldId(1, lengthBits);
		this.byteField = new TripletFieldId(2, Byte.SIZE);
	}

	@Override
	public int step(int idx, DictionaryInfo info, Consumer<TripletSupplier> output) {
		int ctx = info.getContext();
		int cnt = info.getContent();
		int leng2 = info.getLength();
		int leng = leng2 + idx == sequence.length() ? leng2-1 : leng2;

		dictionary.update(idx, leng + 1);
		idx += leng;
		int dist = cnt == -1 ? 0 : ctx - cnt;
		byte b = sequence.byteAt(idx);

		logger.debug("Triplet {}", TripletUtils.tripletString(dist, leng, b));
		output.accept(visitor -> {
			visitor.set(distField, dist & distanceMask);
			visitor.set(lengField, leng);
			visitor.set(byteField, b);
		});

		idx++;
		return idx;
	}
}
