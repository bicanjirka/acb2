package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class LengthModelTest {

    private static int[] flat(int symbols) {
        int[] table = new int[symbols];
        Arrays.fill(table, 1);
        return table;
    }

    @Test
    void theDistributionCoversOnlyTheExcessesAMatchCanStillHave() {
        CumulativeTable table = new CumulativeTable();

        new LengthModel(flat(16)).fill(table, 5);

        assertThat(table.cumulative(4) + table.frequency(4)).isEqualTo(table.total());
        assertThat(table.frequency(4)).isPositive();
    }

    @Test
    void aLengthThatEarlierMatchesHadIsLikelierThanOneTheyDidNot() {
        LengthModel model = new LengthModel(flat(16));
        CumulativeTable table = new CumulativeTable();
        for (int i = 0; i < 30; i++) {
            model.update(2);
        }
        model.update(7);
        model.fill(table, 16);

        assertThat(table.frequency(2)).isGreaterThan(5 * table.frequency(7));
    }

    @Test
    void aLengthThatALaterCandidateSharesGainsWeightForOneMatchOnly() {
        LengthModel model = new LengthModel(flat(16));
        CumulativeTable table = new CumulativeTable();
        model.fill(table, 16);
        int plain = table.frequency(9);

        model.share(9);
        model.fill(table, 16);
        int boosted = table.frequency(9);
        model.fill(table, 16);

        assertThat(boosted).isGreaterThan(plain);
        assertThat(table.frequency(9)).isEqualTo(plain);
    }

    @Test
    void aSharedLengthBeyondWhatIsAllowedIsForgottenAndChangesNothing() {
        LengthModel model = new LengthModel(flat(16));
        CumulativeTable table = new CumulativeTable();
        model.fill(table, 4);
        int plain = table.total();

        model.share(12);
        model.fill(table, 4);

        assertThat(table.total()).isEqualTo(plain);
    }
}
