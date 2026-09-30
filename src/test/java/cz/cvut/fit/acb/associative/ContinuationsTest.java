package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.dictionary.FunnelFixtures;
import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContinuationsTest {

    /** Candidates at 0, 1, 2 and 3 go on with a, b, a and c; the step is at 8. */
    private static final SegmentBuffer TEXT = SegmentBuffer.of("abacxxxx".getBytes());

    private static Continuations gathered(int[] weights) {
        Continuations next = new Continuations(8);
        next.start(FunnelFixtures.funnelOf(new int[]{0, 1, 2, 3}, weights));
        next.gather(TEXT, 0, 8);
        return next;
    }

    @Test
    void theBytesTheCandidatesGoOnWithAreRankedHeaviestFirstWithTheirWeightsAndCounts() {
        Continuations next = gathered(new int[]{5, 20, 7, 1});

        assertThat(next.distinct()).isEqualTo(3);
        assertThat(new int[]{next.byteAt(0), next.byteAt(1), next.byteAt(2)}).containsExactly('b', 'a', 'c');
        assertThat(next.weight(1)).isEqualTo(12);
        assertThat(next.count(1)).isEqualTo(2);
        assertThat(next.total()).isEqualTo(33);
        assertThat(next.nearest(1)).isEqualTo(6);
        assertThat(next.rankOf('c')).isEqualTo(2);
        assertThat(next.rankOf('z')).isEqualTo(-1);
    }

    @Test
    void keepingAByteLeavesOnlyTheCandidatesThatGoOnWithIt() {
        Continuations next = gathered(new int[]{5, 20, 7, 1});

        next.keep(TEXT, 0, 'a');
        next.gather(TEXT, 1, 8);

        assertThat(next.liveCount()).isEqualTo(2);
        assertThat(new int[]{next.livePosition(0), next.livePosition(1)}).containsExactly(0, 2);
        assertThat(next.distinct()).isEqualTo(2);
        assertThat(next.byteAt(0)).isEqualTo('c');
    }

    @Test
    void aRepeatIsMarkedOnTheByteItGoesOnWith() {
        Continuations next = new Continuations(8);
        next.start(FunnelFixtures.funnelOf(new int[]{0, 1}, new int[]{5, 20}));
        next.addRepeat(3, 2);

        next.gather(TEXT, 0, 8);

        assertThat(next.anyRepeat()).isTrue();
        assertThat(next.repeated(next.rankOf('c'))).isTrue();
        assertThat(next.repeated(next.rankOf('b'))).isFalse();
    }
}
