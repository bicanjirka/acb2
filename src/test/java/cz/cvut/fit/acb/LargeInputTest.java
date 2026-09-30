package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.GeneratedInput;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.format.MalformedStreamException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs with {@code mvn verify -Pfull}: about a megabyte through three segments, and a file of 2.5 MB through the CLI. */
@Tag("slow")
class LargeInputTest {

    private static final int SIZE = 1_000_000;
    private static final int SEGMENT_SIZE = 400_000;

    @Test
    void aFileOfSeveralSegmentsCompressedOnSeveralThreadsIsTheFileCompressedOnOne(@TempDir Path dir)
            throws IOException {
        Path input = Files.write(dir.resolve("text"), GeneratedInput.text(2_500_000).bytes());
        Path single = dir.resolve("single.acb");
        Path several = dir.resolve("several.acb");
        Path restored = dir.resolve("restored");

        int exitCodes = new ACBClient().run(new String[]{input.toString(), single.toString(), "-j", "1"})
                + new ACBClient().run(new String[]{input.toString(), several.toString(), "-j", "3"})
                + new ACBClient().run(new String[]{several.toString(), restored.toString(), "-de", "-j", "3"});

        assertThat(exitCodes).isZero();
        assertThat(several).hasSameBinaryContentAs(single);
        assertThat(restored).hasSameBinaryContentAs(input);
    }

    @Test
    void aFileOfSeveralSegmentsIsTheSameFromTheAssociativeCoderOnOneThreadAndOnSeveral(@TempDir Path dir)
            throws IOException {
        Path input = Files.write(dir.resolve("text"), GeneratedInput.text(2_500_000).bytes());
        Path single = dir.resolve("single.acb");
        Path several = dir.resolve("several.acb");
        Path restored = dir.resolve("restored");

        int exitCodes = new ACBClient().run(new String[]{input.toString(), single.toString(), "-tc", "acb", "-j", "1"})
                + new ACBClient().run(new String[]{input.toString(), several.toString(), "-tc", "acb", "-j", "3"})
                + new ACBClient().run(new String[]{several.toString(), restored.toString(), "-de", "-j", "3"});

        assertThat(exitCodes).isZero();
        assertThat(several).hasSameBinaryContentAs(single);
        assertThat(restored).hasSameBinaryContentAs(input);
    }

    @Test
    void aMultiSegmentTextFromTheAssociativeCoderAtItsWidestFunnelDecompressesToItself()
            throws MalformedStreamException {
        byte[] text = GeneratedInput.text(SIZE).bytes();
        CompressionSettings settings = CompressionSettings.defaultsFor(TripletCoding.ACB).withDistanceBits(10)
                .withSegmentSize(SEGMENT_SIZE);
        Compressor compressor = new Compressor(settings);

        CompressedStream stream = compressor.compress(text);

        assertThat(ContainerFormat.encode(stream).length).isLessThan(text.length / 2);
        assertThat(compressor.decompress(stream)).isEqualTo(text);
    }

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
