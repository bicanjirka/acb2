package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ForecastedLiteralsTest {

    @Test
    void anExcludedByteHasNoFrequencyAndTheOthersKeepTheirs() {
        ForecastedLiterals literals = new ForecastedLiterals();
        CumulativeTable table = new CumulativeTable();
        literals.reset();
        literals.exclude('a');
        literals.exclude('b');

        boolean possible = literals.fill(table);

        assertThat(possible).isTrue();
        assertThat(table.frequency('a')).isZero();
        assertThat(table.frequency('b')).isZero();
        assertThat(table.frequency('c')).isPositive();
        assertThat(table.total()).isEqualTo(254);
    }

    @Test
    void theNextLiteralStartsWithoutTheExclusionsAndVotesOfTheLast() {
        ForecastedLiterals literals = new ForecastedLiterals();
        CumulativeTable table = new CumulativeTable();
        literals.reset();
        literals.exclude('a');
        literals.vote('q', 500);
        literals.fill(table);

        literals.reset();
        literals.fill(table);

        assertThat(table.frequency('a')).isEqualTo(table.frequency('c'));
        assertThat(table.frequency('q')).isEqualTo(table.frequency('c'));
    }

    @Test
    void aVoteRaisesABytesFrequencyByAShareOfTheTotalAsBigAsItsShareOfTheVotes() {
        ForecastedLiterals literals = new ForecastedLiterals();
        CumulativeTable table = new CumulativeTable();
        literals.reset();
        literals.vote('e', 300);
        literals.vote('t', 100);

        literals.fill(table);

        assertThat(table.frequency('e')).isEqualTo(1 + 256 * 3 / 4);
        assertThat(table.frequency('t')).isEqualTo(1 + 256 / 4);
        assertThat(table.frequency('x')).isEqualTo(1);
    }

    @Test
    void aByteThatIsExcludedGetsNoVote() {
        ForecastedLiterals literals = new ForecastedLiterals();
        CumulativeTable table = new CumulativeTable();
        literals.reset();
        literals.exclude('e');
        literals.vote('e', 300);
        literals.vote('t', 100);

        literals.fill(table);

        assertThat(table.frequency('e')).isZero();
        assertThat(table.frequency('t')).isEqualTo(1 + 255);
    }

    @Test
    void everyByteExcludedLeavesNothingToCode() {
        ForecastedLiterals literals = new ForecastedLiterals();
        literals.reset();
        for (int symbol = 0; symbol < 256; symbol++) {
            literals.exclude(symbol);
        }

        assertThat(literals.fill(new CumulativeTable())).isFalse();
    }

    @Test
    void aCodedLiteralMakesItLikelierNextTime() {
        ForecastedLiterals literals = new ForecastedLiterals();
        CumulativeTable table = new CumulativeTable();
        for (int i = 0; i < 10; i++) {
            literals.update('e');
        }

        literals.reset();
        literals.fill(table);

        assertThat(table.frequency('e')).isGreaterThan(table.frequency('z'));
    }
}
