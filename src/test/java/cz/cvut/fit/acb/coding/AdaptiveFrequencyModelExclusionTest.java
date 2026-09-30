package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.dictionary.ByteSet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveFrequencyModelExclusionTest {

    private static AdaptiveFrequencyModel modelOf(int... frequencies) {
        return new AdaptiveFrequencyModel(frequencies);
    }

    @Test
    void leavingSymbolsOutTakesTheirFrequenciesOffTheTotal() {
        AdaptiveFrequencyModel model = modelOf(3, 5, 7);

        assertThat(model.totalWithout(ByteSet.of(1))).isEqualTo(10);
        assertThat(model.totalWithout(ByteSet.of(0, 2))).isEqualTo(5);
        assertThat(model.totalWithout(ByteSet.none())).isEqualTo(15);
    }

    @Test
    void symbolsAfterTheLeftOutOnesShiftDownByTheirFrequencies() {
        AdaptiveFrequencyModel model = modelOf(3, 5, 7);

        assertThat(model.cumulativeWithout(0, ByteSet.of(1))).isEqualTo(0);
        assertThat(model.cumulativeWithout(2, ByteSet.of(1))).isEqualTo(3);
        assertThat(model.cumulativeWithout(2, ByteSet.of(0, 1))).isEqualTo(0);
        assertThat(model.cumulativeWithout(2, ByteSet.none())).isEqualTo(8);
    }

    @Test
    void everyTargetOfTheReducedRangeFindsTheSymbolThatOwnsIt() {
        AdaptiveFrequencyModel model = modelOf(3, 5, 7, 2, 4);
        ByteSet[] exclusions = {ByteSet.none(), ByteSet.of(0), ByteSet.of(4), ByteSet.of(1, 3), ByteSet.of(0, 1, 2),
                ByteSet.of(0, 2, 4), ByteSet.of(1, 2, 3, 4)};

        for (ByteSet excluded : exclusions) {
            for (int target = 0; target < model.totalWithout(excluded); target++) {
                int symbol = model.symbolAtWithout(target, excluded);

                assertThat(excluded.contains(symbol)).as("excluded %s at %d", excluded, target).isFalse();
                assertThat(model.cumulativeWithout(symbol, excluded)).isLessThanOrEqualTo(target);
                assertThat(target).isLessThan(model.cumulativeWithout(symbol, excluded) + model.frequency(symbol));
            }
        }
    }
}
