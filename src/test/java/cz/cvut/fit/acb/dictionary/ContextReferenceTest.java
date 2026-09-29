package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.ConfiguredACBProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ContextReferenceTest {

    /** A dictionary holding the given positions of {@code text}, whose length is the position asked about. */
    private static DecoderDictionary dictionaryOf(String text, int... positions) {
        DecoderDictionary dictionary = new ConfiguredACBProvider(CompressionSettings.defaults())
                .decoderDictionary(SegmentBuffer.of(text.getBytes(StandardCharsets.US_ASCII)));
        for (int position : positions) {
            dictionary.update(position, 1);
        }
        return dictionary;
    }

    @Test
    void theNeighbourAboveIsTheReferenceWhenItsContextAgreesLonger() {
        // position 12 has the context "a b d", between "a a" at 4 and "a b z" at 8
        DecoderDictionary dictionary = dictionaryOf("qqaaqzbaqdba", 4, 8);

        assertThat(dictionary.contextRank(12)).isEqualTo(1);
    }

    @Test
    void theNeighbourBelowIsTheReferenceWhenItsContextAgreesLonger() {
        // position 12 has the context "a b d", between "a b c" at 4 and "a c" at 8
        DecoderDictionary dictionary = dictionaryOf("qcbaqzcaqdba", 4, 8);

        assertThat(dictionary.contextRank(12)).isZero();
    }

    @Test
    void aTieGoesToTheNeighbourBelow() {
        // position 12 has the context "a b d", between "a b c" at 4 and "a b z" at 8
        DecoderDictionary dictionary = dictionaryOf("qcbaqzbaqdba", 4, 8);

        assertThat(dictionary.contextRank(12)).isZero();
    }

    @Test
    void aContextBelowEveryEntryIsReferredToTheFirst() {
        DecoderDictionary dictionary = dictionaryOf("qcbaqzbaqdba", 8);

        assertThat(dictionary.contextRank(12)).isZero();
    }

    @Test
    void aContextAboveEveryEntryIsReferredToTheLast() {
        DecoderDictionary dictionary = dictionaryOf("qcbaqzbaqdba", 4);

        assertThat(dictionary.contextRank(12)).isZero();
    }

    @Test
    void anEmptyDictionaryHasNoReference() {
        assertThat(dictionaryOf("qcbaqzbaqdba").contextRank(12)).isEqualTo(-1);
    }
}
