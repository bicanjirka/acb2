package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.fixtures.DictionarySnapshots;
import cz.cvut.fit.acb.fixtures.InterceptingProvider;
import cz.cvut.fit.acb.fixtures.PipelineFixtures;
import cz.cvut.fit.acb.fixtures.SettingsCombination;
import cz.cvut.fit.acb.fixtures.TripletLog;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RoundTripTest {

    static Stream<Arguments> settingsAndCorpus() {
        return SettingsCombination.all()
                .flatMap(settings -> CorpusFile.all().map(file -> Arguments.of(settings, file)));
    }

    static Stream<SettingsCombination> workingSettings() {
        return SettingsCombination.all().filter(settings -> settings.knownRoundTripDefect().isEmpty());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settingsAndCorpus")
    void aCorpusFileDecompressesToItselfAtEverySegmentSize(SettingsCombination settings, CorpusFile file) {
        settings.knownRoundTripDefect().ifPresent(reason -> assumeTrue(false, reason));

        for (int segmentSize : segmentSizesFor(file.bytes().length)) {
            byte[] decompressed = PipelineFixtures.roundTrip(settings.settings().withSegmentSize(segmentSize), file.bytes());

            assertThat(decompressed).as("segment size %d", segmentSize).isEqualTo(file.bytes());
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settingsAndCorpus")
    void theDecoderRebuildsTheEncodersDictionaryAfterEveryUpdate(SettingsCombination settings, CorpusFile file)
            throws MalformedStreamException {
        settings.knownDictionaryDefect().ifPresent(reason -> assumeTrue(false, reason));
        DictionarySnapshots snapshots = new DictionarySnapshots();
        TripletLog log = new TripletLog();
        CompressedStream compressed = new Compressor(settings.settings(),
                InterceptingProvider.components(snapshots::recording, log)).compress(file.bytes());

        new Compressor(settings.settings(), InterceptingProvider.components(snapshots::verifying, log))
                .decompress(compressed);

        assertThat(snapshots.verified()).isEqualTo(snapshots.recorded()).isPositive();
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

    private static int[] segmentSizesFor(int length) {
        return IntStream.of(1, 13, length / 3, length / 2, length / 2 + 1, length - 2, length - 1, length, length + 1)
                .filter(size -> size > 0)
                .distinct()
                .toArray();
    }
}
