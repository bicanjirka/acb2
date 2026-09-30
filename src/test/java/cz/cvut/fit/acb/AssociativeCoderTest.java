package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.fixtures.PipelineFixtures;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** Buyanovsky's coder on its own terms: what its settings mean and what it gains over the layout coders. */
class AssociativeCoderTest {

    private static final CompressionSettings ACB = CompressionSettings.defaultsFor(TripletCoding.ACB);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 8, 12})
    void everyFunnelWidthRoundTripsATextAndABinaryFile(int distanceBits) {
        for (byte[] input : new byte[][]{GeneratedInput.text(6_000).bytes(), GeneratedInput.binary(3_000).bytes()}) {
            byte[] decompressed = PipelineFixtures.roundTrip(ACB.withDistanceBits(distanceBits), input);

            assertThat(decompressed).isEqualTo(input);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 12})
    void everyLongestMatchRoundTripsRunsThatAreLongerThanIt(int lengthBits) {
        byte[] runs = GeneratedInput.runs(5_000).bytes();

        byte[] decompressed = PipelineFixtures.roundTrip(ACB.withLengthBits(lengthBits), runs);

        assertThat(decompressed).isEqualTo(runs);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 10, 255})
    void everyContextDepthRoundTripsAText(int depth) {
        byte[] text = GeneratedInput.text(6_000).bytes();

        byte[] decompressed = PipelineFixtures.roundTrip(ACB.withContextDepth(depth), text);

        assertThat(decompressed).isEqualTo(text);
    }

    @Test
    void theCorpusRoundTripsAtTheCodersOwnDefaults() {
        CorpusFile.all().forEach(file ->
                assertThat(PipelineFixtures.roundTrip(ACB, file.bytes())).isEqualTo(file.bytes()));
    }

    @Test
    void aRepeatedPhraseBecomesAFractionOfItsSize() {
        byte[] input = "the quick brown fox jumps over the lazy dog. ".repeat(200).getBytes();

        int size = ContainerFormat.encode(new Compressor(ACB).compress(input)).length;

        assertThat(size).isLessThan(input.length / 20);
    }

    @Test
    void aTextBecomesSmallerThanTheLayoutCodersMakeIt() {
        byte[] text = GeneratedInput.text(60_000).bytes();
        CompressionSettings layout = CompressionSettings.defaults().withTripletCoding(TripletCoding.VALACH)
                .withEntropyCoding(EntropyCoding.CONTEXT_ARITHMETIC);

        int associative = ContainerFormat.encode(new Compressor(ACB).compress(text)).length;
        int valach = ContainerFormat.encode(new Compressor(layout).compress(text)).length;

        assertThat(associative).isLessThan(valach);
    }

    @Test
    void theCostsOfThePositionTheLengthAndTheLiteralAreReportedAndAddUpToTheBlock() {
        byte[] text = GeneratedInput.text(20_000).bytes();

        CompressionResult result = new Compressor(ACB).compressWithStats(text);

        CompressionStats stats = result.stats();
        assertThat(stats.cost(TripletFieldKind.DISTANCE).symbols()).isPositive().isLessThanOrEqualTo(stats.triplets());
        assertThat(stats.cost(TripletFieldKind.LENGTH).bits()).isPositive();
        assertThat(stats.cost(TripletFieldKind.LITERAL).bits()).isPositive();
        assertThat(stats.cost(TripletFieldKind.FLAG).symbols()).isZero();
        long codedBits = result.stream().blocks().stream().mapToLong(block -> 8L * block.storedLength()).sum();
        assertThat(codedBits).isBetween(stats.fieldBits() - 8, (long) (stats.fieldBits() * 1.01) + 64);
    }

    @Test
    void aSegmentOfRandomBytesIsStoredAsItIs() {
        byte[] noise = GeneratedInput.random(3_000).bytes();

        CompressedStream stream = new Compressor(ACB).compress(noise);

        assertThat(stream.blocks()).singleElement().isEqualTo(Block.stored(noise));
    }

    @Test
    void theOutputDoesNotDependOnTheSegmentsAroundAOne() throws Exception {
        CompressionSettings settings = ACB.withSegmentSize(2_000);
        byte[] first = GeneratedInput.text(2_000).bytes();
        byte[] second = GeneratedInput.binary(2_000).bytes();
        byte[] both = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, both, first.length, second.length);

        CompressedStream together = new Compressor(settings).compress(both);
        CompressedStream alone = new Compressor(settings).compress(second);

        assertThat(together.blocks().get(1)).isEqualTo(alone.blocks().getFirst());
    }
}
