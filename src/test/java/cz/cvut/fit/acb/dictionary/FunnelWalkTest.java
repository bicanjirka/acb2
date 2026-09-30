package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.fixtures.ContextBits;
import cz.cvut.fit.acb.fixtures.SortedListContextIndex;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FunnelWalkTest {

    /**
     * A candidate of a funnel: where its content starts, what it weighs, the bits its context agrees on and
     * its distance in ranks from the context, above positive.
     */
    private record Candidate(int position, int weight, int agreement, int distance) {
    }

    private static List<Candidate> candidatesOf(Funnel funnel) {
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < funnel.size(); i++) {
            candidates.add(new Candidate(funnel.position(i), funnel.weight(i), funnel.agreement(i), funnel.distance(i)));
        }
        return candidates;
    }

    /**
     * The funnel the slow way: the entries sorted, the agreement of each with the context compared from
     * the bytes, the two sides merged by weight, the above side first among equals.
     */
    private static List<Candidate> bruteForce(byte[] text, int depth, int reach, Weighting weighting, int position,
                                              int entries) {
        ContextOrder order = ContextOrder.byLastBytes(SegmentBuffer.of(text), depth);
        SortedListContextIndex sorted = new SortedListContextIndex(order);
        for (int entry = 0; entry < entries; entry++) {
            sorted.insert(entry);
        }
        List<Candidate> funnel = new ArrayList<>();
        if (entries == 0 || position < 4) {
            return funnel;
        }
        int threshold = Weighting.floorLog2((int) (17L * entries / 500));
        int slot = sorted.rank(position);
        int above = 0;
        int below = 0;
        while (true) {
            int aboveWeight = above < reach && slot + above < entries
                    ? weightOf(text, depth, weighting, threshold, position, sorted.cursorAt(slot + above).position(),
                    above + 1) : 0;
            int belowWeight = below < reach && slot - 1 - below >= 0
                    ? weightOf(text, depth, weighting, threshold, position, sorted.cursorAt(slot - 1 - below).position(),
                    below + 1) : 0;
            if (Math.max(aboveWeight, belowWeight) == 0) {
                return funnel;
            }
            if (aboveWeight >= belowWeight) {
                int entry = sorted.cursorAt(slot + above).position();
                funnel.add(new Candidate(entry, aboveWeight, ContextBits.between(text, position, entry, depth), above + 1));
                above++;
            } else {
                int entry = sorted.cursorAt(slot - 1 - below).position();
                funnel.add(new Candidate(entry, belowWeight, ContextBits.between(text, position, entry, depth),
                        -(below + 1)));
                below++;
            }
        }
    }

    private static int weightOf(byte[] text, int depth, Weighting weighting, int threshold, int position, int entry,
                                int distance) {
        return weighting.of(ContextBits.between(text, position, entry, depth), threshold, distance);
    }

    private static List<Candidate> walked(byte[] text, int chunkCapacity, int depth, int reach, Weighting weighting,
                                          int position, int entries) {
        SegmentBuffer segment = SegmentBuffer.of(text);
        ChunkedContextIndex index = new ChunkedContextIndex(ContextOrder.byLastBytes(segment, depth), chunkCapacity);
        for (int entry = entries - 1; entry >= 0; entry--) {
            index.insert(entry);
        }
        FunnelWalk walk = new FunnelWalk(index, segment, reach, depth);
        Funnel funnel = new Funnel(walk.capacity());
        walk.fill(position, weighting, funnel);
        return candidatesOf(funnel);
    }

    @Test
    void aContextThatRepeatsEarlierTextFindsTheEntriesOfTheRepeat() {
        byte[] text = "the cat and the cat and the cat and the cat".getBytes();
        int position = text.length - 4;

        List<Candidate> funnel = walked(text, 4, 255, 8, Weighting.POSITION, position, position);

        assertThat(funnel).isNotEmpty();
        assertThat(funnel.getFirst().weight()).isPositive();
        assertThat(funnel).isEqualTo(bruteForce(text, 255, 8, Weighting.POSITION, position, position));
    }

    @Test
    void anEmptyDictionaryOrATooShortContextHasNoCandidates() {
        byte[] text = "abcabcabc".getBytes();

        assertThat(walked(text, 4, 255, 8, Weighting.POSITION, 6, 0)).isEmpty();
        assertThat(walked(text, 4, 255, 8, Weighting.POSITION, 3, 3)).isEmpty();
    }

    @Property
    void theWalkOverTheSharedBytesOfNeighboursIsTheSortedListWalkOverComparedBytes(
            @ForAll("texts") byte[] text, @ForAll @IntRange(min = 2, max = 9) int chunkCapacity,
            @ForAll @IntRange(min = 1, max = 20) int reach, @ForAll @IntRange(min = 1, max = 12) int depth,
            @ForAll Weighting weighting, @ForAll @IntRange(min = 0, max = 150) int position) {
        int at = Math.min(position, text.length);

        List<Candidate> actual = walked(text, chunkCapacity, depth, reach, weighting, at, at);

        assertThat(actual).isEqualTo(bruteForce(text, depth, reach, weighting, at, at));
    }

    @Property
    void aFunnelHoldsNoMoreThanTheReachOfEachSideAndNoWeightOfZero(
            @ForAll("texts") byte[] text, @ForAll @IntRange(min = 1, max = 6) int reach,
            @ForAll @IntRange(min = 0, max = 150) int position) {
        int at = Math.min(position, text.length);

        List<Candidate> funnel = walked(text, 4, 255, reach, Weighting.POSITION, at, at);

        assertThat(funnel).hasSizeLessThanOrEqualTo(2 * reach);
        assertThat(funnel).allSatisfy(candidate -> assertThat(candidate.weight()).isPositive());
        assertThat(funnel).extracting(Candidate::position).doesNotHaveDuplicates().allSatisfy(entry ->
                assertThat(entry).isLessThan(Math.max(at, 1)));
    }

    /** Few letters make long agreements, which is where the walk over shared bytes could go wrong. */
    @Provide
    Arbitrary<byte[]> texts() {
        Arbitrary<byte[]> anyBytes = Arbitraries.bytes().array(byte[].class).ofMinSize(1).ofMaxSize(150);
        Arbitrary<byte[]> threeLetters = Arbitraries.bytes().between((byte) 'a', (byte) 'c')
                .array(byte[].class).ofMinSize(1).ofMaxSize(150);
        Arbitrary<byte[]> twoLetters = Arbitraries.bytes().between((byte) 'a', (byte) 'b')
                .array(byte[].class).ofMinSize(1).ofMaxSize(150);
        return Arbitraries.oneOf(anyBytes, threeLetters, twoLetters);
    }
}
