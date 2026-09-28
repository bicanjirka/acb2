package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.fixtures.DegenerateInput;
import cz.cvut.fit.acb.fixtures.RoundTripChecks;
import cz.cvut.fit.acb.fixtures.SettingsCombination;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/** Every settings combination over every input; runs with {@code mvn verify -Pfull}. */
@Tag("slow")
class FullRoundTripTest {

    static Stream<Arguments> settingsAndCorpus() {
        return SettingsCombination.all()
                .flatMap(settings -> CorpusFile.all().map(file -> Arguments.of(settings, file)));
    }

    static Stream<Arguments> settingsAndDegenerateInputs() {
        return SettingsCombination.all()
                .flatMap(settings -> DegenerateInput.all().map(input -> Arguments.of(settings, input)));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settingsAndCorpus")
    void everyCombinationDecompressesEveryCorpusFileToItself(SettingsCombination settings, CorpusFile file) {
        RoundTripChecks.corpusFileRoundTripsAtEverySegmentSize(settings, file);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settingsAndDegenerateInputs")
    void everyCombinationDecompressesEveryDegenerateInputToItself(SettingsCombination settings,
                                                                  DegenerateInput input) {
        RoundTripChecks.degenerateInputRoundTrips(settings, input);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("settingsAndCorpus")
    void everyCombinationsDecoderRebuildsTheEncodersDictionary(SettingsCombination settings, CorpusFile file)
            throws MalformedStreamException {
        RoundTripChecks.decoderRebuildsTheEncodersDictionary(settings, file);
    }
}
