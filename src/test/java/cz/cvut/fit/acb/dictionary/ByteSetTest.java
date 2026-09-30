package cz.cvut.fit.acb.dictionary;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ByteSetTest {

    @Test
    void aSetHoldsExactlyTheBytesItWasMadeOfInEachOfItsWords() {
        ByteSet set = ByteSet.of(0, 63, 64, 127, 128, 200, 255);

        for (int value = 0; value < 256; value++) {
            boolean member = value == 0 || value == 63 || value == 64 || value == 127 || value == 128
                    || value == 200 || value == 255;
            assertThat(set.contains(value)).as("contains %d", value).isEqualTo(member);
        }
    }

    @Test
    void nextFindsTheMembersInOrderAndThenNothing() {
        ByteSet set = ByteSet.of(5, 64, 255);

        assertThat(set.next(0)).isEqualTo(5);
        assertThat(set.next(5)).isEqualTo(5);
        assertThat(set.next(6)).isEqualTo(64);
        assertThat(set.next(65)).isEqualTo(255);
        assertThat(set.next(256)).isEqualTo(-1);
        assertThat(ByteSet.none().next(0)).isEqualTo(-1);
        assertThat(set.next(-3)).isEqualTo(5);
    }

    @Test
    void noneIsEmptyAndTheIdentityOfAddingAll() {
        ByteSet set = ByteSet.of(1, 2);

        assertThat(ByteSet.none().isEmpty()).isTrue();
        assertThat(set.isEmpty()).isFalse();
        assertThat(ByteSet.builder().addAll(set).addAll(ByteSet.none()).build()).isEqualTo(set);
    }

    @Test
    void valuesThatAreNotBytesAreRefusedAndNeverMembers() {
        assertThatThrownBy(() -> ByteSet.of(256)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ByteSet.of(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ByteSet.of(1).contains(256)).isFalse();
        assertThat(ByteSet.of(1).contains(-1)).isFalse();
    }
}
