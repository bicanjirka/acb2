/*
 * Copyright 2002-2016 Ataccama Software, s.r.o. All rights reserved.
 * ATACCAMA PROPRIETARY/CONFIDENTIAL.
 * Any use of this source code is prohibited without prior written
 * permission of Ataccama Software, s.r.o.; Czech Republic, Id.no.: 28235550
 * http://www.ataccama.com
 */
package cz.cvut.fit.acb.utils;

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

    public <R> ChainBuilder<U, R, V> chain(Chainable<U, R> nextProcess) {
        process.setConsumer(nextProcess);
        return new ChainBuilder<>(nextProcess, firstProcess);
    }

    public Consumer<V> end(Consumer<U> lastProcess) {
        process.setConsumer(lastProcess);
        return firstProcess;
    }

    public static <T, U> ChainBuilder<T, U, T> create(Chainable<T, U> firstProcess) {
        return new ChainBuilder<>(firstProcess, firstProcess);
    }
}
