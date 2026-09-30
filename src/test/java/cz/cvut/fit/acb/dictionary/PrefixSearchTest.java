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

/** {@link MatchRule#NEAREST_WITH_PREFIX}, through the dictionaries that {@link TripletCoding#PREFIX} uses. */
class PrefixSearchTest {

    private static final CompressionSettings PREFIX = CompressionSettings.defaults()
            .withTripletCoding(TripletCoding.PREFIX);

    private static EncoderDictionary encoderOver(SegmentBuffer segment, int... entries) {
        EncoderDictionary encoder = new ConfiguredACBProvider(PREFIX).encoderDictionary(segment);
        for (int entry : entries) {
            encoder.update(entry, 1);
        }
        return encoder;
    }

    private static DecoderDictionary decoderOver(SegmentBuffer segment, int... entries) {
        DecoderDictionary decoder = new ConfiguredACBProvider(PREFIX).decoderDictionary(segment);
        for (int entry : entries) {
            decoder.update(entry, 1);
        }
        return decoder;
    }

    @Test
    void contentsWalkedAfterTheBestImplyNothingWhateverTheyShareWithIt() throws MalformedStreamException {
        // at 8 the text goes on "abcq"; the context leads to 4 ("abcY...") first, and 0 ("abbX...") shares "ab" but comes after
        SegmentBuffer segment = SegmentBuffer.of("abbXabcYabcq".getBytes(StandardCharsets.US_ASCII));

        SearchResult found = encoderOver(segment, 0, 4).search(8);

        SearchResult.Hit hit = (SearchResult.Hit) found;
        assertThat(hit.length()).isEqualTo(3);
        assertThat(hit.implied()).isZero();
        assertThat(decoderOver(segment, 0, 4).impliedLength(8, hit.distance())).isZero();
    }

    @Test
    void aMatchImpliesTheBytesItSharesWithTheContentsWalkedBeforeIt() throws MalformedStreamException {
        // the context leads to 4 ("abbY...") first; the best, 0 ("abcX..."), is walked second and shares "ab" with it
        SegmentBuffer segment = SegmentBuffer.of("abcXabbYabcq".getBytes(StandardCharsets.US_ASCII));

        SearchResult found = encoderOver(segment, 0, 4).search(8);

        SearchResult.Hit hit = (SearchResult.Hit) found;
        assertThat(hit.distance()).isEqualTo(1);
        assertThat(hit.length()).isEqualTo(3);
        assertThat(hit.implied()).isEqualTo(2);
        assertThat(decoderOver(segment, 0, 4).impliedLength(8, hit.distance())).isEqualTo(2);
    }

    @Test
    void aContentThatMatchesAsLongButIsWalkedAfterTheBestImpliesNothing() throws MalformedStreamException {
        SegmentBuffer segment = SegmentBuffer.of("abcXabcYabcq".getBytes(StandardCharsets.US_ASCII));

        SearchResult found = encoderOver(segment, 0, 4).search(8);

        SearchResult.Hit hit = (SearchResult.Hit) found;
        assertThat(hit.implied()).isZero();
        assertThat(decoderOver(segment, 0, 4).impliedLength(8, hit.distance())).isZero();
    }

    @Test
    void ofTwoContentsThatMatchAsLongTheNearestIsTheBestNotTheSmallest() {
        // 0 ("abcX...") sorts below 4 ("abcY..."), and both match "abc"; the context is next to 4
        SegmentBuffer segment = SegmentBuffer.of("abcXabcYabcq".getBytes(StandardCharsets.US_ASCII));
        CompressionSettings smallest = CompressionSettings.defaults().withTripletCoding(TripletCoding.LCP);
        EncoderDictionary smallestFirst = new ConfiguredACBProvider(smallest).encoderDictionary(segment);
        smallestFirst.update(0, 1);
        smallestFirst.update(4, 1);

        SearchResult.Hit nearest = (SearchResult.Hit) encoderOver(segment, 0, 4).search(8);
        SearchResult.Hit lexicographic = (SearchResult.Hit) smallestFirst.search(8);

        assertThat(nearest.distance()).isZero();
        assertThat(lexicographic.distance()).isEqualTo(1);
    }

    @Test
    void aTextThatNoContentStartsLikeIsNoMatch() {
        SegmentBuffer segment = SegmentBuffer.of("abcdefghijk".getBytes(StandardCharsets.US_ASCII));

        SearchResult found = encoderOver(segment, 0, 1, 2, 3).search(4);

        assertThat(found).isSameAs(SearchResult.miss());
    }

    @Test
    void theLengthASearchLeavesToBeWrittenIsAtLeastOneAndAtMostTheLongestLength() throws MalformedStreamException {
        CompressionSettings settings = PREFIX.withLengthBits(2);
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
    void aMatchShorterThanTheLongestLengthIsTheOneTheNearestRuleFinds() {
        CompressionSettings nearestRule = PREFIX.withTripletCoding(TripletCoding.VALACH);
        byte[] text = GeneratedInput.text(6000).bytes();
        SegmentBuffer segment = SegmentBuffer.of(text);
        EncoderDictionary prefix = new ConfiguredACBProvider(PREFIX).encoderDictionary(segment);
        EncoderDictionary nearest = new ConfiguredACBProvider(nearestRule).encoderDictionary(segment);
        int compared = 0;

        for (int idx = 0; idx < text.length; idx++) {
            if (nearest.search(idx) instanceof SearchResult.Hit expected && expected.length() < PREFIX.maxLength()) {
                SearchResult.Hit hit = (SearchResult.Hit) prefix.search(idx);
                assertThat(hit.distance()).as("distance at %d", idx).isEqualTo(expected.distance());
                assertThat(hit.length()).as("length at %d", idx).isEqualTo(expected.length());
                compared++;
            }
            prefix.update(idx, 1);
            nearest.update(idx, 1);
        }

        assertThat(compared).isPositive();
    }

    @Test
    void aDistanceOutsideTheWindowIsMalformedNotAnAnswer() {
        SegmentBuffer segment = SegmentBuffer.of("abbXabcYabcq".getBytes(StandardCharsets.US_ASCII));
        DecoderDictionary decoder = decoderOver(segment, 0);

        assertThatThrownBy(() -> decoder.impliedLength(8, 5))
                .isInstanceOf(MalformedStreamException.class);
    }
}
