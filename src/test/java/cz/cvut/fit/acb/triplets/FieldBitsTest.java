package cz.cvut.fit.acb.triplets;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldBitsTest {

    @Test
    void aNegativeValueKeepsOnlyTheLowBitsOfItsField() {
        assertThat(FieldBits.lowBits(-1, 6)).isEqualTo(0b111111);
        assertThat(FieldBits.lowBits(-32, 6)).isEqualTo(0b100000);
    }

    @Test
    void theTopBitOfAFieldIsItsSign() {
        assertThat(FieldBits.signExtend(0b100000, 6)).isEqualTo(-32);
        assertThat(FieldBits.signExtend(0b011111, 6)).isEqualTo(31);
    }

    @Property
    void aValueThatFitsTheFieldSurvivesTheRoundTrip(@ForAll @IntRange(min = 1, max = 16) int bits,
                                                    @ForAll @IntRange(min = -32768, max = 32767) int raw) {
        int value = FieldBits.signExtend(FieldBits.lowBits(raw, bits), bits);

        assertThat(FieldBits.signExtend(FieldBits.lowBits(value, bits), bits)).isEqualTo(value);
        assertThat(value).isBetween(-(1 << (bits - 1)), (1 << (bits - 1)) - 1);
    }
}
