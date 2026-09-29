package cz.cvut.fit.acb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(5)
class OrderedMapperTest {

    private static OrderedMapper.Feed<Integer, RuntimeException> counting(int count) {
        return OrderedMapper.Feed.of(IntStream.range(0, count).iterator());
    }

    @Test
    void resultsArriveInTheOrderOfTheFeedEvenWhenLaterItemsFinishFirst() {
        CountDownLatch laterOnesDone = new CountDownLatch(2);
        List<Integer> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(3)) {
            OrderedMapper.on(pool, 3).map(counting(3), item -> {
                if (item == 0) {
                    await(laterOnesDone);
                } else {
                    laterOnesDone.countDown();
                }
                return item * 10;
            }, results::add);
        }

        assertThat(results).containsExactly(0, 10, 20);
    }

    @Test
    void noMoreItemsAreReadThanTheWindowHoldsBeforeTheFirstResultIsHandedOn() {
        List<String> log = new ArrayList<>();
        OrderedMapper.Feed<Integer, RuntimeException> feed = OrderedMapper.Feed.of(
                IntStream.range(0, 4).peek(item -> log.add("read " + item)).iterator());

        OrderedMapper.on(Runnable::run, 2).map(feed, item -> item, result -> log.add("got " + result));

        assertThat(log).containsExactly("read 0", "read 1", "got 0", "read 2", "got 1", "read 3", "got 2", "got 3");
    }

    @Test
    void theSequentialMapperRunsEveryStepOnTheCallingThread() {
        List<Thread> threads = new ArrayList<>();

        OrderedMapper.sequential().map(counting(3), item -> Thread.currentThread(), threads::add);

        assertThat(threads).containsOnly(Thread.currentThread()).hasSize(3);
    }

    @Test
    void aMapperOnAPoolRunsNoStepOnTheCallingThread() {
        List<Thread> threads = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            OrderedMapper.on(pool, 4).map(counting(6), item -> Thread.currentThread(), threads::add);
        }

        assertThat(threads).hasSize(6).doesNotContain(Thread.currentThread());
    }

    @Test
    void aCheckedFailureOfAStepIsRethrownAsItself() {
        OrderedMapper.Feed<Integer, IOException> feed = () -> Optional.of(1);
        OrderedMapper mapper = OrderedMapper.on(Runnable::run, 2);

        assertThatThrownBy(() -> mapper.map(feed, item -> {
            throw new IOException("no disk");
        }, result -> {
        })).isExactlyInstanceOf(IOException.class).hasMessage("no disk");
    }

    @Test
    void aRuntimeFailureOfAStepOnAPoolIsRethrownAsItself() {
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            OrderedMapper mapper = OrderedMapper.on(pool, 2);

            assertThatThrownBy(() -> mapper.map(counting(3), item -> {
                throw new IllegalStateException("bad item " + item);
            }, result -> {
            })).isExactlyInstanceOf(IllegalStateException.class).hasMessage("bad item 0");
        }
    }

    @Test
    void aFailureStopsTheHandingOnOfLaterResults() {
        List<Integer> results = new ArrayList<>();

        assertThatThrownBy(() -> OrderedMapper.sequential().map(counting(5), item -> {
            if (item == 2) {
                throw new IllegalArgumentException("item 2");
            }
            return item;
        }, results::add)).isInstanceOf(IllegalArgumentException.class);

        assertThat(results).containsExactly(0, 1);
    }

    @Test
    void aFailureOfTheFeedIsRethrownAfterTheResultsBeforeItWereHandedOn() {
        List<Integer> results = new ArrayList<>();
        int[] read = {0};
        OrderedMapper.Feed<Integer, IOException> feed = () -> {
            if (read[0] == 2) {
                throw new IOException("unreadable");
            }
            return Optional.of(read[0]++);
        };

        assertThatThrownBy(() -> OrderedMapper.sequential().map(feed, item -> item, results::add))
                .isExactlyInstanceOf(IOException.class).hasMessage("unreadable");
        assertThat(results).containsExactly(0, 1);
    }

    @Test
    void aWindowBelowOneIsRejected() {
        assertThatThrownBy(() -> OrderedMapper.on(Runnable::run, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the later items never finished");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
