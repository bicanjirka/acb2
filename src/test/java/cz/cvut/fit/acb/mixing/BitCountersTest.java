package cz.cvut.fit.acb.mixing;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BitCountersTest {

    @Test
    void aContextNeverSeenIsAnEvenChance() {
        BitCounters counters = new BitCounters(4, 30);

        assertThat(counters.probability(3)).isEqualTo(Logistic.ONE / 2);
        assertThat(counters.count(3)).isZero();
    }

    @Test
    void aContextLearnsTheRateOfItsOnesAndLeavesTheOthersAlone() {
        BitCounters counters = new BitCounters(2, 60);
        Random random = new Random(20260930L);

        for (int i = 0; i < 5_000; i++) {
            counters.update(1, random.nextInt(4) == 0 ? 1 : 0);
        }

        assertThat(counters.probability(1) / (double) Logistic.ONE).isCloseTo(0.25, within(0.08));
        assertThat(counters.probability(0)).isEqualTo(Logistic.ONE / 2);
    }

    @Test
    void theFirstBitsMoveTheEstimateFarThenTheRateSettles() {
        BitCounters counters = new BitCounters(1, 3);

        counters.update(0, 1);
        int afterOne = counters.probability(0);
        for (int i = 0; i < 10; i++) {
            counters.update(0, 1);
        }

        assertThat(afterOne).isGreaterThan(Logistic.ONE * 3 / 4);
        assertThat(counters.probability(0)).isGreaterThan(afterOne);
        assertThat(counters.count(0)).isEqualTo(3);
    }
}
