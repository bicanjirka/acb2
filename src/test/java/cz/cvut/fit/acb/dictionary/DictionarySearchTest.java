package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.ACBProviderImpl;
import cz.cvut.fit.acb.CompressionSettings;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DictionarySearchTest {

    private static Dictionary dictionaryOf(String text) {
        return new ACBProviderImpl(CompressionSettings.defaults())
                .getDictionary(new ByteArray(text.getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void amongEqualMatchesTheNearestToTheContextIsChosen() {
        Dictionary dictionary = dictionaryOf("ababababab");
        dictionary.update(0, 8);

        DictionaryInfo found = dictionary.search(8);

        assertThat(found.getLength()).isEqualTo(2);
        assertThat(found.getContent()).isEqualTo(found.getContext());
    }

    @Test
    void theFirstRankIsACandidateWhenTheWindowIsClampedAtIt() {
        Dictionary dictionary = dictionaryOf("aaa");
        dictionary.update(0, 1);

        DictionaryInfo found = dictionary.search(1);

        assertThat(found.getContext()).isZero();
        assertThat(found.getContent()).isZero();
        assertThat(found.getLength()).isEqualTo(2);
    }

    @Test
    void anEmptyDictionaryFindsNoMatch() {
        DictionaryInfo found = dictionaryOf("aaa").search(0);

        assertThat(found.getContent()).isEqualTo(-1);
        assertThat(found.getLength()).isZero();
    }
}
