package cz.cvut.fit.acb.utils;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BitUtilsTest {

    @Test
    void aNegativeValueKeepsOnlyTheLowBitsOfItsField() {
        assertThat(BitUtils.lowBits(-1, 6)).isEqualTo(0b111111);
        assertThat(BitUtils.lowBits(-32, 6)).isEqualTo(0b100000);
    }

    @Test
    void theTopBitOfAFieldIsItsSign() {
        assertThat(BitUtils.signExtend(0b100000, 6)).isEqualTo(-32);
        assertThat(BitUtils.signExtend(0b011111, 6)).isEqualTo(31);
    }

    @Property
    void aValueThatFitsTheFieldSurvivesTheRoundTrip(@ForAll @IntRange(min = 1, max = 16) int bits,
                                                    @ForAll @IntRange(min = -32768, max = 32767) int raw) {
        int value = BitUtils.signExtend(BitUtils.lowBits(raw, bits), bits);

        assertThat(BitUtils.signExtend(BitUtils.lowBits(value, bits), bits)).isEqualTo(value);
        assertThat(value).isBetween(-(1 << (bits - 1)), (1 << (bits - 1)) - 1);
    }
}
