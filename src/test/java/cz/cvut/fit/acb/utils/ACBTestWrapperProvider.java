/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.utils;

import java.util.Comparator;
import java.util.function.Function;

import cz.cvut.fit.acb.ACBProvider;
import cz.cvut.fit.acb.coding.ByteToTripletConverter;
import cz.cvut.fit.acb.coding.TripletToByteConverter;
import cz.cvut.fit.acb.dictionary.ByteSequence;
import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.core.OrderStatisticTree;
import cz.cvut.fit.acb.triplets.coder.TripletCoder;

/**
 * @author jiri.bican
 */
public class ACBTestWrapperProvider implements ACBProvider {
	
	private ACBProvider delegate;
	
	private Function<Dictionary, Dictionary> dictionary;
	private Function<TripletCoder, TripletCoder> coder;
	private Function<TripletToByteConverter, TripletToByteConverter> t2b;
	private Function<ByteToTripletConverter, ByteToTripletConverter> b2t;
	private Function<OrderStatisticTree<?>, OrderStatisticTree<?>> ost;
	
	public ACBTestWrapperProvider(ACBProvider delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public Dictionary getDictionary(ByteSequence sequence) {
		return dictionary == null ?
				delegate.getDictionary(sequence) :
				dictionary.apply(delegate.getDictionary(sequence));
	}
	
	@Override
	public TripletCoder getCoder(ByteSequence sequence, Dictionary dictionary) {
		return coder == null ?
				delegate.getCoder(sequence, dictionary) :
				coder.apply(delegate.getCoder(sequence, dictionary));
	}
	
	@Override
	public TripletToByteConverter<?> getT2BConverter() {
		return t2b == null ?
				delegate.getT2BConverter() :
				t2b.apply(delegate.getT2BConverter());
	}
	
	@Override
	public ByteToTripletConverter<?> getB2TConverter() {
		return b2t == null ?
				delegate.getB2TConverter() :
				b2t.apply(delegate.getB2TConverter());
	}
	
	@Override
	public <T> OrderStatisticTree<T> getOrderStatisticTree(Comparator<T> comparator) {
		return ost == null ?
				delegate.getOrderStatisticTree(comparator) :
				(OrderStatisticTree<T>) ost.apply(delegate.getOrderStatisticTree(comparator));
	}
	
	
	public void wrapDictionary(Function<Dictionary, Dictionary> wrapper) {
		this.dictionary = wrapper;
	}
	
	public void wrapCoder(Function<TripletCoder, TripletCoder> wrapper) {
		this.coder = wrapper;
	}
	
	public void wrapT2BConverter(Function<TripletToByteConverter, TripletToByteConverter> wrapper) {
		this.t2b = wrapper;
	}
	
	public void wrapB2TConverter(Function<ByteToTripletConverter, ByteToTripletConverter> wrapper) {
		this.b2t = wrapper;
	}
	
	public void wrapOrderStatisticTree(Function<OrderStatisticTree<?>, OrderStatisticTree<?>> wrapper) {
		this.ost = wrapper;
	}
	
}
