package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.ConfiguredACBProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DictionarySearchTest {

    private static EncoderDictionary dictionaryOf(String text) {
        return new ConfiguredACBProvider(CompressionSettings.defaults())
                .encoderDictionary(SegmentBuffer.of(text.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void amongEqualMatchesTheNearestToTheContextIsChosen() {
        EncoderDictionary dictionary = dictionaryOf("ababababab");
        dictionary.update(0, 8);

        SearchResult found = dictionary.search(8);

        assertThat(found).isEqualTo(SearchResult.hit(0, 2));
    }

    @Test
    void theFirstRankIsACandidateWhenTheWindowIsClampedAtIt() {
        EncoderDictionary dictionary = dictionaryOf("aaa");
        dictionary.update(0, 1);

        SearchResult found = dictionary.search(1);

        assertThat(found).isEqualTo(SearchResult.hit(0, 2));
    }

    @Test
    void anEmptyDictionaryFindsNoMatch() {
        SearchResult found = dictionaryOf("aaa").search(0);

        assertThat(found).isEqualTo(SearchResult.miss());
    }
}
