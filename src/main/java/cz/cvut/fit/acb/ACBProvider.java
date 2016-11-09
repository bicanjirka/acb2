package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.AdaptiveArithmeticDecoder;
import cz.cvut.fit.acb.coding.AdaptiveArithmeticEncoder;
import cz.cvut.fit.acb.coding.ByteToTripletConverter;
import cz.cvut.fit.acb.coding.TripletToByteConverter;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryLCP;
import cz.cvut.fit.acb.triplets.coder.SalomonTripletCoder;
import cz.cvut.fit.acb.triplets.coder.SimpleTripletCoder;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;
import cz.cvut.fit.acb.triplets.coder.ValachTripletCoder;

public class ACBProvider {
	private DictionaryProvider dictionary;
	private TripletCoderProvider coder;
	private TripletToByteConverterProvider t2bConverter;
	private ByteToTripletConverterProvider b2tConverter;

	private int distanceBits;
	private int lengthBits;
	
	public ACBProvider(int dist, int leng, String s) {
		distanceBits = dist;
		lengthBits = leng;
		
		///////////// DICTIONARY
		switch (s) {
			case "lcp":
			case "lengthless":
				dictionary = DictionaryLCP::new;
				break;
			case "salomon+":
			case "salomon":
			case "valach":
			default:
				dictionary = Dictionary::new;
		}
		
		/////////////// ENCODER
		switch (s) {
//			case "lcp":
//				coder = new TripletCoderProvider() {
//					@Override
//					public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
//						return new LCPTripletEncoder(sequence, dictionary, distanceBits, lengthBits);
//					}
//				};
//				break;
//			case "lengthless":
//				coder = new TripletCoderProvider() {
//					@Override
//					public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
//						return new LengthlessTripletEncoder(sequence, dictionary, distanceBits, lengthBits);
//					}
//				};
//				break;
			case "salomon+":
				coder = SalomonTripletCoder.SalomonByteful::new;
				break;
			case "salomon":
				coder = SalomonTripletCoder.SalomonByteless::new;
				break;
			case "valach":
				coder = ValachTripletCoder::new;
				break;
			
			default:
				coder = SimpleTripletCoder::new;
				break;
		}
		
		/////////////// TRIPLET CODING STRATEGY
		t2bConverter = AdaptiveArithmeticEncoder::new;
		b2tConverter = AdaptiveArithmeticDecoder::new;
//		t2bConverter = BitArrayComposer::new;
//		b2tConverter = BitArrayDecomposer::new;
	}
	
	
	public Dictionary getDictionary(ByteSequence sequence) {
		return dictionary.provide(sequence, 1 << distanceBits - 2, 1 << lengthBits - 1);
	}

	public TripletCoder getCoder(ByteSequence sequence, Dictionary dictionary) {
		return coder.provide(sequence, dictionary, distanceBits, lengthBits);
	}
	
	public TripletToByteConverter<?> getT2BConverter() {
		return t2bConverter.provide();
	}

	public ByteToTripletConverter<?> getB2TConverter() {
		return b2tConverter.provide();
	}
	
	
	interface DictionaryProvider {
		Dictionary provide(ByteSequence sequence, int maxDistance, int maxLength);
	}

	interface TripletCoderProvider {
		TripletCoder provide(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits);
	}
	
	interface TripletToByteConverterProvider {
		TripletToByteConverter provide();
	}

	interface ByteToTripletConverterProvider {
		ByteToTripletConverter provide();
	}
}
