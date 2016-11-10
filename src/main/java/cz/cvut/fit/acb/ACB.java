package cz.cvut.fit.acb;

import java.nio.ByteBuffer;

import cz.cvut.fit.acb.dictionary.ByteArray;
import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.TripletSupplier;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;
import cz.cvut.fit.acb.utils.ChainAdapter;
import cz.cvut.fit.acb.utils.Chainable;

/**
 * @author jiri.bican
 */
public class ACB {
	
	private ACBProvider provider;
	
	public ACB(ACBProvider provider) {
		this.provider = provider;
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
	
	public Chainable<ByteBuffer, TripletSupplier> compress() {
		return new ChainAdapter<>((byteBuffer, tripletSupplierConsumer) -> {
			ByteArray arr = new ByteArray(byteBuffer.array());
			Dictionary dict = provider.getDictionary(arr);
			TripletCoder coder = provider.getCoder(arr, dict);
			coder.encode(tripletSupplierConsumer);
		});
	}
	
	public Chainable<TripletProcessor, ByteBuffer> decompress() {
		return new ChainAdapter<>((tripletProcessor, byteBufferConsumer) -> {
			ByteBuilder arr = new ByteBuilder();
			Dictionary dict = provider.getDictionary(arr);
			
			TripletCoder coder = provider.getCoder(arr, dict);
			coder.decode(tripletProcessor);

//				long time2 = System.currentTimeMillis();
//				String t = NumberFormat.getInstance().format(time2 - time);
//				System.out.println("decompress took "+t+" ms.");
			
			ByteBuffer buffer = ByteBuffer.wrap(arr.array());
			byteBufferConsumer.accept(buffer);
		});
	}
}
