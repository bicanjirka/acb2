package cz.cvut.fit.acb.dictionary;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ContextOrderTest {

    private static final int DEPTH = 10;

    private static ContextOrder orderOf(int depth, int... bytes) {
        byte[] text = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            text[i] = (byte) bytes[i];
        }
        return ContextOrder.byLastBytes(SegmentBuffer.of(text), depth);
    }

    @Test
    void contextsAreComparedFromTheNearestByteBackwards() {
        ContextOrder order = orderOf(DEPTH, 'a', 'z', 'b', 'a', 'x');

        assertThat(order.compare(4, 2)).isNegative();
        assertThat(order.compare(2, 4)).isPositive();
    }

    @Test
    void bytesAreComparedAsUnsignedSoAHighByteSortsLast() {
        ContextOrder order = orderOf(DEPTH, 0x80, 0x7f, 'x');

        assertThat(order.compare(1, 2)).isPositive();
    }

    @Test
    void aContextThatRunsOutSortsBeforeALongerOneItIsAPrefixOf() {
        ContextOrder order = orderOf(DEPTH, 'a', 'a', 'a', 'a');

        assertThat(order.compare(1, 3)).isNegative();
        assertThat(order.compare(3, 1)).isPositive();
    }

    @Test
    void contextsEqualOverTheWholeDepthAreOrderedByPosition() {
        int[] text = new int[3 * DEPTH];
        Arrays.fill(text, 'a');
        text[0] = 'q';
        ContextOrder order = orderOf(DEPTH, text);

        assertThat(order.compare(2 * DEPTH, 3 * DEPTH - 1)).isNegative();
    }

    @Test
    void sharedBitsCountTheEqualTopBitsOfTheFirstByteThatDiffers() {
        ContextOrder order = orderOf(DEPTH, 'a', 'x', 'c', 'x', 'q');

        assertThat(order.sharedBits(2, 4)).isEqualTo(8 + 6);
        assertThat(order.sharedBits(2, order.prefix(2), 4, order.prefix(4))).isEqualTo(8 + 6);
    }

    @Test
    void sharedBitsStopWhereAContextEndsOrAtTheDepth() {
        ContextOrder order = orderOf(2, 'a', 'b', 'a', 'b', 'a', 'b');

        assertThat(order.sharedBits(2, 6)).isEqualTo(16);
        assertThat(order.sharedBits(1, 5)).isEqualTo(8);
        assertThat(order.sharedBits(1, order.prefix(1), 5, order.prefix(5))).isEqualTo(8);
    }

    @Test
    void theFollowingByteIsTheOneAtThePosition() {
        ContextOrder order = orderOf(DEPTH, 'a', 0xF0, 'c');

        assertThat(order.followingByte(1)).isEqualTo(0xF0);
    }

    @Test
    void bytesBeyondTheDepthDoNotDecideTheOrder() {
        ContextOrder shallow = orderOf(2, 'x', 'b', 'c', 'a', 'b', 'c');
        ContextOrder deep = orderOf(3, 'x', 'b', 'c', 'a', 'b', 'c');

        assertThat(shallow.compare(6, 3)).isPositive();
        assertThat(deep.compare(6, 3)).isNegative();
    }
}
