package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LargeInputTest {

    private static final int SIZE = 1_000_000;
    private static final int SEGMENT_SIZE = 400_000;

    @Test
    void aMultiSegmentTextDecompressesToItselfAndIsSmallerThanItsInput() throws MalformedStreamException {
        byte[] text = GeneratedInput.text(SIZE).bytes();
        CompressionSettings settings = CompressionSettings.defaults()
                .withTripletCoding(TripletCoding.VALACH).withDistanceBits(3).withLengthBits(7)
                .withSegmentSize(SEGMENT_SIZE);
        Compressor compressor = new Compressor(settings);

        CompressedStream stream = compressor.compress(text);

        assertThat(ContainerFormat.encode(stream).length).isLessThan(text.length);
        assertThat(compressor.decompress(stream)).isEqualTo(text);
    }
}
