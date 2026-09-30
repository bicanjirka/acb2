package cz.cvut.fit.acb.mixing;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProbabilityMapTest {

    @Test
    void aNewMapLeavesAProbabilityAboutAsItIs() {
        ProbabilityMap map = new ProbabilityMap(1, 6);

        for (int probability : new int[]{100, 1_000, 2_048, 3_000, 4_000}) {
            assertThat(map.refine(probability, 0)).isCloseTo(probability, within(probability / 10 + 16));
        }
    }

    @Test
    void anOverconfidentInputIsTamed() {
        ProbabilityMap map = new ProbabilityMap(2, 6);
        Random random = new Random(20260930L);
        int claimed = Logistic.ONE * 95 / 100;

        for (int i = 0; i < 20_000; i++) {
            map.refine(claimed, 1);
            map.update(random.nextInt(10) < 6 ? 1 : 0);
        }

        assertThat(map.refine(claimed, 1) / (double) Logistic.ONE).isCloseTo(0.6, within(0.08));
        assertThat(map.refine(claimed, 0)).isCloseTo(claimed, within(claimed / 10));
    }
}
