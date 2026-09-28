package cz.cvut.fit.acb.fixtures;

import java.util.Comparator;
import java.util.function.UnaryOperator;

import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.coding.ByteToTripletConverter;
import cz.cvut.fit.acb.coding.TripletToByteConverter;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.core.OrderStatisticTree;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;

/** Delegates everything, passing each new dictionary through {@code wrap} first. */
public final class DictionaryWrappingProvider implements ACBProvider {

	private final ACBProvider delegate;
	private final UnaryOperator<Dictionary> wrap;

	public DictionaryWrappingProvider(ACBProvider delegate, UnaryOperator<Dictionary> wrap) {
		this.delegate = delegate;
		this.wrap = wrap;
	}

	@Override
	public Dictionary getDictionary(ByteSequence sequence) {
		return this.wrap.apply(this.delegate.getDictionary(sequence));
	}

	@Override
	public TripletCoder getCoder(ByteSequence sequence, Dictionary dictionary) {
		return this.delegate.getCoder(sequence, dictionary);
	}

	@Override
	public TripletToByteConverter<?> getT2BConverter() {
		return this.delegate.getT2BConverter();
	}

	@Override
	public ByteToTripletConverter<?> getB2TConverter() {
		return this.delegate.getB2TConverter();
	}

	@Override
	public <T> OrderStatisticTree<T> getOrderStatisticTree(Comparator<T> comparator) {
		return this.delegate.getOrderStatisticTree(comparator);
	}
}
