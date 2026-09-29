package cz.cvut.fit.acb.coding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LengthFrequenciesTest {

    @Test
    void theFlatStartGivesEveryLengthAFrequencyOfOne() {
        assertThat(LengthFrequencies.flat().startingTable(4)).containsExactly(1, 1, 1, 1);
    }

    @Test
    void lengthsPastTheGivenFrequenciesStartAtOne() {
        assertThat(LengthFrequencies.of(45, 13).startingTable(4)).containsExactly(45, 13, 1, 1);
    }

    @Test
    void frequenciesPastTheAlphabetAreCutOff() {
        LengthFrequencies frequencies = LengthFrequencies.of(45, 13, 10, 7);

        assertThat(frequencies.startingTable(2)).containsExactly(45, 13);
        assertThat(frequencies.limitedTo(2)).isEqualTo(LengthFrequencies.of(45, 13));
        assertThat(frequencies.limitedTo(9)).isEqualTo(frequencies);
    }

    @Test
    void frequenciesMustBePositive() {
        assertThatThrownBy(() -> LengthFrequencies.of(3, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LengthFrequencies.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aStartingTotalAboveWhatTheModelIsHalvedFromDoesNotFit() {
        int limit = AdaptiveFrequencyModel.limitFor(128);

        assertThat(LengthFrequencies.of(limit - 127)).satisfies(frequencies -> frequencies.requireFits(128));
        assertThatThrownBy(() -> LengthFrequencies.of(limit - 126).requireFits(128))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void frequenciesWithTheSameValuesAreEqual() {
        assertThat(LengthFrequencies.of(1, 2)).isEqualTo(LengthFrequencies.of(1, 2))
                .hasSameHashCodeAs(LengthFrequencies.of(1, 2)).isNotEqualTo(LengthFrequencies.of(1, 3));
    }
}
