package cz.cvut.fit.acb.coding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveFrequencyModelExclusionTest {

    private static AdaptiveFrequencyModel modelOf(int... frequencies) {
        return new AdaptiveFrequencyModel(frequencies);
    }

    @Test
    void leavingASymbolOutTakesItsFrequencyOffTheTotal() {
        AdaptiveFrequencyModel model = modelOf(3, 5, 7);

        assertThat(model.totalWithout(1)).isEqualTo(10);
        assertThat(model.totalWithout(-1)).isEqualTo(15);
    }

    @Test
    void symbolsAfterTheLeftOutOneShiftDownByItsFrequency() {
        AdaptiveFrequencyModel model = modelOf(3, 5, 7);

        assertThat(model.cumulativeWithout(0, 1)).isEqualTo(0);
        assertThat(model.cumulativeWithout(2, 1)).isEqualTo(3);
        assertThat(model.cumulativeWithout(2, -1)).isEqualTo(8);
    }

    @Test
    void everyTargetOfTheReducedRangeFindsTheSymbolThatOwnsIt() {
        AdaptiveFrequencyModel model = modelOf(3, 5, 7, 2);

        for (int excluded = -1; excluded < 4; excluded++) {
            for (int target = 0; target < model.totalWithout(excluded); target++) {
                int symbol = model.symbolAtWithout(target, excluded);

                assertThat(symbol).isNotEqualTo(excluded);
                assertThat(model.cumulativeWithout(symbol, excluded)).isLessThanOrEqualTo(target);
                assertThat(target).isLessThan(model.cumulativeWithout(symbol, excluded) + model.frequency(symbol));
            }
        }
    }
}
