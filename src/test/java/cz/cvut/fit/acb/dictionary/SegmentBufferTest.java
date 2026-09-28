package cz.cvut.fit.acb.dictionary;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegmentBufferTest {

    private static SegmentBuffer bufferOf(String text) {
        return SegmentBuffer.of(text.getBytes(StandardCharsets.US_ASCII));
    }

    private static String textOf(SegmentBuffer buffer) {
        return new String(buffer.toArray(), StandardCharsets.US_ASCII);
    }

    @Test
    void aCopyThatEndsBeforeTheAppendedBytesIsTakenAsIs() {
        SegmentBuffer buffer = bufferOf("abcd");

        buffer.appendCopy(1, 2);

        assertThat(textOf(buffer)).isEqualTo("abcdbc");
    }

    @Test
    void aCopyThatRunsIntoTheBytesItAppendsRepeatsThem() {
        SegmentBuffer buffer = bufferOf("ab");

        buffer.appendCopy(0, 5);

        assertThat(textOf(buffer)).isEqualTo("abababa");
    }

    @Test
    void aCopyFromWhereNothingIsKnownIsRefused() {
        SegmentBuffer buffer = bufferOf("ab");

        assertThatThrownBy(() -> buffer.appendCopy(2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> buffer.appendCopy(-1, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEmptyBufferGrowsAsBytesAreAppended() {
        SegmentBuffer buffer = SegmentBuffer.empty();

        for (int i = 0; i < 1_000; i++) {
            buffer.append((byte) i);
        }

        assertThat(buffer.length()).isEqualTo(1_000);
        assertThat(buffer.byteAt(999)).isEqualTo((byte) 999);
    }

    @Test
    void aByteBeyondTheKnownOnesCannotBeRead() {
        SegmentBuffer buffer = bufferOf("ab");

        assertThatThrownBy(() -> buffer.byteAt(2)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void theCommonLengthStopsAtTheFirstDifference() {
        SegmentBuffer buffer = bufferOf("abcxabcy");

        assertThat(buffer.commonLength(4, 0, 10)).isEqualTo(3);
    }

    @Test
    void theCommonLengthStopsAtTheLimit() {
        SegmentBuffer buffer = bufferOf("abcdabcd");

        assertThat(buffer.commonLength(4, 0, 2)).isEqualTo(2);
    }

    @Test
    void theCommonLengthStopsAtTheEndOfTheKnownBytes() {
        SegmentBuffer buffer = bufferOf("ababab");

        assertThat(buffer.commonLength(2, 0, 10)).isEqualTo(4);
        assertThat(buffer.commonLength(6, 0, 10)).isZero();
    }

    @Test
    void theArrayHandedOutIsACopy() {
        SegmentBuffer buffer = bufferOf("ab");

        buffer.toArray()[0] = 'z';

        assertThat(textOf(buffer)).isEqualTo("ab");
    }
}
