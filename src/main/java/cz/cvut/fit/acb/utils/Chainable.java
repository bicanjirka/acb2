package cz.cvut.fit.acb.utils;

import java.util.function.Consumer;

/**
 * @author jiri.bican
 */
public interface Chainable<T, U> extends Consumer<T> {
	void setConsumer(Consumer<U> consumer);
}
