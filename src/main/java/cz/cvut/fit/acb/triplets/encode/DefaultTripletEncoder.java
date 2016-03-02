package cz.cvut.fit.acb.triplets.encode;

import java.io.IOException;
import java.io.OutputStream;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.Triplet;

public class DefaultTripletEncoder extends BaseTripletEncoder {

	public DefaultTripletEncoder(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.writer = new BaseTripletWriter(distanceBits, lengthBits, out) {
			@Override
			public void write(Triplet t) throws IOException {
				if (print) System.out.println(cnt++ + ") write " + t);
				out.write(t.getDistance(), distBits);
				out.write(t.getLenght(), lengBits);
				out.write(t.getSymbol());
			}
		};
	}

	@Override
	public int proccess(int idx, DictionaryInfo info) throws IOException {
		int ctx = info.getContext();
		int cnt = info.getContent();
		int leng = info.getLength();

		dictionary.update(idx, leng + 1);
		idx += leng;
		int dist = cnt == -1 ? 0 : ctx - cnt;
		writer.write(new Triplet(dist, leng, sequence.byteAt(idx)));
		return idx + 1;
	}
}