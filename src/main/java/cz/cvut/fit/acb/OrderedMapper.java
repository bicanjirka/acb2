package cz.cvut.fit.acb;

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Maps a feed of items on an executor it does not own, at most {@code window} at a time, and hands
 * the results to a sink in the order of the feed, on the calling thread. The feed is read and the
 * sink called only by the caller, so neither needs to be thread-safe; the step runs on the
 * executor and must be. {@link #sequential()} runs each step on the calling thread, one item at a
 * time.
 */
public final class OrderedMapper {

    /** Items one at a time until none is left. */
    @FunctionalInterface
    public interface Feed<T, E extends Exception> {

        Optional<T> next() throws E;

        static <T> Feed<T, RuntimeException> of(Iterator<T> items) {
            return () -> items.hasNext() ? Optional.of(items.next()) : Optional.empty();
        }
    }

    /** What is done to each item. */
    @FunctionalInterface
    public interface Step<T, R, E extends Exception> {

        R apply(T item) throws E;
    }

    /** Carries a step's checked exception through {@link CompletableFuture}, which only knows unchecked ones. */
    private static final class Checked extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        private Checked(Exception cause) {
            super(cause);
        }
    }

    private static final OrderedMapper SEQUENTIAL = new OrderedMapper(Runnable::run, 1);

    private final Executor executor;
    private final int window;

    private OrderedMapper(Executor executor, int window) {
        this.executor = executor;
        this.window = window;
    }

    public static OrderedMapper sequential() {
        return SEQUENTIAL;
    }

    /** @param window how many items may be in flight or waiting for their turn, at least 1 */
    public static OrderedMapper on(Executor executor, int window) {
        if (window < 1) {
            throw new IllegalArgumentException("window must be at least one: " + window);
        }
        return new OrderedMapper(executor, window);
    }

    /**
     * Stops at the first failure, from the feed, a step or the sink, and throws it; items not yet
     * handed on are dropped, and steps already running finish on their own.
     */
    public <T, R, E extends Exception> void map(Feed<T, E> feed, Step<T, R, E> step, Consumer<R> sink) throws E {
        Deque<CompletableFuture<R>> pending = new ArrayDeque<>();
        try {
            boolean more = true;
            while (true) {
                while (more && pending.size() < this.window) {
                    Optional<T> item = feed.next();
                    if (item.isPresent()) {
                        pending.add(this.submit(item.get(), step));
                    } else {
                        more = false;
                    }
                }
                if (pending.isEmpty()) {
                    return;
                }
                sink.accept(await(pending.remove()));
            }
        } finally {
            pending.forEach(future -> future.cancel(false));
        }
    }

    private <T, R, E extends Exception> CompletableFuture<R> submit(T item, Step<T, R, E> step) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return step.apply(item);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new Checked(e);
            }
        }, this.executor);
    }

    @SuppressWarnings("unchecked")
    private static <R, E extends Exception> R await(CompletableFuture<R> future) throws E {
        try {
            return future.join();
        } catch (CompletionException e) {
            switch (e.getCause()) {
                case Checked checked -> throw (E) checked.getCause();
                case RuntimeException failure -> throw failure;
                case Error failure -> throw failure;
                default -> throw e;
            }
        }
    }
}
