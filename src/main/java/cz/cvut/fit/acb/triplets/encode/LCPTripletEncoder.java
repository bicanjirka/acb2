package cz.cvut.fit.acb.triplets.encode;

import java.io.IOException;
import java.io.OutputStream;

import cz.cvut.fit.acb.ACB;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.Triplet;

public class LCPTripletEncoder extends BaseTripletEncoder {

	private int distanceBits;

	public LCPTripletEncoder(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
		super(sequence, dictionary);
		this.distanceBits = distanceBits;
		this.writer = new BaseTripletWriterExtension(distanceBits, lengthBits, out);
	}

	@Override
	public int proccess(int idx, DictionaryInfo info) throws IOException {
		int ctx = info.getContext();
		int cnt = info.getContent();
		int leng = info.getLength();
		int lcp = info.getLcp();

		dictionary.update(idx, leng + lcp + 1);
		idx += leng + lcp;
		int dist = cnt == -1 ? 0 : ctx - cnt;
		writer.write(new Triplet(dist, leng, sequence.byteAt(idx)));
		if (ACB.print_trip) System.out.println("\t[lcp = " + lcp + "]");
		if (idx + 1 >= sequence.length()) {
			((BaseTripletWriterExtension) writer).write((1 << distanceBits - 2) - lcp + 1);
		}
		return idx + 1;
	}

	private static class BaseTripletWriterExtension extends BaseTripletWriter {
		private BaseTripletWriterExtension(int distBits, int lengBits, OutputStream out) {
			super(distBits, lengBits, out);
		}
	
		@Override
		public void write(Triplet t) throws IOException {
			if (print) System.out.print(cnt++ + ") write " + t);
			out.write(t.getDistance(), distBits);
			out.write(t.getLenght(), lengBits);
			out.write(t.getSymbol());
		}
	
		public void write(int i) throws IOException {
			if (print) System.out.println(cnt++ + ") write " + i);
			out.write(i, distBits);
		}
	}
}
