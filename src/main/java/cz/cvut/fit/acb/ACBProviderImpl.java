package cz.cvut.fit.acb;

import java.util.Comparator;

import cz.cvut.fit.acb.coding.AdaptiveArithmeticDecoder;
import cz.cvut.fit.acb.coding.AdaptiveArithmeticEncoder;
import cz.cvut.fit.acb.coding.BitArrayComposer;
import cz.cvut.fit.acb.coding.BitArrayDecomposer;
import cz.cvut.fit.acb.coding.ByteToTripletConverter;
import cz.cvut.fit.acb.coding.TripletToByteConverter;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryBase;
import cz.cvut.fit.acb.dictionary.DictionaryLCP;
import cz.cvut.fit.acb.dictionary.core.BST;
import cz.cvut.fit.acb.dictionary.core.BinarySearchST;
import cz.cvut.fit.acb.dictionary.core.OrderStatisticTree;
import cz.cvut.fit.acb.dictionary.core.RedBlackBST;
import cz.cvut.fit.acb.triplets.coder.LCPTripletCoder;
import cz.cvut.fit.acb.triplets.coder.SalomonTripletCoder;
import cz.cvut.fit.acb.triplets.coder.SimpleTripletCoder;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;
import cz.cvut.fit.acb.triplets.coder.ValachTripletCoder;

/**
 * @author jiri.bican
 */
public class ACBProviderImpl implements ACBProvider {
	public final int distanceBits;
	public final int lengthBits;
	
	private DictionaryProvider dictionary;
	private TripletCoderProvider coder;
	private TripletToByteConverterProvider t2bConverter;
	private ByteToTripletConverterProvider b2tConverter;
	private OrderStatisticTreeProvider orderStatisticTree;
	
	public ACBProviderImpl(ACBProviderParameters params) {
		
		distanceBits = params.distanceBits;
		lengthBits = params.lengthBits;
		
		switch (params.tc) {
			case LCP:
				dictionary = DictionaryLCP::new;
				break;
			case SALOMON:
			case SALOMON2:
			case SIMPLE:
			case VALACH:
			default:
				dictionary = DictionaryBase::new;
		}
		
		switch (params.tc) {
			case SALOMON:
				coder = SalomonTripletCoder.SalomonByteless::new;
				break;
			case SALOMON2:
				coder = SalomonTripletCoder.SalomonByteful::new;
				break;
			case VALACH:
				coder = ValachTripletCoder::new;
				break;
			case LCP:
				coder = LCPTripletCoder::new;
				break;
			case SIMPLE:
			default:
				coder = SimpleTripletCoder::new;
				break;
		}
		
		switch (params.cd) {
			case ADAPTIVE_ARITHMETIC:
				t2bConverter = AdaptiveArithmeticEncoder::new;
				b2tConverter = AdaptiveArithmeticDecoder::new;
				break;
			case BIT_ARRAY:
				t2bConverter = BitArrayComposer::new;
				b2tConverter = BitArrayDecomposer::new;
				break;
		}
		
		switch (params.tr) {
			case RED_BLACK:
				orderStatisticTree = RedBlackBST::new;
				break;
			case BST:
				orderStatisticTree = BinarySearchST::new;
				break;
			case ST:
				orderStatisticTree = BST::new;
				break;
		}
		
	}
	
	
	@Override
	public Dictionary getDictionary(ByteSequence sequence) {
		return dictionary.provide(this, sequence, 1 << (distanceBits - 1), (1 << lengthBits) - 1);
	}
	
	@Override
	public TripletCoder getCoder(ByteSequence sequence, Dictionary dictionary) {
		return coder.provide(sequence, dictionary, distanceBits, lengthBits);
	}
	
	@Override
	public TripletToByteConverter<?> getT2BConverter() {
		return t2bConverter.provide();
	}
	
	@Override
	public ByteToTripletConverter<?> getB2TConverter() {
		return b2tConverter.provide();
	}
	
	@Override
	public <T> OrderStatisticTree<T> getOrderStatisticTree(Comparator<T> comparator) {
		return orderStatisticTree.provide(comparator);
	}
	
	
	private interface DictionaryProvider {
		Dictionary provide(ACBProvider p, ByteSequence sequence, int maxDistance, int maxLength);
	}
	
	private interface TripletCoderProvider {
		TripletCoder provide(ByteSequence sequence, Dictionary dictionary, int distanceBits, int lengthBits);
	}
	
	private interface TripletToByteConverterProvider {
		TripletToByteConverter provide();
	}
	
	private interface ByteToTripletConverterProvider {
		ByteToTripletConverter provide();
	}
	
	private interface OrderStatisticTreeProvider {
		<K> OrderStatisticTree<K> provide(Comparator<K> comparator);
	}
}
