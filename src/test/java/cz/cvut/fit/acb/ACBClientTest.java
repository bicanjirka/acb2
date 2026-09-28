package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import cz.cvut.fit.acb.format.StreamHeader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ACBClientTest {

    private static final int OK = 0;
    private static final int FAILURE = 1;
    private static final int USAGE = 2;

    @TempDir
    Path dir;

    @Test
    void compressingAFileWritesTheOutputAndLeavesTheInputUntouched() throws IOException {
        Path input = this.corpusFile("loremipsum");
        byte[] original = Files.readAllBytes(input);
        Path compressed = this.dir.resolve("lorem.acb");

        int exitCode = run(input, compressed);

        assertThat(exitCode).isEqualTo(OK);
        assertThat(Files.readAllBytes(input)).isEqualTo(original);
        assertThat(compressed).isNotEmptyFile();
    }

    @Test
    void decompressionReadsTheCodingSettingsFromTheFile() throws IOException {
        Path input = this.corpusFile("binary");
        Path compressed = this.dir.resolve("binary.acb");
        Path restored = this.dir.resolve("binary.out");
        run(input, compressed, "-tc", "valach", "-bs", "-d", "9", "-l", "3");

        int exitCode = run(compressed, restored, "-de");

        assertThat(exitCode).isEqualTo(OK);
        assertThat(restored).hasSameBinaryContentAs(input);
    }

    @Test
    void aDirectoryRoundTripsFileByFileIntoOutputDirectories() throws IOException {
        Path inputs = Files.createDirectory(this.dir.resolve("in"));
        CorpusFile.all().forEach(file -> write(inputs.resolve(file.name()), file.bytes()));
        Files.createDirectory(inputs.resolve("nested"));
        Path compressed = this.dir.resolve("compressed");
        Path restored = this.dir.resolve("restored");
        run(inputs, compressed);

        int exitCode = run(compressed, restored, "-de");

        assertThat(exitCode).isEqualTo(OK);
        CorpusFile.all().forEach(file -> assertThat(restored.resolve(file.name())).hasBinaryContent(file.bytes()));
        assertThat(restored.resolve("nested")).doesNotExist();
    }

    @Test
    void aFileCompressesIntoAnExistingOutputDirectoryUnderItsOwnName() throws IOException {
        Path input = this.corpusFile("swissmiss");
        Path outputs = Files.createDirectory(this.dir.resolve("out"));

        int exitCode = run(input, outputs);

        assertThat(exitCode).isEqualTo(OK);
        assertThat(outputs.resolve("swissmiss")).isNotEmptyFile();
    }

    @Test
    void anOutputThatIsTheInputIsRejectedAndTheInputKept() throws IOException {
        Path input = this.corpusFile("mississippi");
        byte[] original = Files.readAllBytes(input);

        int exitCode = run(input, input);

        assertThat(exitCode).isEqualTo(FAILURE);
        assertThat(Files.readAllBytes(input)).isEqualTo(original);
    }

    @Test
    void aMissingInputFails() {
        int exitCode = run(this.dir.resolve("missing"), this.dir.resolve("out"));

        assertThat(exitCode).isEqualTo(FAILURE);
    }

    @Test
    void lcpTripletCodingIsRejectedUntilItRoundTrips() throws IOException {
        Path input = this.corpusFile("aaaa");
        Path output = this.dir.resolve("aaaa.acb");

        int exitCode = run(input, output, "-tc", "lcp");

        assertThat(exitCode).isEqualTo(USAGE);
        assertThat(output).doesNotExist();
    }

    @Test
    void customLengthFrequenciesTravelWithTheFile() throws IOException {
        Path input = this.corpusFile("loremipsum");
        Path compressed = this.dir.resolve("lorem.acb");
        Path restored = this.dir.resolve("lorem.out");
        run(input, compressed, "-af", "1,2,3,4");

        int exitCode = run(compressed, restored, "-de");

        assertThat(exitCode).isEqualTo(OK);
        assertThat(restored).hasSameBinaryContentAs(input);
    }

    @Test
    void decompressingAFileThatIsNotAnAcbStreamFailsAndWritesNothing() throws IOException {
        Path input = this.corpusFile("loremipsum");
        Path output = this.dir.resolve("lorem.out");

        int exitCode = run(input, output, "-de");

        assertThat(exitCode).isEqualTo(FAILURE);
        assertThat(output).doesNotExist();
    }

    @Test
    void bitWidthsBeyondWhatTheFormatStoresAreRejected() throws IOException {
        Path input = this.corpusFile("aaaa");

        assertThat(run(input, this.dir.resolve("a"), "-d", "31")).isEqualTo(USAGE);
        assertThat(run(input, this.dir.resolve("b"), "-l", "0")).isEqualTo(USAGE);
    }

    @Test
    void lengthFrequenciesMustBePositiveIntegers() throws IOException {
        Path input = this.corpusFile("aaaa");

        assertThat(run(input, this.dir.resolve("a"), "-af", "3,0")).isEqualTo(USAGE);
        assertThat(run(input, this.dir.resolve("b"), "-af", "3,x")).isEqualTo(USAGE);
    }

    @Test
    void measuringWritesOneLinePerFileWithBothSizes() throws IOException {
        Path input = this.corpusFile("loremipsum");
        Path compressed = this.dir.resolve("lorem.acb");
        Path report = this.dir.resolve("report.txt");

        int exitCode = run(input, compressed, "-m" + report);

        assertThat(exitCode).isEqualTo(OK);
        List<String> lines = Files.readAllLines(report);
        assertThat(lines).singleElement().asString()
                .startsWith("loremipsum\t")
                .contains("in: " + Files.size(input) + " B", "out: " + Files.size(compressed) + " B");
    }

    @Test
    void helpSucceedsWithoutAnyArguments() {
        assertThat(new ACBClient().run(new String[]{"-h"})).isEqualTo(OK);
    }

    @Test
    void missingArgumentsAndUnknownOptionsAreUsageErrors() throws IOException {
        Path input = this.corpusFile("aaaa");

        assertThat(new ACBClient().run(new String[0])).isEqualTo(USAGE);
        assertThat(new ACBClient().run(new String[]{input.toString()})).isEqualTo(USAGE);
        assertThat(run(input, this.dir.resolve("a"), "-nonsense")).isEqualTo(USAGE);
        assertThat(run(input, this.dir.resolve("b"), "-log", "loud")).isEqualTo(USAGE);
    }

    @Test
    void anExistingOutputIsRefusedAndKeptUnlessForced() throws IOException {
        Path input = this.corpusFile("loremipsum");
        Path output = Files.writeString(this.dir.resolve("lorem.acb"), "precious");

        int refused = run(input, output);
        String afterRefusal = Files.readString(output);
        int forced = run(input, output, "-f");

        assertThat(refused).isEqualTo(FAILURE);
        assertThat(afterRefusal).isEqualTo("precious");
        assertThat(forced).isEqualTo(OK);
        assertThat(output).isNotEmptyFile().content().isNotEqualTo("precious");
    }

    @Test
    void aDecompressionThatFailsHalfWayLeavesNothingBehind() throws IOException {
        CompressionSettings settings = CompressionSettings.defaults();
        CompressedStream corrupt = new CompressedStream(StreamHeader.of(settings),
                List.of(ByteBuffer.allocate(Integer.BYTES).putInt(10).array()));
        Path input = Files.write(this.dir.resolve("corrupt.acb"), ContainerFormat.encode(corrupt));
        Path output = this.dir.resolve("corrupt.out");

        int exitCode = run(input, output, "-de");

        assertThat(exitCode).isEqualTo(FAILURE);
        try (Stream<Path> left = Files.list(this.dir)) {
            assertThat(left).containsExactly(input);
        }
    }

    @Test
    void aLogLevelOptionIsAcceptedAndDoesNotChangeTheResult() throws IOException {
        Path input = this.corpusFile("aaaa");

        try {
            int exitCode = run(input, this.dir.resolve("aaaa.acb"), "-log", "debug");

            assertThat(exitCode).isEqualTo(OK);
        } finally {
            Configurator.setRootLevel(Level.WARN);
        }
    }

    private Path corpusFile(String name) throws IOException {
        CorpusFile file = CorpusFile.all().filter(f -> f.name().equals(name)).findFirst().orElseThrow();
        return Files.write(this.dir.resolve(name), file.bytes());
    }

    private static void write(Path path, byte[] bytes) {
        try {
            Files.write(path, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int run(Path input, Path output, String... options) {
        String[] args = new String[options.length + 2];
        args[0] = input.toString();
        args[1] = output.toString();
        System.arraycopy(options, 0, args, 2, options.length);
        return new ACBClient().run(args);
    }
}
