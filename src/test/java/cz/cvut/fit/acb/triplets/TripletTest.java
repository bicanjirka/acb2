package cz.cvut.fit.acb.triplets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripletTest {

    @Test
    void aTripletCodesTheBytesItCopiesAndTheLiteralAfterThem() {
        assertThat(Triplet.literal((byte) 'a').consumed()).isEqualTo(1);
        assertThat(Triplet.match(3, 5).consumed()).isEqualTo(5);
        assertThat(Triplet.matchWithLiteral(3, 5, (byte) 'a').consumed()).isEqualTo(6);
    }

    @Test
    void aMatchOfNoBytesCannotBeMade() {
        assertThatThrownBy(() -> Triplet.match(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Triplet.matchWithLiteral(0, 0, (byte) 'a'))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
