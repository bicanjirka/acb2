package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;

import cz.cvut.fit.acb.dictionary.ByteArray;
import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import cz.cvut.fit.acb.triplets.decode.TripletDecoder;
import cz.cvut.fit.acb.triplets.encode.TripletEncoder;

public class ACB {

	public static boolean print_dict = false;
	public static boolean print_trip = true;
	private ACBProvider provider;

	public ACB(ACBProvider provider) {
		this.provider = provider;
	}

	public void compress(byte[] in, OutputStream out) {
		// http://www.javamex.com/tutorials/memory/ascii_charsequence.shtml
		// https://github.com/boonproject/boon/wiki/Auto-Growable-Byte-Buffer-like-a-ByteBuilder
		long time = System.currentTimeMillis();
		int idx = 0;
		ByteArray arr = new ByteArray(in);
		int ceiling = arr.length();
		Dictionary dict = provider.getDictionary(arr);

		try (TripletEncoder coder = provider.getCoder(arr, dict, out)) {
			
			// vylepseni:
			//✓1) do tripletu poslu delku contentu minus lcp s druhym nej contentem
			//✓2) do tripletu nebudu kodovat delku, ale budu ji predpokladat jako lcp viz 1
			// 3) 
			// dotazy: eof souboru, vyhledavat content pres opakovany select
			
			// genericky write
			// pri poslednim readu a writu se porvadi dictionary update zbytecne, prehazet poradi?
			
			while (idx < ceiling) {
				DictionaryInfo info = dict.search(idx);
				if (print_dict) print(idx, arr, info.getContext(), info.getContent(), info.getLength(), dict);
				idx = coder.proccess(idx, info); // vnorit search do coderu? a treba i celou smycku? nebo dat procces do hlavicky whilu?
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		long time2 = System.currentTimeMillis();
		
		String t = NumberFormat.getInstance().format(time2 - time);
		System.out.println("compress took "+t+" ms.");
	}

	public static void print(int idx, ByteSequence arr, int ctx, int cnt, int length, Dictionary dict) {
		System.out.println();
		System.out.println(new String(arr.array(0, idx)) + "|" + new String(arr.array(idx, arr.length())));
		System.out.println();
		System.out.println(dict);
		System.out.println("context " + ctx);
		System.out.println("content " + cnt);
		System.out.println("length  " + length);
	}

	public void decompress(InputStream in, OutputStream out) {
		long time = System.currentTimeMillis();
		ByteBuilder arr = new ByteBuilder();
		Dictionary dict = provider.getDictionary(arr);

		try (TripletDecoder decoder = provider.getDecoder(arr, dict, in)) {
			while (decoder.proccess()) {
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		long time2 = System.currentTimeMillis();
		String t = NumberFormat.getInstance().format(time2 - time);
		System.out.println("decompress took "+t+" ms.");

		try {
			out.write(arr.array());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
