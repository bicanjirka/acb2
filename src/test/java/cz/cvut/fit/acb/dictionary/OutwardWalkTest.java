package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.fixtures.SortedListContextIndex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class OutwardWalkTest {

    private static final int ENTRIES = 9;

    private static ContextIndex indexOfNineEntries() {
        byte[] text = new byte[ENTRIES];
        new Random(7).nextBytes(text);
        SortedListContextIndex index = new SortedListContextIndex(ContextOrder.byLastBytes(SegmentBuffer.of(text), 4));
        for (int entry = 0; entry < ENTRIES; entry++) {
            index.insert(entry);
        }
        return index;
    }

    private static List<Integer> ranksOf(OutwardWalk walk) {
        List<Integer> ranks = new ArrayList<>();
        while (walk.advance()) {
            ranks.add(walk.rank());
        }
        return ranks;
    }

    @Test
    void theWalkTakesTheRankAboveBeforeTheRankBelowAtEachDistanceFromTheContext() {
        ContextIndex index = indexOfNineEntries();

        List<Integer> ranks = ranksOf(OutwardWalk.over(index, 4, 0, 8));

        assertThat(ranks).containsExactly(4, 5, 3, 6, 2, 7, 1, 8, 0);
    }

    @Test
    void theWalkStaysInsideTheWindow() {
        ContextIndex index = indexOfNineEntries();

        List<Integer> ranks = ranksOf(OutwardWalk.over(index, 3, 2, 5));

        assertThat(ranks).containsExactly(3, 4, 2, 5);
    }

    @Test
    void whenTheWindowEndsBelowTheContextTheWalkGoesOnAbove() {
        ContextIndex index = indexOfNineEntries();

        List<Integer> ranks = ranksOf(OutwardWalk.over(index, 1, 1, 8));

        assertThat(ranks).containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    void whenTheWindowEndsAboveTheContextTheWalkGoesOnBelow() {
        ContextIndex index = indexOfNineEntries();

        List<Integer> ranks = ranksOf(OutwardWalk.over(index, 8, 0, 8));

        assertThat(ranks).containsExactly(8, 7, 6, 5, 4, 3, 2, 1, 0);
    }

    @Test
    void anEmptyWindowHasNoRanks() {
        ContextIndex index = indexOfNineEntries();

        OutwardWalk walk = OutwardWalk.over(index, -1, 0, -1);

        assertThat(walk.advance()).isFalse();
    }

    @Test
    void eachRankComesWithTheStartOfItsContent() {
        ContextIndex index = indexOfNineEntries();
        OutwardWalk walk = OutwardWalk.over(index, 4, 0, 8);

        while (walk.advance()) {
            assertThat(walk.position()).isEqualTo(index.cursorAt(walk.rank()).position());
        }
    }

    @Test
    void aWalkThatHasEndedStaysEnded() {
        ContextIndex index = indexOfNineEntries();
        OutwardWalk walk = OutwardWalk.over(index, 4, 3, 5);
        ranksOf(walk);

        assertThat(walk.advance()).isFalse();
    }
}
