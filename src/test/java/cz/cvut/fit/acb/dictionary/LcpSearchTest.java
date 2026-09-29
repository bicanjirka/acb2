package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.ConfiguredACBProvider;
import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LcpSearchTest {

    private static final CompressionSettings LCP = CompressionSettings.defaults()
            .withTripletCoding(TripletCoding.LCP);

    @Test
    void aMatchImpliesTheBytesItSharesWithTheContentsBelowIt() throws MalformedStreamException {
        // at 8 the text goes on "abcq"; the contents at 0 ("abbX...") and 4 ("abcY...") share "ab"
        SegmentBuffer segment = SegmentBuffer.of("abbXabcYabcq".getBytes(StandardCharsets.US_ASCII));
        ConfiguredACBProvider provider = new ConfiguredACBProvider(LCP);
        EncoderDictionary encoder = provider.encoderDictionary(segment);
        DecoderDictionary decoder = provider.decoderDictionary(segment);
        encoder.update(0, 1);
        encoder.update(4, 1);
        decoder.update(0, 1);
        decoder.update(4, 1);

        SearchResult found = encoder.search(8);

        SearchResult.Hit hit = (SearchResult.Hit) found;
        assertThat(hit.length()).isEqualTo(3);
        assertThat(hit.implied()).isEqualTo(2);
        assertThat(decoder.impliedLength(8, hit.distance())).isEqualTo(2);
    }

    @Test
    void whenNoContentSortsBelowTheBestNothingIsImplied() {
        SegmentBuffer segment = SegmentBuffer.of("abcXabdYabcq".getBytes(StandardCharsets.US_ASCII));
        EncoderDictionary encoder = new ConfiguredACBProvider(LCP).encoderDictionary(segment);
        encoder.update(0, 1);
        encoder.update(4, 1);

        SearchResult found = encoder.search(8);

        assertThat(found).isInstanceOfSatisfying(SearchResult.Hit.class, hit -> {
            assertThat(hit.length()).isEqualTo(3);
            assertThat(hit.implied()).isZero();
        });
    }

    @Test
    void theLengthASearchLeavesToBeWrittenIsAtLeastOneAndAtMostTheLongestLength() throws MalformedStreamException {
        CompressionSettings settings = LCP.withLengthBits(2);
        byte[] text = GeneratedInput.text(6000).bytes();
        SegmentBuffer segment = SegmentBuffer.of(text);
        ConfiguredACBProvider provider = new ConfiguredACBProvider(settings);
        EncoderDictionary encoder = provider.encoderDictionary(segment);
        SegmentBuffer decoded = SegmentBuffer.empty();
        DecoderDictionary decoder = provider.decoderDictionary(decoded);
        int hits = 0;
        int longerThanTheMaximum = 0;

        for (int idx = 0; idx < text.length; idx++) {
            if (encoder.search(idx) instanceof SearchResult.Hit hit) {
                hits++;
                assertThat(hit.length() - hit.implied()).isBetween(1, settings.maxLength());
                assertThat(decoder.impliedLength(idx, hit.distance())).isEqualTo(hit.implied());
                longerThanTheMaximum += hit.length() > settings.maxLength() ? 1 : 0;
            }
            encoder.update(idx, 1);
            decoded.append(text[idx]);
            decoder.update(idx, 1);
        }

        assertThat(hits).isPositive();
        assertThat(longerThanTheMaximum).as("matches beyond the longest length a triplet carries").isPositive();
    }

    @Test
    void aDistanceOutsideTheWindowIsMalformedNotAnAnswer() {
        SegmentBuffer segment = SegmentBuffer.of("abbXabcYabcq".getBytes(StandardCharsets.US_ASCII));
        DecoderDictionary decoder = new ConfiguredACBProvider(LCP).decoderDictionary(segment);
        decoder.update(0, 1);

        assertThatThrownBy(() -> decoder.impliedLength(8, 5))
                .isInstanceOf(MalformedStreamException.class);
    }
}
