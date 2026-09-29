package cz.cvut.fit.acb.dictionary;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LcpMatchesTest {

    private static LcpMatches over(String text) {
        return new LcpMatches(SegmentBuffer.of(text.getBytes(StandardCharsets.ISO_8859_1)));
    }

    @Test
    void contentsAreComparedByTheBytesFromTheirPositionOn() {
        LcpMatches matches = over("abbxabcx");

        assertThat(matches.compare(8, 0, 4)).isNegative();
        assertThat(matches.compare(8, 4, 0)).isPositive();
        assertThat(matches.commonPrefix(8, 0, 4)).isEqualTo(2);
    }

    @Test
    void aContentCutShortByThePositionSortsBeforeALongerOneItIsAPrefixOf() {
        LcpMatches matches = over("abxab");

        assertThat(matches.compare(5, 3, 0)).isNegative();
        assertThat(matches.commonPrefix(5, 3, 0)).isEqualTo(2);
    }

    @Test
    void bytesAreComparedAsUnsigned() {
        LcpMatches matches = over("\u0080ax\u007fax");

        assertThat(matches.compare(6, 0, 3)).isPositive();
    }

    @Test
    void theImpliedLengthIsTheLongestPrefixSharedWithACandidateBelowTheBest() {
        LcpMatches matches = over("abbxabcxabdx");

        int implied = matches.impliedLength(12, 4, new int[]{0, 4, 8});

        assertThat(implied).isEqualTo(2);
    }

    @Test
    void candidatesAboveTheBestImplyNothing() {
        LcpMatches matches = over("abdxabcxabbx");

        int implied = matches.impliedLength(12, 4, new int[]{0, 4});

        assertThat(implied).isZero();
    }
}
