package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.dictionary.Funnel;
import cz.cvut.fit.acb.dictionary.FunnelFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PositionModelTest {

    private static final Funnel THREE = FunnelFixtures.funnelOf(new int[]{10, 20, 30}, new int[]{900, 300, 100});

    @Test
    void theDistributionHasTheEscapeFirstAndOneSymbolPerCandidateInProportionToItsWeight() {
        CumulativeTable table = new CumulativeTable();

        new PositionModel().fill(THREE, table);

        assertThat(table.frequency(1)).isEqualTo(900);
        assertThat(table.frequency(2)).isEqualTo(300);
        assertThat(table.frequency(3)).isEqualTo(100);
        assertThat(table.frequency(0)).isPositive();
    }

    @Test
    void anEscapeThatKeepsComingGrowsHeavierAndAMatchThatKeepsComingShrinksIt() {
        CumulativeTable table = new CumulativeTable();
        PositionModel model = new PositionModel();
        model.fill(THREE, table);
        int fresh = table.frequency(0);

        for (int i = 0; i < 20; i++) {
            model.record(THREE, true);
        }
        model.fill(THREE, table);
        int afterEscapes = table.frequency(0);
        for (int i = 0; i < 100; i++) {
            model.record(THREE, false);
        }
        model.fill(THREE, table);

        assertThat(afterEscapes).isGreaterThan(fresh);
        assertThat(table.frequency(0)).isLessThan(fresh);
    }

    @Test
    void funnelsWhoseBestCandidateWeighsVeryDifferentlyAreCountedApart() {
        Funnel strong = FunnelFixtures.funnelOf(new int[]{1}, new int[]{20_000});
        Funnel weak = FunnelFixtures.funnelOf(new int[]{1}, new int[]{3});
        CumulativeTable table = new CumulativeTable();
        PositionModel model = new PositionModel();

        for (int i = 0; i < 50; i++) {
            model.record(strong, false);
            model.record(weak, true);
        }
        model.fill(strong, table);
        double strongEscape = (double) table.frequency(0) / table.total();
        model.fill(weak, table);
        double weakEscape = (double) table.frequency(0) / table.total();

        assertThat(weakEscape).isGreaterThan(strongEscape * 5);
    }

    @Test
    void weightsTooBigForTheRangeCoderAreScaledDownWithoutDroppingACandidate() {
        Funnel heavy = FunnelFixtures.funnelOf(new int[]{1, 2, 3}, new int[]{Integer.MAX_VALUE / 4, 1, 1});
        CumulativeTable table = new CumulativeTable();

        new PositionModel().fill(heavy, table);

        assertThat(table.total()).isLessThan(1 << 20);
        assertThat(table.frequency(2)).isPositive();
        assertThat(table.frequency(3)).isPositive();
    }
}
