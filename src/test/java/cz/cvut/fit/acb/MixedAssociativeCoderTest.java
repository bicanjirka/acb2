package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.fixtures.PipelineFixtures;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** The associative coder with its models mixed, on its own terms and against Buyanovsky's own. */
class MixedAssociativeCoderTest {

    private static final CompressionSettings ACBX = CompressionSettings.defaultsFor(TripletCoding.ACBX);

    private static int compressedSize(CompressionSettings settings, byte[] input) {
        return ContainerFormat.encode(new Compressor(settings).compress(input)).length;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 8, 12})
    void everyFunnelWidthRoundTripsATextAndABinaryFile(int distanceBits) {
        for (byte[] input : new byte[][]{GeneratedInput.text(6_000).bytes(), GeneratedInput.binary(3_000).bytes()}) {
            byte[] decompressed = PipelineFixtures.roundTrip(ACBX.withDistanceBits(distanceBits), input);

            assertThat(decompressed).isEqualTo(input);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 12})
    void everyLongestMatchRoundTripsRunsThatAreLongerThanIt(int lengthBits) {
        byte[] runs = GeneratedInput.runs(5_000).bytes();

        byte[] decompressed = PipelineFixtures.roundTrip(ACBX.withLengthBits(lengthBits), runs);

        assertThat(decompressed).isEqualTo(runs);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 10, 255})
    void everyContextDepthRoundTripsAText(int depth) {
        byte[] text = GeneratedInput.text(6_000).bytes();

        byte[] decompressed = PipelineFixtures.roundTrip(ACBX.withContextDepth(depth), text);

        assertThat(decompressed).isEqualTo(text);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 17, 300})
    void tinySegmentsRoundTrip(int segmentSize) {
        byte[] text = GeneratedInput.text(2_000).bytes();

        byte[] decompressed = PipelineFixtures.roundTrip(ACBX.withSegmentSize(segmentSize), text);

        assertThat(decompressed).isEqualTo(text);
    }

    @Test
    void theCorpusRoundTripsAtTheCodersOwnDefaults() {
        CorpusFile.all().forEach(file ->
                assertThat(PipelineFixtures.roundTrip(ACBX, file.bytes())).isEqualTo(file.bytes()));
    }

    @Test
    void aRepeatedPhraseBecomesAFractionOfItsSize() {
        byte[] input = "the quick brown fox jumps over the lazy dog. ".repeat(200).getBytes();

        int size = compressedSize(ACBX, input);

        assertThat(size).isLessThan(input.length / 40);
    }

    @Test
    void aTextAndABinaryFileBecomeSmallerThanBuyanovskysCoderMakesThem() {
        CompressionSettings buyanovsky = CompressionSettings.defaultsFor(TripletCoding.ACB);

        for (byte[] input : new byte[][]{GeneratedInput.text(60_000).bytes(), GeneratedInput.binary(30_000).bytes()}) {
            assertThat(compressedSize(ACBX, input)).isLessThan(compressedSize(buyanovsky, input));
        }
    }

    @Test
    void theCostsOfTheDecisionsAndTheLiteralsAreReportedAndAddUpToTheBlock() {
        byte[] text = GeneratedInput.text(20_000).bytes();

        CompressionResult result = new Compressor(ACBX).compressWithStats(text);

        CompressionStats stats = result.stats();
        assertThat(stats.cost(TripletFieldKind.DISTANCE).bits()).isPositive();
        assertThat(stats.cost(TripletFieldKind.LENGTH).bits()).isPositive();
        assertThat(stats.cost(TripletFieldKind.LITERAL).bits()).isPositive();
        assertThat(stats.cost(TripletFieldKind.FLAG).symbols()).isZero();
        long codedBits = result.stream().blocks().stream().mapToLong(block -> 8L * block.storedLength()).sum();
        assertThat(codedBits).isBetween(stats.fieldBits() - 8, (long) (stats.fieldBits() * 1.01) + 64);
    }

    @Test
    void theOutputDoesNotDependOnTheSegmentsAroundAOne() {
        CompressionSettings settings = ACBX.withSegmentSize(2_000);
        byte[] first = GeneratedInput.text(2_000).bytes();
        byte[] second = GeneratedInput.binary(2_000).bytes();
        byte[] both = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, both, first.length, second.length);

        CompressedStream together = new Compressor(settings).compress(both);
        CompressedStream alone = new Compressor(settings).compress(second);

        assertThat(together.blocks().get(1)).isEqualTo(alone.blocks().getFirst());
    }
}
