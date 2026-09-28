package cz.cvut.fit.acb.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author jiri.bican
 */
public class ChainBuilder<T, U, V> {
	private Chainable<T, U> process;
	private Consumer<V> firstProcess;
	
	private ChainBuilder(Chainable<T, U> process, Consumer<V> fistProcess) {
		this.process = process;
		this.firstProcess = fistProcess;
	}
	
	public static <T, U> ChainBuilder<T, U, T> create(Chainable<T, U> firstProcess) {
		return new ChainBuilder<>(firstProcess, firstProcess);
	}
	
	public static <T, U> ChainBuilder<T, U, T> create(BiConsumer<T, Consumer<U>> first) {
		return create(new ChainAdapter<T, U>(first));
	}
	
	
	public <R> ChainBuilder<U, R, V> chain(Chainable<U, R> nextProcess) {
		process.setConsumer(nextProcess);
		return new ChainBuilder<>(nextProcess, firstProcess);
	}
	
	public <R> ChainBuilder<U, R, V> chain(BiConsumer<U, Consumer<R>> next) {
		return chain(new ChainAdapter<U, R>(next));
	}
	
	
	public Consumer<V> end(Consumer<U> lastProcess) {
		process.setConsumer(lastProcess);
		return firstProcess;
	}
}
