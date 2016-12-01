/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import cz.cvut.fit.acb.dictionary.Dictionary;
import cz.cvut.fit.acb.dictionary.DictionaryInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author jiri.bican
 */
public class ACBTestDictionary implements Dictionary {
	private Dictionary delegate;
	private List<Dictionary> stored = new ArrayList<>();
	private Consumer<Dictionary> strategy;
	
	public ACBTestDictionary() {
	}
	
	@Override
	public Dictionary clone() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public byte[] copy(int cnt, int leng) {
		return delegate.copy(cnt, leng);
	}
	
	@Override
	public DictionaryInfo search(int idx) {
		return delegate.search(idx);
	}
	
	@Override
	public DictionaryInfo searchContent(int ctx, int idx) {
		return delegate.searchContent(ctx, idx);
	}
	
	@Override
	public int searchContext(int idx) {
		return delegate.searchContext(idx);
	}
	
	@Override
	public void update(int idx, int count) {
		delegate.update(idx, count);
		strategy.accept(delegate.clone());
	}
	
	public Dictionary store(Dictionary delegate) {
		this.delegate = delegate;
		stored.clear();
		strategy = stored::add;
		return this;
	}
	
	public Dictionary test(Dictionary delegate) {
		this.delegate = delegate;
		Iterator<Dictionary> iterator = stored.iterator();
		strategy = dict -> {
			assertTrue(iterator.hasNext());
			Dictionary next = iterator.next();
			assertEquals(next, dict);
		};
		return this;
	}
}
