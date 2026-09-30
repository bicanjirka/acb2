package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.ConfiguredACBProvider;
import cz.cvut.fit.acb.TripletCoding;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ContinuationsTest {

    private static final CompressionSettings SETTINGS = CompressionSettings.defaults()
            .withTripletCoding(TripletCoding.VALACH);

    private static EncoderDictionary dictionaryOver(String text, int entries) {
        EncoderDictionary dictionary = new ConfiguredACBProvider(SETTINGS)
                .encoderDictionary(SegmentBuffer.of(text.getBytes(StandardCharsets.US_ASCII)));
        dictionary.update(0, entries);
        return dictionary;
    }

    @Test
    void withNothingMatchedEveryCandidateNamesItsFirstByte() {
        EncoderDictionary dictionary = dictionaryOver("abcXabdYab", 8);

        ByteSet continuations = dictionary.continuations(8, -1, 0);

        assertThat(continuations).isEqualTo(ByteSet.of('a', 'b', 'c', 'd', 'X', 'Y'));
    }

    @Test
    void afterAMatchOnlyTheCandidatesThatMatchedAsFarNameTheirNextByte() {
        EncoderDictionary dictionary = dictionaryOver("abcXabdYabe", 8);

        ByteSet continuations = dictionary.continuations(8, 0, 2);

        assertThat(continuations).isEqualTo(ByteSet.of('c', 'd'));
    }

    @Test
    void aCandidateWhoseNextByteIsNotKnownYetIsLeftOut() {
        EncoderDictionary dictionary = dictionaryOver("aaaaaaaaaa", 8);

        ByteSet continuations = dictionary.continuations(8, 0, 2);

        assertThat(continuations).isEqualTo(ByteSet.of('a'));
        assertThat(dictionary.continuations(8, 0, 7)).isEqualTo(ByteSet.of('a'));
        assertThat(dictionary.continuations(8, 0, 8)).isEqualTo(ByteSet.none());
    }

    @Test
    void anEmptyDictionaryHasNoContinuations() {
        EncoderDictionary dictionary = dictionaryOver("abc", 0);

        assertThat(dictionary.continuations(0, -1, 0)).isEqualTo(ByteSet.none());
    }
}
