package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletFactory;
import cz.cvut.fit.acb.triplets.TripletReader;
import cz.cvut.fit.acb.triplets.TripletWriter;

public class ACB {

	private Dictionary dict;
	private int dist;
	private int leng;

	public ACB(int distance, int length) {
		this.dist = distance;
		this.leng = length;
	}

	public void compress(byte[] in, OutputStream out) {
		// http://www.javamex.com/tutorials/memory/ascii_charsequence.shtml
		int idx = 0;
		String txt = new String(in, Charset.defaultCharset());
		int ceiling = txt.length();
		dict = new Dictionary(txt, dist, leng);

		try (TripletWriter writer = TripletFactory.getWriter("default", out, dist, leng)) {
			dict.update(idx, 1);
			writer.write(new Triplet(0, 0, (char) in[idx]));
			idx++;

			while (idx < ceiling) {
				int ctx = dict.searchContext(idx);
				int[] cntArr = dict.searchContent(ctx, idx);
				int cnt = cntArr[0];
				int len = cntArr[1];
				// print(idx, txt, ctx, cnt, len);
				dict.update(idx, len + 1);
				idx += len;
				int dist = cnt == -1 ? 0 : cnt - ctx;
				writer.write(new Triplet(dist, len, (char) in[idx]));
				idx++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void print(int idx, String text, int ctx, int cnt, int length, Dictionary dict) {
		System.out.println();
		System.out.println(text.substring(0, idx) + "|" + text.substring(idx));
		System.out.println();
		System.out.println(dict);
		System.out.println("context " + ctx);
		System.out.println("content " + cnt);
		System.out.println("length  " + length);
	}

	public void decompress(InputStream in, OutputStream out) {
		int idx = 0;
		StringBuffer txt = new StringBuffer();
		dict = new Dictionary(txt, dist, leng);

		try (TripletReader reader = TripletFactory.getReader("default", in, dist, leng)) {
			Triplet t;
			while ((t = reader.read()) != null) {
				int dist = t.getDistance();
				int leng = t.getLenght();
				char ch = t.getSymbol();
				int ctx = dict.searchContext(idx);
				int cnt = dist + ctx;
				CharSequence seq;
				if (leng > 0) {
					seq = dict.copy(cnt, leng);
					txt.append(seq);
				}
				txt.append(ch);
				leng++;
				dict.update(idx, leng);
				idx += leng;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			out.write(txt.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Dictionary getDictionary() {
		return dict;
	}

}
