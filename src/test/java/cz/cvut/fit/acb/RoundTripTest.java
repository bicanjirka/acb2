package cz.cvut.fit.acb;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.fixtures.DictionarySnapshots;
import cz.cvut.fit.acb.fixtures.DictionaryWrappingProvider;
import cz.cvut.fit.acb.fixtures.PipelineFixtures;
import cz.cvut.fit.acb.fixtures.SettingsCombination;
import cz.cvut.fit.acb.fixtures.TripletLog;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RoundTripTest {

	static Stream<Arguments> settingsAndCorpus() {
		return SettingsCombination.all()
				.flatMap(settings -> CorpusFile.all().map(file -> Arguments.of(settings, file)));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("settingsAndCorpus")
	void aCorpusFileDecompressesToItselfAtEverySegmentSize(SettingsCombination settings, CorpusFile file) {
		settings.knownRoundTripDefect().ifPresent(reason -> assumeTrue(false, reason));
		
		for (int segmentSize : segmentSizesFor(file.bytes().length)) {
			byte[] decompressed = PipelineFixtures.roundTrip(settings.provider(), file.bytes(), segmentSize);

			assertThat(decompressed).as("segment size %d", segmentSize).isEqualTo(file.bytes());
		}
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("settingsAndCorpus")
	void theDecoderRebuildsTheEncodersDictionaryAfterEveryUpdate(SettingsCombination settings, CorpusFile file) {
		settings.knownDictionaryDefect().ifPresent(reason -> assumeTrue(false, reason));
		DictionarySnapshots snapshots = new DictionarySnapshots();
		TripletLog log = new TripletLog();
		List<byte[]> compressed = PipelineFixtures.compress(
				new DictionaryWrappingProvider(settings.provider(), snapshots::recording),
				file.bytes(), PipelineFixtures.WHOLE_INPUT, log);

		PipelineFixtures.decompress(
				new DictionaryWrappingProvider(settings.provider(), snapshots::verifying), compressed, log);

		assertThat(snapshots.verified()).isEqualTo(snapshots.recorded()).isPositive();
	}

	private static int[] segmentSizesFor(int length) {
		return IntStream.of(1, 13, length / 3, length / 2, length / 2 + 1, length - 2, length - 1, length, length + 1)
				.filter(size -> size > 0)
				.distinct()
				.toArray();
	}
}
