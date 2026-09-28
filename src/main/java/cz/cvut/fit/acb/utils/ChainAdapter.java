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
