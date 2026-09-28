package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.fixtures.SortedListContextIndex;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkedContextIndexTest {

    private static byte[] textOfTwoLetters(int length) {
        byte[] text = new byte[length];
        Random random = new Random(20260928L);
        for (int i = 0; i < length; i++) {
            text[i] = (byte) ('a' + random.nextInt(2));
        }
        return text;
    }

    private static ChunkedContextIndex indexOver(int length) {
        return new ChunkedContextIndex(ContextOrder.byLastBytes(new ByteArray(new byte[length])));
    }

    @Test
    void aTextThatFillsManyDefaultChunksInPositionOrderAgreesWithASortedList() {
        byte[] text = textOfTwoLetters(3_000);
        ContextOrder order = ContextOrder.byLastBytes(new ByteArray(text));
        ChunkedContextIndex actual = new ChunkedContextIndex(order);
        SortedListContextIndex expected = new SortedListContextIndex(order);

        for (int position = 0; position < text.length; position++) {
            assertThat(actual.rank(position)).isEqualTo(expected.rank(position));
            actual.insert(position);
            expected.insert(position);
        }

        for (int rank = 0; rank < text.length; rank += 7) {
            assertThat(actual.cursorAt(rank).position()).isEqualTo(expected.cursorAt(rank).position());
        }
    }

    @Test
    void aCursorIsRefusedOnceTheIndexHasChanged() {
        ChunkedContextIndex index = indexOver(4);
        index.insert(0);
        ContextCursor cursor = index.cursorAt(0);

        index.insert(1);

        assertThatThrownBy(cursor::position).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aRankBeyondTheEntriesHasNoCursor() {
        ChunkedContextIndex index = indexOver(4);
        index.insert(0);

        assertThatThrownBy(() -> index.cursorAt(1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> index.cursorAt(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void anEmptyIndexHasNoEntriesBeforeAnyPosition() {
        ChunkedContextIndex index = indexOver(4);

        assertThat(index.size()).isZero();
        assertThat(index.rank(3)).isZero();
    }
}
