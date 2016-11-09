package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.NumberFormat;

import cz.cvut.fit.acb.dictionary.ByteArray;
import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.triplets.decode.TripletDecoder;
import cz.cvut.fit.acb.triplets.encode.TripletEncoder;
import cz.cvut.fit.acb.utils.ChainAdapter;
import cz.cvut.fit.acb.utils.Chainable;

public class ACB {

	public static boolean print_dict = false;
	public static boolean print_trip = false;
	private ACBProvider provider;

	public ACB(ACBProvider provider) {
		this.provider = provider;
	}

	public void compress(byte[] in, OutputStream out) {
		// http://www.javamex.com/tutorials/memory/ascii_charsequence.shtml
		// https://github.com/boonproject/boon/wiki/Auto-Growable-Byte-Buffer-like-a-ByteBuilder
		// pri poslednim readu a writu se porvadi dictionary update zbytecne, prehazet poradi?
		long time = System.currentTimeMillis();
		ByteArray arr = new ByteArray(in);
		Dictionary dict = provider.getDictionary(arr);
		
		TripletEncoder coder = provider.getCoder(arr, dict);

		provider.getT2BConverter();
		
//		while (idx < ceiling) {
//			DictionaryInfo info = dict.search(idx);
//			if (print_dict) print(idx, arr, info.getContext(), info.getContent(), info.getLength(), dict);
//			idx = coder.process(idx, info);
//		}
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
	
	public static void printSeq(int idx, ByteSequence arr) {
		System.out.println();
		System.out.println(new String(arr.array(0, idx)) + "|" + new String(arr.array(idx, arr.length())));
		System.out.println();
	}

	public static void printInfo(int idx, int ctx, int cnt, int length, Dictionary dict) {
		System.out.println();
		System.out.println(dict);
		System.out.println("context " + ctx);
		System.out.println("content " + cnt);
		System.out.println("length  " + length);
		System.out.println("append  " + (length > 0 ? new String(dict.copy(cnt, length)) : "nothing"));
	}

	public void decompress(InputStream in, OutputStream out) {
		long time = System.currentTimeMillis();
		ByteBuilder arr = new ByteBuilder();
		Dictionary dict = provider.getDictionary(arr);

//		try (TripletDecoder decoder = provider.getDecoder(arr, dict, in)) {
//			while (decoder.process()) {
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		long time2 = System.currentTimeMillis();
		String t = NumberFormat.getInstance().format(time2 - time);
		System.out.println("decompress took "+t+" ms.");

		try {
			out.write(arr.array());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Chainable<ByteBuffer, TripletSupplier> compress() {
		return new ChainAdapter<>((byteBuffer, tripletSupplierConsumer) -> {
			ByteArray arr = new ByteArray(byteBuffer.array());
			Dictionary dict = provider.getDictionary(arr);
			TripletEncoder coder = provider.getCoder(arr, dict);
			coder.process(tripletSupplierConsumer);
		});
	}

	public Chainable<TripletProcessor, ByteBuffer> decompress() {
		return new ChainAdapter<>((tripletProcessor, byteBufferConsumer) -> {
			ByteBuilder arr = new ByteBuilder();
			Dictionary dict = provider.getDictionary(arr);
			
			TripletDecoder decoder = provider.getDecoder(arr, dict);
			decoder.process(tripletProcessor);

//				long time2 = System.currentTimeMillis();
//				String t = NumberFormat.getInstance().format(time2 - time);
//				System.out.println("decompress took "+t+" ms.");
			
			ByteBuffer buffer = ByteBuffer.wrap(arr.array());
			byteBufferConsumer.accept(buffer);
		});
	}
}
