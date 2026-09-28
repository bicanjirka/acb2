package cz.cvut.fit.acb.fixtures;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.ACBProviderImpl;
import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.triplets.TripletProcessor;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;

/** The real components, with each dictionary passed through {@code dictionaries} and triplets through a log. */
public final class InterceptingProvider implements ACBProvider {
	
	private final ACBProvider delegate;
	private final UnaryOperator<Dictionary> dictionaries;
	private final TripletLog log;
	
	public InterceptingProvider(ACBProvider delegate, UnaryOperator<Dictionary> dictionaries, TripletLog log) {
		this.delegate = delegate;
		this.dictionaries = dictionaries;
		this.log = log;
	}
	
	/** For {@code new Compressor(settings, components(...))}. */
	public static Function<CompressionSettings, ACBProvider> components(UnaryOperator<Dictionary> dictionaries,
	                                                                   TripletLog log) {
		return settings -> new InterceptingProvider(new ACBProviderImpl(settings), dictionaries, log);
	}
	
	@Override
	public Dictionary getDictionary(ByteSequence sequence) {
		return this.dictionaries.apply(this.delegate.getDictionary(sequence));
	}
	
	@Override
	public TripletCoder getCoder(ByteSequence sequence, Dictionary dictionary) {
		return this.delegate.getCoder(sequence, dictionary);
	}
	
	@Override
	public TripletWriter getTripletWriter() {
		return this.log.recording(this.delegate.getTripletWriter());
	}
	
	@Override
	public TripletProcessor getTripletReader(List<byte[]> payload) {
		return this.log.checking(this.delegate.getTripletReader(payload));
	}
	
}
