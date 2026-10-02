package cz.cvut.fit.acb.coding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CumulativeTableTest {

    private static CumulativeTable of(int... frequencies) {
        CumulativeTable table = new CumulativeTable();
        table.begin(frequencies.length);
        for (int symbol = 0; symbol < frequencies.length; symbol++) {
            table.set(symbol, frequencies[symbol]);
        }
        table.seal();
        return table;
    }

    @Test
    void theCumulativeFrequenciesAreTheSumsOfTheOnesBefore() {
        CumulativeTable table = of(3, 1, 4);

        assertThat(table.total()).isEqualTo(8);
        assertThat(table.cumulative(0)).isZero();
        assertThat(table.cumulative(1)).isEqualTo(3);
        assertThat(table.cumulative(2)).isEqualTo(4);
        assertThat(table.frequency(2)).isEqualTo(4);
    }

    @Test
    void everyTargetFallsInTheSymbolWhoseRangeHoldsIt() {
        CumulativeTable table = of(3, 1, 4);

        assertThat(new int[]{table.symbolAt(0), table.symbolAt(2), table.symbolAt(3), table.symbolAt(4),
                table.symbolAt(7)}).containsExactly(0, 0, 1, 2, 2);
    }

    @Test
    void aSymbolOfFrequencyZeroIsNeverFound() {
        CumulativeTable table = of(2, 0, 0, 5, 0);

        for (int target = 0; target < table.total(); target++) {
            assertThat(table.frequency(table.symbolAt(target))).isPositive();
        }
    }

    @Test
    void aDistributionWithNoPossibleSymbolIsNotSealed() {
        CumulativeTable table = of(0, 0, 0);

        assertThat(table.seal()).isFalse();
    }

    @Test
    void frequenciesThatTotalMoreThanTheRangeCoderTakesAreHalvedWithoutLosingASymbol() {
        CumulativeTable table = of(RangeEncoder.MAX_TOTAL, 3, 1);

        assertThat(table.total()).isLessThanOrEqualTo(RangeEncoder.MAX_TOTAL);
        assertThat(table.frequency(1)).isPositive();
        assertThat(table.frequency(2)).isPositive();
    }

    @Test
    void aTableStartedFromCountsHasTheirFrequenciesUpToItsSize() {
        FrequencyCounts counts = new FrequencyCounts(new int[]{3, 1, 4, 1, 5});
        CumulativeTable table = new CumulativeTable();

        table.begin(counts, 3);
        table.set(1, 0);
        table.seal();

        assertThat(table.total()).isEqualTo(7);
        assertThat(table.cumulative(2)).isEqualTo(3);
        assertThat(table.symbolAt(6)).isEqualTo(2);
    }

    @Test
    void aTargetIsFoundAlikeBeforeAndAfterTheSumsUpToItAreAskedFor() {
        CumulativeTable table = of(3, 0, 1, 4, 2);
        int[] before = new int[table.total()];
        for (int target = 0; target < table.total(); target++) {
            before[target] = of(3, 0, 1, 4, 2).symbolAt(target);
        }

        table.cumulative(5);

        for (int target = 0; target < table.total(); target++) {
            assertThat(table.symbolAt(target)).as("target %d", target).isEqualTo(before[target]);
        }
    }

    @Test
    void aTableRefilledForFewerSymbolsForgetsTheOldOnes() {
        CumulativeTable table = of(1, 1, 1, 1, 1);

        table.begin(2);
        table.set(0, 0);
        table.set(1, 7);
        table.seal();

        assertThat(table.total()).isEqualTo(7);
        assertThat(table.symbolAt(6)).isEqualTo(1);
    }
}
