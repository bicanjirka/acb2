package cz.cvut.fit.acb.coding;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveFrequencyModelPropertiesTest {

    @Property
    void afterAnyIncrementsItAgreesWithANaiveModelOnEverySymbol(
            @ForAll("startingFrequencies") int[] initial,
            @ForAll @Size(max = 3_000) List<@IntRange(min = 0, max = 1_000) Integer> seen) {
        AdaptiveFrequencyModel actual = new AdaptiveFrequencyModel(initial);
        NaiveFrequencyModel expected = new NaiveFrequencyModel(initial);

        for (int value : seen) {
            int symbol = value % initial.length;
            actual.increment(symbol);
            expected.increment(symbol);
        }

        assertThat(actual.total()).isEqualTo(expected.total());
        for (int symbol = 0; symbol < initial.length; symbol++) {
            assertThat(actual.frequency(symbol)).isEqualTo(expected.frequency(symbol));
            assertThat(actual.cumulative(symbol)).isEqualTo(expected.cumulative(symbol));
        }
        for (int target = 0; target < expected.total(); target += 1 + expected.total() / 200) {
            assertThat(actual.symbolAt(target)).as("symbol at %d", target).isEqualTo(expected.symbolAt(target));
        }
    }

    @Property
    void theTotalNeverPassesTheLimitAndNoSymbolFallsToZero(
            @ForAll("startingFrequencies") int[] initial,
            @ForAll @Size(max = 3_000) List<@IntRange(min = 0, max = 1_000) Integer> seen) {
        AdaptiveFrequencyModel model = new AdaptiveFrequencyModel(initial);

        for (int value : seen) {
            model.increment(value % initial.length);
        }

        assertThat(model.total()).isLessThanOrEqualTo(AdaptiveFrequencyModel.limitFor(initial.length));
        for (int symbol = 0; symbol < initial.length; symbol++) {
            assertThat(model.frequency(symbol)).isPositive();
        }
    }

    @Provide
    Arbitrary<int[]> startingFrequencies() {
        return Arbitraries.integers().between(1, 300).array(int[].class).ofMinSize(1).ofMaxSize(40);
    }
}
