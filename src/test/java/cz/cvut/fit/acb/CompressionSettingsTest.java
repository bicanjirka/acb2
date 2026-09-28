package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.AdaptiveFrequencyModel;
import cz.cvut.fit.acb.format.StreamHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompressionSettingsTest {

    @Test
    void aWithCopyChangesOnlyItsOwnComponent() {
        CompressionSettings defaults = CompressionSettings.defaults();

        CompressionSettings changed = defaults.withTripletCoding(TripletCoding.VALACH);

        assertThat(changed.tripletCoding()).isEqualTo(TripletCoding.VALACH);
        assertThat(changed.withTripletCoding(defaults.tripletCoding())).isEqualTo(defaults);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, CompressionSettings.MAX_FIELD_BITS + 1})
    void bitWidthsOutsideWhatAStreamCarriesAreRejected(int bits) {
        CompressionSettings defaults = CompressionSettings.defaults();

        assertThatThrownBy(() -> defaults.withDistanceBits(bits)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> defaults.withLengthBits(bits)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7, 12, CompressionSettings.MAX_FIELD_BITS})
    void everyLengthWidthAcceptsTheFlatStartOfItsModel(int bits) {
        CompressionSettings settings = CompressionSettings.defaults().withLengthBits(bits);

        assertThat(settings.lengthBits()).isEqualTo(bits);
    }

    @Test
    void lengthFrequenciesMustBePositive() {
        assertThatThrownBy(() -> CompressionSettings.defaults().withLengthFrequencies(3, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lengthFrequenciesThatOverflowTheModelAreRejected() {
        int tooMuch = AdaptiveFrequencyModel.limitFor(CompressionSettings.lengthAlphabetSize(7));

        assertThatThrownBy(() -> CompressionSettings.defaults().withLengthFrequencies(tooMuch, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CompressionSettings.defaults().withLengthFrequencies(Integer.MAX_VALUE, Integer.MAX_VALUE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lengthFrequenciesBeyondTheLengthAlphabetAreDroppedFromTheHeader() {
        CompressionSettings settings = CompressionSettings.defaults().withLengthBits(1)
                .withLengthFrequencies(45, 13, 10, 7, 5, 4);

        assertThat(StreamHeader.of(settings).lengthFrequencies()).containsExactly(45, 13, 10);
    }

    @Test
    void lengthFrequenciesCannotBeChangedFromOutside() {
        int[] frequencies = {5, 4};
        CompressionSettings settings = CompressionSettings.defaults().withLengthFrequencies(frequencies);

        frequencies[0] = 99;
        settings.lengthFrequencies()[1] = 99;

        assertThat(settings.lengthFrequencies()).containsExactly(5, 4);
    }

    @Test
    void theBitWidthsBoundTheMatchDistanceAndLength() {
        CompressionSettings settings = CompressionSettings.defaults().withDistanceBits(6).withLengthBits(4);

        assertThat(settings.maxDistance()).isEqualTo(32);
        assertThat(settings.maxLength()).isEqualTo(15);
    }
}
