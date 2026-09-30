package cz.cvut.fit.acb.mixing;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LogisticTest {

    @Test
    void anEvenChanceIsAStretchOfZero() {
        assertThat(Logistic.squash(0)).isCloseTo(Logistic.ONE / 2, within(1));
        assertThat(Logistic.stretch(Logistic.ONE / 2)).isBetween(-8, 8);
    }

    @Test
    void stretchesPastTheRangeSaturateWithoutReachingCertainty() {
        assertThat(Logistic.squash(100_000)).isEqualTo(Logistic.ONE - 1);
        assertThat(Logistic.squash(-100_000)).isEqualTo(1);
    }

    @Property
    void squashingAStretchGivesTheProbabilityBack(@ForAll @IntRange(min = 1, max = Logistic.ONE - 1) int probability) {
        int back = Logistic.squash(Logistic.stretch(probability));

        assertThat(back).isCloseTo(probability, within(Math.max(8, probability / 16)));
    }

    @Property
    void aLargerStretchIsNeverALessLikelyBit(@ForAll @IntRange(min = -2100, max = 2100) int stretch) {
        assertThat(Logistic.squash(stretch + 1)).isGreaterThanOrEqualTo(Logistic.squash(stretch));
    }
}
