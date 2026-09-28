package cz.cvut.fit.acb.dictionary;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ContextOrderTest {

    private static ContextOrder orderOf(int... bytes) {
        byte[] text = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            text[i] = (byte) bytes[i];
        }
        return ContextOrder.byLastBytes(new ByteArray(text));
    }

    @Test
    void contextsAreComparedFromTheNearestByteBackwards() {
        ContextOrder order = orderOf('a', 'z', 'b', 'a', 'x');

        assertThat(order.compare(4, 2)).isNegative();
        assertThat(order.compare(2, 4)).isPositive();
    }

    @Test
    void bytesAreComparedAsSignedSoAHighByteSortsFirst() {
        ContextOrder order = orderOf(0x80, 0x7f, 'x');

        assertThat(order.compare(1, 2)).isNegative();
    }

    @Test
    void aContextThatRunsOutSortsBeforeALongerOneItIsAPrefixOf() {
        ContextOrder order = orderOf('a', 'a', 'a', 'a');

        assertThat(order.compare(1, 3)).isNegative();
        assertThat(order.compare(3, 1)).isPositive();
    }

    @Test
    void contextsEqualOverTheWholeDepthAreOrderedByPosition() {
        int[] text = new int[3 * ContextOrder.DEPTH];
        Arrays.fill(text, 'a');
        text[0] = 'q';
        ContextOrder order = orderOf(text);

        assertThat(order.compare(2 * ContextOrder.DEPTH, 3 * ContextOrder.DEPTH - 1)).isNegative();
    }
}
