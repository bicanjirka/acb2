//package cz.cvut.fit.acb.triplets.decode;
//
//import java.io.IOException;
//
//import cz.cvut.fit.acb.dictionary.ByteBuilder;
//import cz.cvut.fit.acb.dictionary.Dictionary;
//import cz.cvut.fit.acb.triplets.TripletProcessor;
//import cz.cvut.fit.acb.triplets.TripletSupplier;
//import cz.cvut.fit.acb.utils.BitUtils;
//
//public class LengthlessTripletDecoder extends BaseTripletDecoder {
//
//	private BaseTripletReader<TripletSupplier> reader;
//
//	public LengthlessTripletDecoder(ByteBuilder sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
//		super(sequence, dictionary);
//		this.reader = new BaseTripletReader<TripletSupplier>(distanceBits, lengthBits, null) {
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
//	}
//
//	public boolean process() throws IOException {
//		TripletSupplier t = reader.read();
//		if (t == null)
//			return false;
////		int dist = t.getDistance();
////		byte b = t.getSymbol();
////		int ctx = dictionary.searchContext(idx);
////		int leng = dictionary.searchContent(ctx, idx).getContent();
////		int cnt = ctx - dist;
////
////		if (leng > 0) {
////			byte[] seq = dictionary.copy(cnt, leng);
////			sequence.append(seq);
////		}
////
////		sequence.append(b);
////		leng++;
////		dictionary.update(idx, leng);
////		idx += leng;
//		return true;
//	}
//
//	@Override
//	public void process(TripletProcessor input) {
//
//	}
//}
