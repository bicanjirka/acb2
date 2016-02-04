package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cz.cvut.fit.acb.dictionary.ByteArray;
import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletFactory;
import cz.cvut.fit.acb.triplets.TripletReader;
import cz.cvut.fit.acb.triplets.TripletWriter;

public class ACB {

	private Dictionary dict;
	private int dist;
	private int leng;
	private String tripletEncoding;
	
	private static boolean print = false;

	public ACB(int distance, int length, String tripletEncoding) {
		this.dist = distance;
		this.leng = length;
		this.tripletEncoding = tripletEncoding;
	}

	public void compress(byte[] in, OutputStream out) {
		// http://www.javamex.com/tutorials/memory/ascii_charsequence.shtml
		// https://github.com/boonproject/boon/wiki/Auto-Growable-Byte-Buffer-like-a-ByteBuilder
		int idx = 0;
		ByteArray txt = new ByteArray(in);
		int ceiling = txt.length();
		dict = new Dictionary(txt, dist, leng);

		try (TripletWriter writer = TripletFactory.getWriter(tripletEncoding, out, dist, leng)) {
			// TripletProvider bude interface factory constructor ktery na parametr triplet-coding vrati triplet providera a z toho dostanu writer/reader
			// a triplet strategy na zprocesovani tripletu ve slovniku
			// vylepseni:
			// 1) do tripletu poslu delku contentu minus lcp s druhym nej contentem
			// 2) do tripletu enbudu kodovat delku, ale budu ji predpokladat jako lcp viz 1
			// 3) 
			if (print) print(idx, txt.toString(), 0, -1, 0, dict);
			dict.update(idx, 1);
			writer.write(new Triplet(0, 0, in[idx]));
			idx++;
			while (idx < ceiling) {
				int ctx = dict.searchContext(idx);
				int[] cntArr = dict.searchContent(ctx, idx);
				int cnt = cntArr[0];
				int leng = cntArr[1];
				if (print) print(idx, txt.toString(), ctx, cnt, leng, dict);
				
				if ("default".equals(tripletEncoding)) {
					dict.update(idx, leng + 1);
					idx += leng;
					int dist = cnt == -1 ? 0 : ctx - cnt;
					writer.write(new Triplet(dist, leng, in[idx]));
					idx++;
				}
				
				if ("salomon".equals(tripletEncoding)) {
					int dist = cnt == -1 ? 0 : ctx - cnt;
					if (dist == 0 && leng == 0) {
						// flag 0
						dict.update(idx, 1);
						writer.write(new Triplet(0, 0, in[idx]));
						idx++;
					} else {
						// flag 1
						dict.update(idx, leng);
						writer.write(new Triplet(dist, leng, (byte) 0));
						idx += leng;
					}
				}
				
				if ("salomon+".equals(tripletEncoding)) {
					int dist = cnt == -1 ? 0 : ctx - cnt;
					if (dist == 0 && leng == 0) {
						// flag 0
						dict.update(idx, 1);
						writer.write(new Triplet(0, 0, in[idx]));
						idx++;
					} else {
						// flag 1
						dict.update(idx, leng + 1);
						idx += leng;
						writer.write(new Triplet(dist, leng, in[idx]));
						idx++;
					}
				}
				
				if ("valach".equals(tripletEncoding)) {
					int dist = cnt == -1 ? 0 : ctx - cnt;
					if (leng == 0) {
						dict.update(idx, 1);
						writer.write(new Triplet(0, 0, in[idx]));
						idx++;
					} else {
						dict.update(idx, leng + 1);
						idx += leng;
						writer.write(new Triplet(dist, leng, in[idx]));
						idx++;
					}
				}
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
		ByteBuilder txt = new ByteBuilder();
		dict = new Dictionary(txt, dist, leng);

		try (TripletReader reader = TripletFactory.getReader(tripletEncoding, in, dist, leng)) {
			Triplet t;
			while ((t = reader.read()) != null) {
				int dist = t.getDistance();
				int leng = t.getLenght();
				byte b = t.getSymbol();
				int ctx = dict.searchContext(idx);
				int cnt = ctx - dist;
				
				if (leng > 0) {
					byte[] seq = dict.copy(cnt, leng);
					txt.append(seq);
				}
				
				if ("default".equals(tripletEncoding)) {
					txt.append(b);
					leng++;
					dict.update(idx, leng);
					idx += leng;
				}
				
				if ("salomon".equals(tripletEncoding)) {
					if (dist == 0 && leng == 0) {
						// flag 0
						txt.append(b);
						dict.update(idx, 1);
						idx++;
					} else {
						// flag 1
						dict.update(idx, leng);
						idx += leng;
					}
				}
				
				if ("salomon+".equals(tripletEncoding)) {
					if (dist == 0 && leng == 0) {
						// flag 0
						txt.append(b);
						dict.update(idx, 1);
						idx++;
					} else {
						// flag 1
						txt.append(b);
						leng++;
						dict.update(idx, leng);
						idx += leng;
					}
				}
				
				if ("valach".equals(tripletEncoding)) {
					if (leng == 0) {
						txt.append(b);
						dict.update(idx, 1);
						idx++;
					} else {
						txt.append(b);
						leng++;
						dict.update(idx, leng);
						idx += leng;
					}
				}
				if (print) print(idx, txt.toString(), ctx, cnt, leng, dict);
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
