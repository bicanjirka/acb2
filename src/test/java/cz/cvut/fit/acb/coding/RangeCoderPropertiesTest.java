package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RangeCoderPropertiesTest {

    @Property
    void anySymbolsOfAnyStaticDistributionDecodeToThemselves(
            @ForAll("distributions") int[] frequencies,
            @ForAll @Size(max = 400) List<@IntRange(min = 0, max = 10_000) Integer> chosen)
            throws MalformedStreamException {
        int total = Arrays.stream(frequencies).sum();
        int[] cumulative = new int[frequencies.length + 1];
        for (int i = 0; i < frequencies.length; i++) {
            cumulative[i + 1] = cumulative[i] + frequencies[i];
        }
        RangeEncoder encoder = new RangeEncoder();
        for (int value : chosen) {
            int symbol = value % frequencies.length;
            encoder.encode(cumulative[symbol], frequencies[symbol], total);
        }
        encoder.finish();
        RangeDecoder decoder = new RangeDecoder(encoder.toArray());

        for (int value : chosen) {
            int expected = value % frequencies.length;
            int target = decoder.target(total);
            int symbol = 0;
            while (cumulative[symbol + 1] <= target) {
                symbol++;
            }
            decoder.consume(cumulative[symbol], frequencies[symbol]);
            assertThat(symbol).isEqualTo(expected);
        }
    }

    /** One symbol that takes nearly the whole total, next to symbols of frequency 1, tests the carries. */
    @Property
    void aHeavilySkewedDistributionSurvivesLongRunsOfTheLikelySymbol(
            @ForAll @IntRange(min = 1, max = 3_000) int run, @ForAll @IntRange(min = 2, max = 50) int rare)
            throws MalformedStreamException {
        int total = RangeEncoder.MAX_TOTAL;
        RangeEncoder encoder = new RangeEncoder();
        for (int i = 0; i < run; i++) {
            encoder.encode(0, total - rare, total);
        }
        encoder.encode(total - rare, 1, total);
        encoder.finish();
        RangeDecoder decoder = new RangeDecoder(encoder.toArray());

        for (int i = 0; i < run; i++) {
            int target = decoder.target(total);
            assertThat(target).isLessThan(total - rare);
            decoder.consume(0, total - rare);
        }
        assertThat(decoder.target(total)).isEqualTo(total - rare);
    }

    @Provide
    Arbitrary<int[]> distributions() {
        Arbitrary<int[]> flat = Arbitraries.integers().between(1, 50).array(int[].class).ofMinSize(1).ofMaxSize(30);
        Arbitrary<int[]> skewed = Arbitraries.integers().between(1, 100_000).array(int[].class)
                .ofMinSize(2).ofMaxSize(6);
        return Arbitraries.oneOf(flat, skewed);
    }
}
