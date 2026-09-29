package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.FieldCost;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CompressionStatsTest {

    private static final byte[] MISSISSIPPI = "mississippi".getBytes(StandardCharsets.US_ASCII);

    private static CompressionSettings settings(TripletCoding coding, EntropyCoding entropy) {
        return CompressionSettings.defaults().withTripletCoding(coding).withEntropyCoding(entropy)
                .withDistanceBits(8).withLengthBits(8);
    }

    @Test
    void everyStepOfTheSimpleCoderCodesAllThreeFields() {
        CompressionStats stats = new Compressor(settings(TripletCoding.SIMPLE, EntropyCoding.BIT_ARRAY))
                .compressWithStats(MISSISSIPPI).stats();

        assertThat(stats.inputBytes()).isEqualTo(11);
        assertThat(stats.segments()).isEqualTo(1);
        assertThat(stats.triplets()).isEqualTo(6);
        assertThat(stats.fields()).containsExactly(
                new FieldCost(TripletFieldKind.DISTANCE, 6, 48),
                new FieldCost(TripletFieldKind.LENGTH, 6, 48),
                new FieldCost(TripletFieldKind.LITERAL, 6, 48));
    }

    @Test
    void theValachCoderLeavesTheDistanceOutOfLiterals() {
        CompressionStats stats = new Compressor(settings(TripletCoding.VALACH, EntropyCoding.BIT_ARRAY))
                .compressWithStats(MISSISSIPPI).stats();

        assertThat(stats.cost(TripletFieldKind.DISTANCE).symbols()).isEqualTo(3);
        assertThat(stats.cost(TripletFieldKind.LENGTH).symbols()).isEqualTo(6);
        assertThat(stats.cost(TripletFieldKind.FLAG)).isEqualTo(FieldCost.none(TripletFieldKind.FLAG));
    }

    @Test
    void theSalomonCoderCountsItsFlagAsAField() {
        CompressionStats stats = new Compressor(settings(TripletCoding.SALOMON, EntropyCoding.BIT_ARRAY))
                .compressWithStats(MISSISSIPPI).stats();

        assertThat(stats.cost(TripletFieldKind.FLAG)).isEqualTo(new FieldCost(TripletFieldKind.FLAG, 8, 8));
    }

    @Test
    void rangeCodedFieldCostsAreTheIdealCostOfTheBlockThatCarriesThem() {
        CompressionSettings settings = settings(TripletCoding.SIMPLE, EntropyCoding.ADAPTIVE_ARITHMETIC);
        byte[] text = "the quick brown fox jumps over the lazy dog. ".repeat(100).getBytes(StandardCharsets.US_ASCII);

        CompressionResult result = new Compressor(settings).compressWithStats(text);

        long blockBits = result.stream().blocks().getFirst().storedLength() * 8L;
        assertThat(result.stats().fieldBits()).isPositive().isBetween(blockBits - 64, blockBits + 64);
    }

    @Test
    void statsOfSeparateRunsAddUp() {
        Compressor compressor = new Compressor(settings(TripletCoding.VALACH, EntropyCoding.BIT_ARRAY)
                .withSegmentSize(4));
        CompressionStats once = compressor.compressWithStats(MISSISSIPPI).stats();

        CompressionStats twice = CompressionStats.none().plus(once).plus(once);

        assertThat(once.segments()).isEqualTo(3);
        assertThat(twice.inputBytes()).isEqualTo(22);
        assertThat(twice.triplets()).isEqualTo(2 * once.triplets());
        assertThat(twice.fieldBits()).isEqualTo(2 * once.fieldBits());
        assertThat(CompressionStats.none().plus(once)).isEqualTo(once);
    }

    @Test
    void theStreamWithStatsIsTheStreamWithout() {
        Compressor compressor = new Compressor(settings(TripletCoding.SIMPLE, EntropyCoding.ADAPTIVE_ARITHMETIC));

        CompressionResult result = compressor.compressWithStats(MISSISSIPPI);

        assertThat(result.stream().header()).isEqualTo(compressor.compress(MISSISSIPPI).header());
        assertThat(result.stream().blocks()).containsExactlyElementsOf(compressor.compress(MISSISSIPPI).blocks());
    }
}
