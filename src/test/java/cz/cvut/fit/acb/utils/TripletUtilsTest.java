package cz.cvut.fit.acb.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TripletUtilsTest {

    @Test
    void aTripletPrintsItsFieldsInCodingOrder() {
        String text = TripletUtils.tripletString(3, 12, (byte) -1);

        assertThat(text).isEqualTo("(3, 12, -1)");
    }
}
