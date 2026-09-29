package cz.cvut.fit.acb.fixtures;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.Compressor;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** The round-trip assertions shared by the quick and the full test classes, which differ only in the grid. */
public final class RoundTripChecks {
    private static final int SMALL_SEGMENT = 500;

    private RoundTripChecks() {
    }

    /** A corpus file decompresses to itself at every segment size that splits it interestingly. */
    public static void corpusFileRoundTripsAtEverySegmentSize(SettingsCombination settings, CorpusFile file) {
        for (int segmentSize : segmentSizesFor(file.bytes().length)) {
            byte[] decompressed = PipelineFixtures.roundTrip(settings.settings().withSegmentSize(segmentSize),
                    file.bytes());

            assertThat(decompressed).as("segment size %d", segmentSize).isEqualTo(file.bytes());
        }
    }

    public static void degenerateInputRoundTrips(SettingsCombination settings, DegenerateInput input) {
        for (int segmentSize : new int[]{CompressionSettings.defaults().segmentSize(), SMALL_SEGMENT}) {
            byte[] decompressed = PipelineFixtures.roundTrip(settings.settings().withSegmentSize(segmentSize),
                    input.bytes());

            assertThat(decompressed).as("segment size %d", segmentSize).isEqualTo(input.bytes());
        }
    }

    public static void decoderRebuildsTheEncodersDictionary(SettingsCombination settings, CorpusFile file)
            throws MalformedStreamException {
        DictionarySnapshots snapshots = new DictionarySnapshots();
        TripletLog log = new TripletLog();
        CompressedStream compressed = new Compressor(settings.settings(),
                InterceptingProvider.components(snapshots::recording, log)).compress(file.bytes());

        new Compressor(settings.settings(), InterceptingProvider.components(snapshots::verifying, log))
                .decompress(compressed);

        assertThat(snapshots.verified()).isEqualTo(snapshots.recorded()).isPositive();
    }

    private static int[] segmentSizesFor(int length) {
        return IntStream.of(1, 13, length / 3, length / 2, length / 2 + 1, length - 2, length - 1, length, length + 1)
                .filter(size -> size > 0)
                .distinct()
                .toArray();
    }
}
