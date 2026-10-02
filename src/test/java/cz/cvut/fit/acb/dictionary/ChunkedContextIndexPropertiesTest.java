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

    private static final int DEPTH = 10;

    @Property
    void afterAnyInsertsItAgreesWithASortedListOnEveryRankAndEveryPosition(
            @ForAll("texts") byte[] text, @ForAll @IntRange(min = 2, max = 9) int chunkCapacity,
            @ForAll boolean walks, @ForAll long seed) {
        ContextOrder order = ContextOrder.byLastBytes(SegmentBuffer.of(text), DEPTH);
        ChunkedContextIndex actual = new ChunkedContextIndex(order, chunkCapacity, walks);
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
        ContextOrder order = ContextOrder.byLastBytes(SegmentBuffer.of(text), DEPTH);
        ChunkedContextIndex actual = new ChunkedContextIndex(order, chunkCapacity, true);
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
            assertThat(up.sharedWithPrevious()).isEqualTo(expectedUp.sharedWithPrevious());
        } while (moveBoth(up, expectedUp, true));
        do {
            assertThat(down.rank()).isEqualTo(expectedDown.rank());
            assertThat(down.position()).isEqualTo(expectedDown.position());
            assertThat(down.sharedWithPrevious()).isEqualTo(expectedDown.sharedWithPrevious());
        } while (moveBoth(down, expectedDown, false));
        assertThat(up.rank()).isEqualTo(text.length - 1);
        assertThat(down.rank()).isZero();
    }

    @Property
    void theEntriesAroundAPositionAreItsNeighboursInASortedListWithTheirSharedBitsAfterAnyInserts(
            @ForAll("texts") byte[] text, @ForAll @IntRange(min = 2, max = 9) int chunkCapacity,
            @ForAll @IntRange(min = 1, max = 12) int reach, @ForAll long seed) {
        ContextOrder order = ContextOrder.byLastBytes(SegmentBuffer.of(text), DEPTH);
        ChunkedContextIndex actual = new ChunkedContextIndex(order, chunkCapacity, true);
        SortedListContextIndex expected = new SortedListContextIndex(order);
        for (int position : shuffledPositions(text.length, new Random(seed))) {
            actual.insert(position);
            expected.insert(position);
        }
        Surroundings found = new Surroundings(reach);
        Surroundings wanted = new Surroundings(reach);

        for (int position = 0; position <= text.length; position++) {
            actual.around(position, reach, found);
            expected.around(position, reach, wanted);

            assertThat(found.belowCount()).as("below %d", position).isEqualTo(wanted.belowCount());
            assertThat(found.aboveCount()).as("above %d", position).isEqualTo(wanted.aboveCount());
            assertThat(found.belowAgreement()).as("below %d", position).isEqualTo(wanted.belowAgreement());
            assertThat(found.aboveAgreement()).as("above %d", position).isEqualTo(wanted.aboveAgreement());
            for (int i = found.capacity() - found.belowCount(); i < found.capacity(); i++) {
                assertThat(found.belowPositions()[i]).isEqualTo(wanted.belowPositions()[i]);
                assertThat(found.belowShared()[i]).isEqualTo(wanted.belowShared()[i]);
            }
            for (int i = 0; i < found.aboveCount(); i++) {
                assertThat(found.abovePositions()[i]).isEqualTo(wanted.abovePositions()[i]);
                assertThat(found.aboveShared()[i]).isEqualTo(wanted.aboveShared()[i]);
            }
        }
    }

    @Property
    void placesLookedUpBeforeInsertsAreStillRightAfterThem(
            @ForAll("texts") byte[] text, @ForAll @IntRange(min = 2, max = 9) int chunkCapacity,
            @ForAll @IntRange(min = 1, max = 6) int reach, @ForAll long seed) {
        ContextOrder order = ContextOrder.byLastBytes(SegmentBuffer.of(text), DEPTH);
        ChunkedContextIndex actual = new ChunkedContextIndex(order, chunkCapacity, true);
        SortedListContextIndex expected = new SortedListContextIndex(order);
        Random random = new Random(seed);
        Surroundings found = new Surroundings(reach);
        Surroundings wanted = new Surroundings(reach);
        int[] lookedUp = {0, 0};

        for (int position : shuffledPositions(text.length, random)) {
            if (random.nextInt(3) == 0) {
                int slot = random.nextInt(lookedUp.length);
                lookedUp[slot] = random.nextInt(text.length + 1);
                actual.around(lookedUp[slot], reach, found);
            }
            actual.insert(position);
            expected.insert(position);

            for (int place : lookedUp) {
                actual.around(place, reach, found);
                expected.around(place, reach, wanted);
                assertThat(positionsOf(found)).as("around %d", place).isEqualTo(positionsOf(wanted));
            }
        }
    }

    private static List<Integer> positionsOf(Surroundings surroundings) {
        List<Integer> positions = new ArrayList<>();
        for (int i = surroundings.capacity() - surroundings.belowCount(); i < surroundings.capacity(); i++) {
            positions.add(surroundings.belowPositions()[i]);
        }
        for (int i = 0; i < surroundings.aboveCount(); i++) {
            positions.add(surroundings.abovePositions()[i]);
        }
        return positions;
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
