package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.Compressor;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.fixtures.CorpusFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One compressed file per coder, checked in under {@code src/test/resources/golden/v<VERSION>},
 * that must keep decoding. A change to the container layout, the coder codes, or how a coder lays
 * out triplets that does not bump {@link ContainerFormat#VERSION} breaks these files. A deliberate
 * bump leaves no files for the new version; write them with {@link #main} and commit them.
 */
class GoldenStreamTest {

    private static final int SEGMENT_SIZE = 250;
    /** Spelled out, so that a change of the defaults cannot change what the files must decode to. */
    private static final CompressionSettings BASE = CompressionSettings.defaults().withSegmentSize(SEGMENT_SIZE)
            .withDistanceBits(6).withLengthBits(4).withLengthFrequencies(45, 13, 10, 7, 5, 4);

    record Golden(String name, CompressionSettings settings) {

        @Override
        public String toString() {
            return this.name;
        }
    }

    static Stream<Golden> goldens() {
        return Stream.of(
                new Golden("simple", BASE.withTripletCoding(TripletCoding.SIMPLE)),
                new Golden("salomon", BASE.withTripletCoding(TripletCoding.SALOMON)
                        .withDistanceBits(5).withLengthBits(6)),
                new Golden("salomon2", BASE.withTripletCoding(TripletCoding.SALOMON2)
                        .withDistanceBits(7).withLengthBits(3)),
                new Golden("valach", BASE.withTripletCoding(TripletCoding.VALACH).withLengthBits(7)),
                new Golden("valach-bit-array", BASE.withTripletCoding(TripletCoding.VALACH)
                        .withEntropyCoding(EntropyCoding.BIT_ARRAY).withDistanceBits(4)));
    }

    @ParameterizedTest
    @MethodSource("goldens")
    void aStreamWrittenByThisFormatVersionStillDecodesToItsInput(Golden golden) throws MalformedStreamException {
        byte[] file = resource(golden);

        CompressedStream stream = ContainerFormat.decode(file);

        assertThat(stream.header()).isEqualTo(StreamHeader.of(golden.settings()));
        assertThat(new Compressor(golden.settings()).decompress(stream)).isEqualTo(plaintext());
    }

    @Test
    void theGoldenInputSpansSeveralSegments() {
        assertThat(plaintext().length).isGreaterThan(2 * SEGMENT_SIZE);
    }

    /** Writes the golden files for the current format version: {@code GoldenStreamTest [directory]}. */
    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "src/test/resources/golden");
        Path directory = root.resolve("v" + ContainerFormat.VERSION);
        Files.createDirectories(directory);
        for (Golden golden : goldens().toList()) {
            byte[] file = ContainerFormat.encode(new Compressor(golden.settings()).compress(plaintext()));
            Files.write(directory.resolve(golden.name() + ".acb"), file);
        }
    }

    private static byte[] plaintext() {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        List<CorpusFile> files = CorpusFile.all().toList();
        files.forEach(file -> all.writeBytes(file.bytes()));
        return all.toByteArray();
    }

    private static byte[] resource(Golden golden) {
        String path = "/golden/v" + ContainerFormat.VERSION + "/" + golden.name() + ".acb";
        try (InputStream in = GoldenStreamTest.class.getResourceAsStream(path)) {
            assertThat(in).as("%s is missing; run GoldenStreamTest.main after a deliberate VERSION bump", path)
                    .isNotNull();
            return in.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Cannot read " + path, e);
        }
    }
}
