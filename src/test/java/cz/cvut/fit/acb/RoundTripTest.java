package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.fixtures.DegenerateInput;
import cz.cvut.fit.acb.fixtures.PipelineFixtures;
import cz.cvut.fit.acb.fixtures.RoundTripChecks;
import cz.cvut.fit.acb.fixtures.SettingsCombination;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** The quick round trips, over the representative settings; {@code FullRoundTripTest} covers every combination. */
class RoundTripTest {

    static Stream<Arguments> settingsAndCorpus() {
        return SettingsCombination.representative()
                .flatMap(settings -> CorpusFile.all().map(file -> Arguments.of(settings, file)));
    }

    static Stream<Arguments> settingsAndDegenerateInputs() {
        return SettingsCombination.representative()
                .flatMap(settings -> DegenerateInput.all().map(input -> Arguments.of(settings, input)));
    }

    static Stream<SettingsCombination> workingSettings() {
        return SettingsCombination.representative().filter(settings -> settings.knownRoundTripDefect().isEmpty());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settingsAndCorpus")
    void aCorpusFileDecompressesToItselfAtEverySegmentSize(SettingsCombination settings, CorpusFile file) {
        RoundTripChecks.corpusFileRoundTripsAtEverySegmentSize(settings, file);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settingsAndDegenerateInputs")
    void aDegenerateInputDecompressesToItself(SettingsCombination settings, DegenerateInput input) {
        RoundTripChecks.degenerateInputRoundTrips(settings, input);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settingsAndCorpus")
    void theDecoderRebuildsTheEncodersDictionaryAfterEveryUpdate(SettingsCombination settings, CorpusFile file)
            throws MalformedStreamException {
        RoundTripChecks.decoderRebuildsTheEncodersDictionary(settings, file);
    }

    @ParameterizedTest
    @MethodSource("workingSettings")
    void anEmptyInputDecompressesToAnEmptyOutput(SettingsCombination settings) {
        byte[] decompressed = PipelineFixtures.roundTrip(settings.settings(), new byte[0]);

        assertThat(decompressed).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("workingSettings")
    void oneCompressorHandlesStreamsIndependently(SettingsCombination settings) throws MalformedStreamException {
        Compressor compressor = new Compressor(settings.settings().withSegmentSize(13));
        List<byte[]> inputs = CorpusFile.all().map(CorpusFile::bytes).toList();

        List<CompressedStream> compressed = inputs.stream().map(compressor::compress).toList();

        for (int i = 0; i < inputs.size(); i++) {
            assertThat(compressor.decompress(compressed.get(i))).isEqualTo(inputs.get(i));
        }
    }
}
