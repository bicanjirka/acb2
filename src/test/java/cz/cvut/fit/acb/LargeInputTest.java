package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class LargeInputTest {

    private static final long SEED = 20260928L;
    private static final int SIZE = 1_000_000;
    private static final int SEGMENT_SIZE = 400_000;

    @Test
    void aMultiSegmentTextDecompressesToItselfAndIsSmallerThanItsInput() throws MalformedStreamException {
        byte[] text = wordSoup(SIZE);
        CompressionSettings settings = CompressionSettings.defaults()
                .withTripletCoding(TripletCoding.VALACH).withDistanceBits(3).withLengthBits(7)
                .withSegmentSize(SEGMENT_SIZE);
        Compressor compressor = new Compressor(settings);

        CompressedStream stream = compressor.compress(text);

        assertThat(ContainerFormat.encode(stream).length).isLessThan(text.length);
        assertThat(compressor.decompress(stream)).isEqualTo(text);
    }

    /** Words drawn from a small vocabulary with a skewed choice, so matches are plentiful but not total. */
    private static byte[] wordSoup(int size) {
        Random random = new Random(SEED);
        String[] vocabulary = new String[300];
        for (int i = 0; i < vocabulary.length; i++) {
            StringBuilder word = new StringBuilder();
            for (int c = 2 + random.nextInt(8); c > 0; c--) {
                word.append((char) ('a' + random.nextInt(26)));
            }
            vocabulary[i] = word.toString();
        }
        ByteArrayOutputStream text = new ByteArrayOutputStream(size + 16);
        while (text.size() < size) {
            double skew = random.nextDouble();
            text.writeBytes(vocabulary[(int) (vocabulary.length * skew * skew)].getBytes(StandardCharsets.US_ASCII));
            text.write(' ');
        }
        return text.toByteArray();
    }
}
