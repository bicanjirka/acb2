package cz.cvut.fit.acb.triplets.decode;

import java.io.IOException;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.TripletProcessor;

public class LCPTripletDecoder extends BaseTripletDecoder {

	public LCPTripletDecoder(ByteBuilder sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
//		this.reader = new BaseTripletReader<TripletSupplier>(distanceBits, lengthBits, in) {
//			@Override
//			public TripletSupplier read() throws IOException {
//				int read = in.read(distBits);
//				int distance = BitUtils.isNegative(read, distBits) ? BitUtils.fillHighBits(read) : read;
//				int length = in.read(lengBits);
//				int ch = in.read();
//
//				TripletSupplier t = eof(read, length, ch) ? null : visitor -> {
//
//                };
//				if (print) System.out.print(cnt++ + ") read " + t + (t==null?"\n":""));
//				return t;
//			}
//		};
//		setCloseable(reader);
	}

	public boolean process() throws IOException {
//		TripletSupplier t = reader.read();
//		if (t == null)
//			return false;
		/*if (t.flag) {
			int dist = t.getDistance();
			byte b = sequence.byteAt(idx - 1);	// take last added byte
			sequence.crop(dist + 1);			// shorten sequence by amount plus 1 for taken byte
			sequence.append(b);					// add last byte again
			if (ACB.print_trip)	System.out.println("\t<<< cropped by " + dist);
			return false;
		}*/
//		int dist = t.getDistance();
//		int leng = t.getLenght();
//		byte b = t.getSymbol();
//		int ctx = dictionary.searchContext(idx);
//		int cnt = ctx - dist;
//		int lcp = 0;
//		if (ctx > 0) {
//			int idx = dictionary.select(cnt);
//			lcp = dictionary.searchContent(ctx, idx).getLcp(); // select index je spatne, v 7 tripletu pak vyjde length 2 minsto 1 (ii, ip)
////			lcp = ((DictionaryLCP) dictionary).searchLcp(ctx, idx, cnt);
//			leng += lcp;
//		}
//		if (ACB.print_trip)	System.out.println("\t[lcp = " + lcp + "]");
//		if (leng > 0) {
//			byte[] seq = dictionary.copy(cnt, leng);
//			sequence.append(seq);
//		}
//		if (ACB.print_dict) ACB.printInfo(idx, ctx, cnt, leng, dictionary);
//		sequence.append(b);
//		leng++;
//		dictionary.update(idx, leng);
//		idx += leng;
//		if (ACB.print_dict) ACB.printSeq(idx, sequence);
		return true;
	}

	@Override
	public void process(TripletProcessor input) {

	}
}
