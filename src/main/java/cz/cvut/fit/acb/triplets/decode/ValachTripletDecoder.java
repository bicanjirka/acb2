package cz.cvut.fit.acb.triplets.decode;

import java.io.IOException;
import java.io.InputStream;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.utils.BitUtils;
import cz.cvut.fit.acb.utils.TripletUtils;

public class ValachTripletDecoder extends BaseTripletDecoder {

	private BaseTripletReader<Triplet> reader;

	public ValachTripletDecoder(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.reader = new BaseTripletReader<Triplet>(distanceBits, lengthBits, in) {
			@Override
			public Triplet read() throws IOException {
				int length = in.read(lengBits);
				Triplet t;
				
				if (length == 0) {
					int ch = in.read();
					t = eof(length, ch) ? null : new Triplet(0, 0, ch);
				} else {
					int read = in.read(distBits);
					int distance = BitUtils.isNegative(read, distBits) ? BitUtils.fillHighBits(read) : read;
					int ch = in.read();
					t = eof(read, length, ch) ? null : new Triplet(distance, length, ch);
				}
				if (print) System.out.println(cnt++ + ") read " + TripletUtils.printValach(t));
				return t;
			}
		};
	}

	@Override
	public boolean proccess() throws IOException {
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
	}

}
