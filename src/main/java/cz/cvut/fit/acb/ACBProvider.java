package cz.cvut.fit.acb;

import java.io.InputStream;
import java.io.OutputStream;

import cz.cvut.fit.acb.dictionary.ByteBuilder;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryLCP;
import cz.cvut.fit.acb.triplets.decode.DefaultTripletDecoder;
import cz.cvut.fit.acb.triplets.decode.LCPTripletDecoder;
import cz.cvut.fit.acb.triplets.decode.LengthlessTripletDecoder;
import cz.cvut.fit.acb.triplets.decode.Salomon2TripletDecoder;
import cz.cvut.fit.acb.triplets.decode.SalomonTripletDecoder;
import cz.cvut.fit.acb.triplets.decode.TripletDecoder;
import cz.cvut.fit.acb.triplets.decode.ValachTripletDecoder;
import cz.cvut.fit.acb.triplets.encode.DefaultTripletEncoder;
import cz.cvut.fit.acb.triplets.encode.LCPTripletEncoder;
import cz.cvut.fit.acb.triplets.encode.LengthlessTripletEncoder;
import cz.cvut.fit.acb.triplets.encode.Salomon2TripletEncoder;
import cz.cvut.fit.acb.triplets.encode.SalomonTripletEncoder;
import cz.cvut.fit.acb.triplets.encode.TripletEncoder;
import cz.cvut.fit.acb.triplets.encode.ValachTripletEncoder;

public class ACBProvider {
	// nemel bych udelat encoding providera a decoding providera, kteri by mi teprve vraceli spravne classy?
	private DictionaryProvider dictionary;
	private TripletEncoderProvider coder;
	private TripletDecoderProvider decoder;
	
	private int distanceBits;
	private int lengthBits;
	
	public ACBProvider(int dist, int leng, String s) {
		distanceBits = dist;
		lengthBits = leng;
		
		///////////// DICTIONARY
		switch (s) {
		case "lcp":
		case "lengthless":
			dictionary = new DictionaryProvider() {
				@Override
				public Dictionary provide(ByteSequence sequence, int maxDistance, int maxLength) {
					return new DictionaryLCP(sequence, maxDistance, maxLength);
				}
			};
			break;
		case "salomon+":
		case "salomon":
		case "valach":
		default:
			dictionary = new DictionaryProvider() {
				@Override
				public Dictionary provide(ByteSequence sequence, int maxDistance, int maxLength) {
					return new Dictionary(sequence, maxDistance, maxLength);
				}
			};
		}
		
		/////////////// ENCODER
		switch (s) {
		case "lcp":
			coder = new TripletEncoderProvider() {
				@Override
				public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
					return new LCPTripletEncoder(sequence, dictionary, out, distanceBits, lengthBits);
				}
			};
			break;
		case "lengthless":
			coder = new TripletEncoderProvider() {
				@Override
				public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
					return new LengthlessTripletEncoder(sequence, dictionary, out, distanceBits, lengthBits);
				}
			};
			break;
		case "salomon+":
			coder = new TripletEncoderProvider() {
				@Override
				public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
					return new Salomon2TripletEncoder(sequence, dictionary, out, distanceBits, lengthBits);
				}
			};
			break;
		case "salomon":
			coder = new TripletEncoderProvider() {
				@Override
				public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
					return new SalomonTripletEncoder(sequence, dictionary, out, distanceBits, lengthBits);
				}
			};
			break;
		case "valach":
			coder = new TripletEncoderProvider() {
				@Override
				public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
					return new ValachTripletEncoder(sequence, dictionary, out, distanceBits, lengthBits);
				}
			};
			break;

		default:
			coder = new TripletEncoderProvider() {
				@Override
				public TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits) {
					return new DefaultTripletEncoder(sequence, dictionary, out, distanceBits, lengthBits);
				}
			};
			break;
		}
		
		////////////////// DECODER
		switch (s) {
		case "lcp":
			decoder = new TripletDecoderProvider() {
				@Override
				public TripletDecoder provide(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits) {
					return new LCPTripletDecoder(sequence, dictionary, in, distanceBits, lengthBits);
				}
			};
			break;
		case "lengthless":
			decoder = new TripletDecoderProvider() {
				@Override
				public TripletDecoder provide(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits) {
					return new LengthlessTripletDecoder(sequence, dictionary, in, distanceBits, lengthBits);
				}
			};
			break;
		case "salomon+":
			decoder = new TripletDecoderProvider() {
				@Override
				public TripletDecoder provide(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits) {
					return new Salomon2TripletDecoder(sequence, dictionary, in, distanceBits, lengthBits);
				}
			};
			break;
		case "salomon":
			decoder = new TripletDecoderProvider() {
				@Override
				public TripletDecoder provide(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits) {
					return new SalomonTripletDecoder(sequence, dictionary, in, distanceBits, lengthBits);
				}
			};
			break;
		case "valach":
			decoder = new TripletDecoderProvider() {
				@Override
				public TripletDecoder provide(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits) {
					return new ValachTripletDecoder(sequence, dictionary, in, distanceBits, lengthBits);
				}
			};
			break;

		default:
			decoder = new TripletDecoderProvider() {
				@Override
				public TripletDecoder provide(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits) {
					return new DefaultTripletDecoder(sequence, dictionary, in, distanceBits, lengthBits);
				}
			};
			break;
		}
	}
	
	
	
	public Dictionary getDictionary(ByteSequence sequence) {
		return dictionary.provide(sequence, 1 << distanceBits - 2, 1 << lengthBits - 1);
	}
	public TripletEncoder getCoder(ByteSequence sequence, Dictionary dictionary, OutputStream out) {
		return coder.provide(sequence, dictionary, out, distanceBits, lengthBits);
	}
	public TripletDecoder getDecoder(ByteBuilder sequence, Dictionary dictionary, InputStream in) {
		return decoder.provide(sequence, dictionary, in, distanceBits, lengthBits);
	}
	
	
	
	interface DictionaryProvider {
		Dictionary provide(ByteSequence sequence, int maxDistance, int maxLength);
	}
	interface TripletEncoderProvider {
		TripletEncoder provide(ByteSequence sequence, Dictionary dictionary, OutputStream out, int distanceBits, int lengthBits);
	}
	interface TripletDecoderProvider {
		TripletDecoder provide(ByteBuilder sequence, Dictionary dictionary, InputStream in, int distanceBits, int lengthBits);
	}
}
