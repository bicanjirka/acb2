package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.AdaptiveArithmeticDecoder;
import cz.cvut.fit.acb.coding.AdaptiveArithmeticEncoder;
import cz.cvut.fit.acb.coding.ByteToTripletConverter;
import cz.cvut.fit.acb.coding.TripletToByteConverter;
import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryLCP;
import cz.cvut.fit.acb.triplets.decode.DefaultTripletDecoder;
import cz.cvut.fit.acb.triplets.decode.LCPTripletDecoder;
import cz.cvut.fit.acb.triplets.decode.Salomon2TripletDecoder;
import cz.cvut.fit.acb.triplets.decode.SalomonTripletDecoder;
import cz.cvut.fit.acb.triplets.decode.TripletDecoder;
import cz.cvut.fit.acb.triplets.decode.ValachTripletDecoder;
import cz.cvut.fit.acb.triplets.encode.DefaultTripletEncoder;
import cz.cvut.fit.acb.triplets.encode.Salomon2TripletEncoder;
import cz.cvut.fit.acb.triplets.encode.SalomonTripletEncoder;
import cz.cvut.fit.acb.triplets.encode.TripletEncoder;
import cz.cvut.fit.acb.triplets.encode.ValachTripletEncoder;

public class ACBProvider {
	private DictionaryProvider dictionary;
	private TripletEncoderProvider coder;
	private TripletDecoderProvider decoder;
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
//				coder = new TripletEncoderProvider() {
//					@Override
//					public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
//						return new LCPTripletEncoder(sequence, dictionary, distanceBits, lengthBits);
//					}
//				};
//				break;
//			case "lengthless":
//				coder = new TripletEncoderProvider() {
//					@Override
//					public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits) {
//						return new LengthlessTripletEncoder(sequence, dictionary, distanceBits, lengthBits);
//					}
//				};
//				break;
			case "salomon+":
				coder = Salomon2TripletEncoder::new;
				break;
			case "salomon":
				coder = SalomonTripletEncoder::new;
				break;
			case "valach":
				coder = ValachTripletEncoder::new;
				break;
			
			default:
				coder = DefaultTripletEncoder::new;
				break;
		}
		
		////////////////// DECODER
		switch (s) {
			case "lcp":
				decoder = LCPTripletDecoder::new;
				break;
//			case "lengthless":
//				decoder = LengthlessTripletDecoder::new;
//				break;
			case "salomon+":
				decoder = Salomon2TripletDecoder::new;
				break;
			case "salomon":
				decoder = SalomonTripletDecoder::new;
				break;
			case "valach":
				decoder = ValachTripletDecoder::new;
				break;
			
			default:
				decoder = DefaultTripletDecoder::new;
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
	
	public TripletEncoder getCoder(ByteSequence sequence, Dictionary dictionary) {
		return coder.provide(sequence, dictionary, distanceBits, lengthBits);
	}
	
	public TripletDecoder getDecoder(ByteBuilder sequence, Dictionary dictionary) {
		return decoder.provide(sequence, dictionary, distanceBits, lengthBits);
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
	
	interface TripletEncoderProvider {
		TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits);
	}
	
	interface TripletDecoderProvider {
		TripletDecoder provide(ByteBuilder sequence, Dictionary dictionary, int distanceBits, int lengthBits);
	}
	
	interface TripletToByteConverterProvider {
		TripletToByteConverter provide();
	}

	interface ByteToTripletConverterProvider {
		ByteToTripletConverter provide();
	}
}
