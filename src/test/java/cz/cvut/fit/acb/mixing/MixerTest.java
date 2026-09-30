package cz.cvut.fit.acb.mixing;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MixerTest {

    /** Mixes one input that knows the bit and one that is noise, and returns the cost of the last thousand bits. */
    private static double costOfLearning(Mixer mixer, Random random, int set) {
        double bits = 0;
        for (int i = 0; i < 20_000; i++) {
            int bit = random.nextInt(2);
            mixer.addProbability(bit != 0 ? Logistic.ONE * 7 / 8 : Logistic.ONE / 8);
            mixer.addProbability(1 + random.nextInt(Logistic.ONE - 1));
            mixer.select(0, set);
            int probability = mixer.mix();
            if (i >= 19_000) {
                bits -= Math.log((bit != 0 ? probability : Logistic.ONE - probability) / (double) Logistic.ONE) / Math.log(2);
            }
            mixer.update(bit);
        }
        return bits;
    }

    @Test
    void theInputThatKnowsTheBitWinsTheWeight() {
        Mixer mixer = new Mixer(2, 16, 1 << 14, 1);

        double bits = costOfLearning(mixer, new Random(20260930L), 0);

        assertThat(bits / 1_000).isLessThan(0.3);
    }

    @Test
    void theSetsOfABankLearnApart() {
        Mixer mixer = new Mixer(1, 16, 1 << 14, 2);
        for (int i = 0; i < 5_000; i++) {
            mixer.add(1_000);
            mixer.select(0, 1);
            mixer.mix();
            mixer.update(0);
        }

        mixer.add(1_000);
        mixer.select(0, 0);
        int untouched = mixer.mix();
        mixer.update(1);
        mixer.add(1_000);
        mixer.select(0, 1);
        int taught = mixer.mix();

        assertThat(untouched).isGreaterThan(Logistic.ONE / 2);
        assertThat(taught).isLessThan(Logistic.ONE / 2);
    }

    @Test
    void twoBanksGiveTheAverageOfWhatEachSays() {
        Mixer one = new Mixer(1, 16, 1 << 16, 1);
        Mixer two = new Mixer(1, 16, 1 << 16, 1, 1);

        one.add(512);
        two.add(512);
        one.select(0, 0);
        two.select(0, 0);
        two.select(1, 0);

        assertThat(two.mix()).isEqualTo(one.mix());
    }

    @Test
    void aBitWithTheWrongNumberOfInputsIsRefused() {
        Mixer mixer = new Mixer(2, 16, 1 << 14, 1);
        mixer.add(0);

        assertThatThrownBy(mixer::mix).isInstanceOf(IllegalStateException.class);
    }
}
