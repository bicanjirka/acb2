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
public final class ACBProviderImpl implements ACBProvider {

	private final CompressionSettings settings;

	public ACBProviderImpl(CompressionSettings settings) {
		this.settings = settings;
	}

	@Override
	public Dictionary getDictionary(ByteSequence sequence) {
		int maxDistance = this.settings.maxDistance();
		int maxLength = this.settings.maxLength();
		return switch (this.settings.tripletCoding()) {
			case LCP -> new DictionaryLCP(this, sequence, maxDistance, maxLength);
			case SALOMON, SALOMON2, SIMPLE, VALACH -> new DictionaryBase(this, sequence, maxDistance, maxLength);
		};
	}

	@Override
	public TripletCoder getCoder(ByteSequence sequence, Dictionary dictionary) {
		int distanceBits = this.settings.distanceBits();
		int lengthBits = this.settings.lengthBits();
		return switch (this.settings.tripletCoding()) {
			case SALOMON -> new SalomonTripletCoder.SalomonByteless(sequence, dictionary, distanceBits, lengthBits);
			case SALOMON2 -> new SalomonTripletCoder.SalomonByteful(sequence, dictionary, distanceBits, lengthBits);
			case SIMPLE -> new SimpleTripletCoder(sequence, dictionary, distanceBits, lengthBits);
			case VALACH -> new ValachTripletCoder(sequence, dictionary, distanceBits, lengthBits);
			case LCP -> new LCPTripletCoder(sequence, dictionary, distanceBits, lengthBits);
		};
	}

	@Override
	public TripletToByteConverter<?> getT2BConverter() {
		return switch (this.settings.entropyCoding()) {
			case ADAPTIVE_ARITHMETIC -> new AdaptiveArithmeticEncoder(this.settings.lengthFrequencies());
			case BIT_ARRAY -> new BitArrayComposer();
		};
	}

	@Override
	public ByteToTripletConverter<?> getB2TConverter() {
		return switch (this.settings.entropyCoding()) {
			case ADAPTIVE_ARITHMETIC -> new AdaptiveArithmeticDecoder(this.settings.lengthFrequencies());
			case BIT_ARRAY -> new BitArrayDecomposer();
		};
	}

	@Override
	public <T> OrderStatisticTree<T> getOrderStatisticTree(Comparator<T> comparator) {
		return switch (this.settings.dictionaryStructure()) {
			case RED_BLACK -> new RedBlackBST<>(comparator);
			case BST -> new BST<>(comparator);
			case ST -> new BinarySearchST<>(comparator);
		};
	}
}
