/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author jiri.bican
 */
public class ChainAdapter<T, U> implements Chainable<T, U> {
	private final BiConsumer<T, Consumer<U>> biConsumer;
	private Consumer<U> uConsumer;
	
	public ChainAdapter(BiConsumer<T, Consumer<U>> consumer) {
		this.biConsumer = consumer;
	}
	
	@Override
	public void setConsumer(Consumer<U> consumer) {
		this.uConsumer = consumer;
	}
	
	@Override
	public void accept(T t) {
		biConsumer.accept(t, uConsumer);
	}
}
