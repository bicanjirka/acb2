//package cz.cvut.fit.acb.triplets.encode;
//
//import cz.cvut.fit.acb.dictionary.ByteSequence;
//import cz.cvut.fit.acb.dictionary.Dictionary;
//import cz.cvut.fit.acb.dictionary.DictionaryInfo;
//import cz.cvut.fit.acb.triplets.TripletSupplier;
//
//import java.io.IOException;
//import java.io.OutputStream;
//
//public class LengthlessTripletEncoder extends BaseTripletEncoder {
//
//
//	public LengthlessTripletEncoder(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
//		super(sequence, dictionary);
//		this.writer = new BaseTripletWriter(distanceBits, lengthBits, out) {
//			@Override
//			public void write(TripletSupplier t) throws IOException {
//				if (print) System.out.println(cnt++ + ") write " + t);
//				out.write(t.getDistance(), distBits);
////				out.write(t.getLenght(), lengBits);
//				out.write(t.getSymbol());
//			}
//		};
//	}
//
//	@Override
//	public int process(int idx, DictionaryInfo info) throws IOException {
//		int ctx = info.getContext();
//		int cnt = info.getContent();
////		int leng = info.getLength();
//		int lcp = info.getLcp();
//
//		dictionary.update(idx, lcp + 1);
//		idx += lcp;
//		int dist = cnt == -1 ? 0 : ctx - cnt;
//		writer.write(new TripletSupplier(dist, 0, sequence.byteAt(idx)));
//		return idx + 1;
//	}
//}
