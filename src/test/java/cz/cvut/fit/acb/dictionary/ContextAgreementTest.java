package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.fixtures.ContextBits;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAgreementTest {

    /** The agreement in bits the way the walk gets it: the whole bytes first, then the bits of the byte that broke it. */
    private static int bits(byte[] bytes, int first, int second, int maxBytes) {
        int limit = Math.min(maxBytes, Math.min(first, second));
        return ContextAgreement.bitsAfter(bytes, first, second, ContextAgreement.commonBytes(bytes, first, second, limit),
                limit);
    }

    @Test
    void contextsThatDifferInTheNearestByteAgreeOnTheBitsBeforeTheFirstDifference() {
        byte[] bytes = {'x', 0b0110_0001, 'y', 0b0110_0111};

        assertThat(bits(bytes, 2, 4, 10)).isEqualTo(5);
    }

    @Test
    void contextsThatAgreeForEightBytesOrMoreAreComparedPastTheFirstWord() {
        byte[] bytes = new byte[40];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i % 7);
        }
        bytes[17] ^= 0b0000_0100;

        int bits = bits(bytes, 30, 37, 255);

        assertThat(bits).isEqualTo(ContextBits.between(bytes, 30, 37, 255));
        assertThat(bits).isGreaterThan(64);
    }

    @Test
    void theAgreementIsCappedAtTheBytesAskedFor() {
        byte[] bytes = new byte[30];

        assertThat(bits(bytes, 20, 28, 5)).isEqualTo(40);
    }

    @Test
    void theAgreementNeverReachesPastTheStartOfTheSegment() {
        byte[] bytes = new byte[30];

        assertThat(bits(bytes, 3, 29, 255)).isEqualTo(24);
    }

    @Test
    void agreementThatIsKnownToStartWithEightEqualBytesIsCountedFromThere() {
        byte[] bytes = new byte[60];
        bytes[30] = 1;

        assertThat(ContextAgreement.commonBytesFrom(bytes, 45, 58, 8, 40)).isEqualTo(ContextAgreement.commonBytes(bytes, 45,
                58, 40));
    }

    @Property
    void theAgreementIsTheEqualBitsCountedOneByOne(@ForAll("texts") byte[] bytes, @ForAll @IntRange(min = 0, max = 80) int first,
                                                   @ForAll @IntRange(min = 0, max = 80) int second,
                                                   @ForAll @IntRange(min = 0, max = 40) int maxBytes) {
        int a = Math.min(first, bytes.length);
        int b = Math.min(second, bytes.length);

        assertThat(bits(bytes, a, b, maxBytes)).isEqualTo(ContextBits.between(bytes, a, b, maxBytes));
    }

    @Property
    void theWholeBytesOfAgreementAreTheBitsOfItDividedByEight(@ForAll("texts") byte[] bytes,
                                                              @ForAll @IntRange(min = 0, max = 80) int first,
                                                              @ForAll @IntRange(min = 0, max = 80) int second) {
        int a = Math.min(first, bytes.length);
        int b = Math.min(second, bytes.length);
        int limit = Math.min(a, b);

        assertThat(ContextAgreement.commonBytes(bytes, a, b, limit)).isEqualTo(ContextBits.between(bytes, a, b, limit) / 8);
    }

    @Property
    void theNearestBytesAsANumberOrderLikeTheContextsTheyStartAndTellWhereTheyDiffer(
            @ForAll("texts") byte[] bytes, @ForAll @IntRange(min = 0, max = 80) int first,
            @ForAll @IntRange(min = 0, max = 80) int second) {
        int a = Math.min(first, bytes.length);
        int b = Math.min(second, bytes.length);
        long prefixA = ContextAgreement.nearestBytes(bytes, a, Math.min(a, Long.BYTES));
        long prefixB = ContextAgreement.nearestBytes(bytes, b, Math.min(b, Long.BYTES));
        int limit = Math.min(Math.min(a, b), Long.BYTES);

        if (prefixA != prefixB) {
            assertThat(Math.min(limit, Long.numberOfLeadingZeros(prefixA ^ prefixB) >>> 3))
                    .isEqualTo(ContextAgreement.commonBytes(bytes, a, b, limit));
        }
    }

    /** Two letters agree for long; uniform bytes rarely do, so both are tried. */
    @Provide
    Arbitrary<byte[]> texts() {
        Arbitrary<byte[]> anyBytes = Arbitraries.bytes().array(byte[].class).ofMinSize(1).ofMaxSize(80);
        Arbitrary<byte[]> fewSymbols = Arbitraries.bytes().between((byte) 'a', (byte) 'b')
                .array(byte[].class).ofMinSize(1).ofMaxSize(80);
        return Arbitraries.oneOf(anyBytes, fewSymbols);
    }
}
