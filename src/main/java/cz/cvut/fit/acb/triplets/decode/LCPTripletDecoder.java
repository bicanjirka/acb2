package cz.cvut.fit.acb.triplets.decode;

import java.io.IOException;
import java.io.InputStream;

import cz.cvut.fit.acb.ACB;
import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.utils.BitUtils;

public class LCPTripletDecoder extends BaseTripletDecoder {

	private BaseTripletReader<TripletWithFlag> reader;
//	private int lastLcp;

	public LCPTripletDecoder(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.reader = new BaseTripletReader<TripletWithFlag>(distanceBits, lengthBits, in) {
			@Override
			public TripletWithFlag read() throws IOException {
				int read = in.read(distBits);
				int distance = BitUtils.isNegative(read, distBits) ? BitUtils.fillHighBits(read) : read;
				int length = in.read(lengBits);
				int ch = in.read();

				TripletWithFlag t = new TripletWithFlag(distance, length, ch, eof(read, length, ch));
				if (print) System.out.print(cnt++ + ") read " + t + (t==null?"\n":""));
				return t;
			}
		};
		setCloseable(reader);
	}

	@Override
	public boolean proccess() throws IOException {
		TripletWithFlag t = reader.read();
		if (t.flag) {
			int dist = t.getDistance();
			byte b = sequence.byteAt(idx - 1);	// take last added byte
			sequence.crop(dist + 1);			// shorten sequence by amount plus 1 for taken byte
			sequence.append(b);					// add last byte again
			if (ACB.print_trip)	System.out.println("\t<<< cropped by " + dist);
			return false;
		}
		int dist = t.getDistance();
		int leng = t.getLenght();
		byte b = t.getSymbol();
		int ctx = dictionary.searchContext(idx);
		int cnt = ctx - dist;
		int lcp = 0;
		if (ctx > 0) {
			int select = dictionary.select(cnt);
			lcp = dictionary.searchContent(ctx, select).getLcp();
			leng += lcp;
		}
//		lastLcp = lcp;
		if (ACB.print_trip)	System.out.println("\t[lcp = " + lcp + "]");
		if (leng > 0) {
			byte[] seq = dictionary.copy(cnt, leng);
			sequence.append(seq);
		}
		
		sequence.append(b);
		leng++;
		dictionary.update(idx, leng);
		idx += leng;
		if (ACB.print_dict) ACB.print(idx, sequence, ctx, cnt, leng, dictionary);
		return true;
	}
	
	private static class TripletWithFlag extends Triplet {

		private boolean flag;

		public TripletWithFlag(int distance, int lenght, byte b, boolean flag) {
			super(distance, lenght, b);
			this.flag = flag;
		}

		public TripletWithFlag(int distance, int lenght, int i, boolean flag) {
			super(distance, lenght, i);
			this.flag = flag;
		}
	}
}
