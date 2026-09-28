package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.fixtures.SortedListContextIndex;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkedContextIndexPropertiesTest {

    @Property
    void afterAnyInsertsItAgreesWithASortedListOnEveryRankAndEveryPosition(
            @ForAll("texts") byte[] text, @ForAll @IntRange(min = 2, max = 9) int chunkCapacity,
            @ForAll long seed) {
        ContextOrder order = ContextOrder.byLastBytes(new ByteArray(text));
        ChunkedContextIndex actual = new ChunkedContextIndex(order, chunkCapacity);
        SortedListContextIndex expected = new SortedListContextIndex(order);
        Random random = new Random(seed);

        for (int position : shuffledPositions(text.length, random)) {
            if (random.nextBoolean()) {
                assertThat(actual.rank(position)).isEqualTo(expected.rank(position));
            }
            actual.insert(position);
            expected.insert(position);
        }

        assertThat(actual.size()).isEqualTo(expected.size());
        for (int rank = 0; rank < expected.size(); rank++) {
            assertThat(actual.cursorAt(rank).position()).as("rank %d", rank)
                    .isEqualTo(expected.cursorAt(rank).position());
        }
        for (int position = 0; position <= text.length; position++) {
            assertThat(actual.rank(position)).as("rank of %d", position).isEqualTo(expected.rank(position));
        }
    }

    @Property
    void cursorsWalkTheSameNeighboursAsASortedList(@ForAll("texts") byte[] text,
                                                    @ForAll @IntRange(min = 2, max = 9) int chunkCapacity,
                                                    @ForAll long seed) {
        ContextOrder order = ContextOrder.byLastBytes(new ByteArray(text));
        ChunkedContextIndex actual = new ChunkedContextIndex(order, chunkCapacity);
        SortedListContextIndex expected = new SortedListContextIndex(order);
        for (int position = 0; position < text.length; position++) {
            actual.insert(position);
            expected.insert(position);
        }
        int start = new Random(seed).nextInt(text.length);
        ContextCursor up = actual.cursorAt(start);
        ContextCursor expectedUp = expected.cursorAt(start);
        ContextCursor down = actual.cursorAt(start);
        ContextCursor expectedDown = expected.cursorAt(start);

        do {
            assertThat(up.rank()).isEqualTo(expectedUp.rank());
            assertThat(up.position()).isEqualTo(expectedUp.position());
        } while (moveBoth(up, expectedUp, true));
        do {
            assertThat(down.rank()).isEqualTo(expectedDown.rank());
            assertThat(down.position()).isEqualTo(expectedDown.position());
        } while (moveBoth(down, expectedDown, false));
        assertThat(up.rank()).isEqualTo(text.length - 1);
        assertThat(down.rank()).isZero();
    }

    /** Uniform bytes rarely tie, so half the texts use two letters to force equal contexts. */
    @Provide
    Arbitrary<byte[]> texts() {
        Arbitrary<byte[]> anyBytes = Arbitraries.bytes().array(byte[].class).ofMinSize(1).ofMaxSize(150);
        Arbitrary<byte[]> fewSymbols = Arbitraries.bytes().between((byte) 'a', (byte) 'b')
                .array(byte[].class).ofMinSize(1).ofMaxSize(150);
        return Arbitraries.oneOf(anyBytes, fewSymbols);
    }

    private static boolean moveBoth(ContextCursor actual, ContextCursor expected, boolean up) {
        boolean moved = up ? actual.moveUp() : actual.moveDown();
        boolean expectedMoved = up ? expected.moveUp() : expected.moveDown();
        assertThat(moved).isEqualTo(expectedMoved);
        return moved;
    }

    private static List<Integer> shuffledPositions(int length, Random random) {
        List<Integer> positions = new ArrayList<>();
        for (int position = 0; position < length; position++) {
            positions.add(position);
        }
        Collections.shuffle(positions, random);
        return positions;
    }
}
