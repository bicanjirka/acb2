package cz.cvut.fit.acb.triplets.encode;

import java.io.IOException;
import java.io.OutputStream;

import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.utils.TripletUtils;

public class Salomon2TripletEncoder extends BaseTripletEncoder {


	public Salomon2TripletEncoder(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.writer = new BaseTripletWriter(distanceBits, lengthBits, out) {
			@Override
			public void write(Triplet t) throws IOException {
				if (print) System.out.println(cnt++ + ") write " + TripletUtils.printSalomon2(t));
				int dist = t.getDistance();
				int leng = t.getLenght();
				byte ch = t.getSymbol();
				if (dist == 0 && leng == 0) {
					out.write(0, 1);
					out.write(ch);
				} else {
					out.write(1, 1);
					out.write(dist, distBits);
					out.write(leng, lengBits);
					out.write(ch);
				}
			}
		};
	}

	@Override
	public int proccess(int idx, DictionaryInfo info) throws IOException {
		int ctx = info.getContext();
		int cnt = info.getContent();
		int leng = info.getLength();
		
		int dist = cnt == -1 ? 0 : ctx - cnt;
		if (dist == 0 && leng == 0) {
			// flag 0
			dictionary.update(idx, 1);
			writer.write(new Triplet(0, 0, sequence.byteAt(idx)));
			return idx + 1;
		} else {
			// flag 1
			dictionary.update(idx, leng + 1);
			idx += leng;
			writer.write(new Triplet(dist, leng, sequence.byteAt(idx)));
			return idx + 1;
		}
	}
}
