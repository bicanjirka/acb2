package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompressorTest {

    private final Compressor compressor = new Compressor(CompressionSettings.defaults());

    private static byte[] repeated(int length) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) 'a');
        return bytes;
    }

    private static byte[] random(int length) {
        byte[] bytes = new byte[length];
        new Random(length).nextBytes(bytes);
        return bytes;
    }

    @Test
    void aStreamRecordsTheSettingsItWasCompressedWith() {
        CompressionSettings settings = CompressionSettings.defaults().withTripletCoding(TripletCoding.SALOMON)
                .withSegmentSize(4096);

        CompressedStream stream = new Compressor(settings).compress(new byte[]{1, 2, 3});

        assertThat(stream.header()).isEqualTo(StreamHeader.of(settings));
    }

    @Test
    void modelledLiteralsMakeATextSmallerThanTheOrderZeroCoderDoes() {
        byte[] text = GeneratedInput.text(60_000).bytes();
        CompressionSettings orderZero = CompressionSettings.defaults();
        CompressionSettings modelled = orderZero.withEntropyCoding(EntropyCoding.CONTEXT_ARITHMETIC);

        int plain = ContainerFormat.encode(new Compressor(orderZero).compress(text)).length;
        int better = ContainerFormat.encode(new Compressor(modelled).compress(text)).length;

        assertThat(better).isLessThan(plain);
    }


    @Test
    void anEmptyInputHasNoBlocks() throws MalformedStreamException {
        CompressedStream stream = this.compressor.compress(new byte[0]);

        assertThat(stream.blocks()).isEmpty();
        assertThat(this.compressor.decompress(stream)).isEmpty();
    }

    @Test
    void aSegmentThatCodingShrinksIsStoredCoded() {
        CompressedStream stream = this.compressor.compress(repeated(1000));

        assertThat(stream.blocks()).singleElement().isInstanceOfSatisfying(Block.Coded.class,
                block -> assertThat(block.storedLength()).isLessThan(1000));
    }

    @Test
    void aSegmentThatCodingWouldGrowIsStoredAsItIs() throws MalformedStreamException {
        byte[] input = random(2000);

        CompressedStream stream = this.compressor.compress(input);

        assertThat(stream.blocks()).singleElement().isEqualTo(Block.stored(input));
        assertThat(this.compressor.decompress(stream)).isEqualTo(input);
    }

    @Test
    void anInputThatDoesNotCompressGrowsOnlyByTheContainerOverhead() {
        byte[] input = random(100_000);
        CompressionSettings settings = CompressionSettings.defaults().withSegmentSize(30_000);

        int size = ContainerFormat.encode(new Compressor(settings).compress(input)).length;

        assertThat(size).isLessThan(input.length + 100);
    }

    @Test
    void everyBlockIsCodedOnItsOwnSoALoneBlockDecodesToItsSegment() throws MalformedStreamException {
        CompressionSettings settings = CompressionSettings.defaults().withSegmentSize(1000);
        byte[] second = "the second segment, the second segment, the second segment".repeat(15).getBytes();
        byte[] input = concat(repeated(1000), second);
        CompressedStream both = new Compressor(settings).compress(input);

        CompressedStream onlyTheSecond = new CompressedStream(both.header(), List.of(both.blocks().get(1)));

        assertThat(both.blocks()).hasSize(2);
        assertThat(this.compressor.decompress(onlyTheSecond)).isEqualTo(second);
    }

    @Test
    void segmentsOfDifferentSizesMayFollowEachOther() throws MalformedStreamException {
        CompressionSettings settings = CompressionSettings.defaults().withSegmentSize(2000);
        List<byte[]> segments = List.of(repeated(500), random(30), repeated(2000), repeated(1));

        CompressedStream stream = new Compressor(settings).compress(segments.iterator());

        byte[] expected = segments.stream().reduce(new byte[0], CompressorTest::concat);
        assertThat(new Compressor(settings).decompress(stream)).isEqualTo(expected);
    }

    @Test
    void aSegmentLongerThanTheSegmentSizeIsRejected() {
        Compressor small = new Compressor(CompressionSettings.defaults().withSegmentSize(10));

        assertThatThrownBy(() -> small.compress(List.of(new byte[11]).iterator()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEmptySegmentIsRejected() {
        assertThatThrownBy(() -> this.compressor.compress(List.of(new byte[0]).iterator()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] joined = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, joined, first.length, second.length);
        return joined;
    }
}
