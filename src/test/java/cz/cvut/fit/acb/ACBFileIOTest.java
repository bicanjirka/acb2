package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerReader;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ACBFileIOTest {

    private static final int[] SEGMENT_SIZES = {1, 2, 13, 1000, 1_000_000, Integer.MAX_VALUE};

    private final ACBFileIO io = new ACBFileIO();

    @TempDir
    Path dir;

    static List<CorpusFile> corpus() {
        return CorpusFile.all().toList();
    }

    @ParameterizedTest
    @MethodSource("corpus")
    void aFileReadsAsFullSegmentsAndOneShorterRemainder(CorpusFile file) throws IOException {
        Path path = this.write(file);

        for (int segmentSize : SEGMENT_SIZES) {
            List<byte[]> segments = new ArrayList<>();
            try (SegmentReader reader = this.io.readSegments(path, segmentSize)) {
                reader.forEachRemaining(segments::add);
            }

            assertThat(segments.stream().map(segment -> segment.length)).as("segment size %d", segmentSize)
                    .containsExactlyElementsOf(expectedSegmentSizes(file.bytes().length, segmentSize));
            assertThat(concatenated(segments)).isEqualTo(file.bytes());
        }
    }

    @Test
    void aSegmentSizeBelowOneIsRejected() throws IOException {
        Path path = this.write(CorpusFile.all().findFirst().orElseThrow());

        assertThatThrownBy(() -> this.io.readSegments(path, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void writtenSegmentsLandInOrder() throws IOException {
        Path path = this.dir.resolve("segments");

        try (SegmentWriter writer = this.io.writeSegments(path)) {
            writer.accept(new byte[]{1, 2});
            writer.accept(new byte[]{3});
            writer.commit();
        }

        assertThat(path).hasBinaryContent(new byte[]{1, 2, 3});
    }

    @Test
    void segmentsThatAreNotCommittedLeaveNothingBehind() throws IOException {
        Path path = this.dir.resolve("segments");

        try (SegmentWriter writer = this.io.writeSegments(path)) {
            writer.accept(new byte[]{1, 2});
        }

        assertThat(this.dir).isEmptyDirectory();
    }

    @Test
    void aCommitReplacesTheFileThatWasThereAndLeavesNoTemporaryFile() throws IOException {
        Path path = Files.write(this.dir.resolve("segments"), new byte[]{9, 9, 9, 9});

        try (SegmentWriter writer = this.io.writeSegments(path)) {
            writer.accept(new byte[]{1});
            assertThat(path).hasBinaryContent(new byte[]{9, 9, 9, 9});
            writer.commit();
        }

        assertThat(path).hasBinaryContent(new byte[]{1});
        try (Stream<Path> files = Files.list(this.dir)) {
            assertThat(files).containsExactly(path);
        }
    }

    @Test
    void savingACompressedStreamLeavesOnlyTheOutput() throws IOException {
        Path path = this.dir.resolve("stream.acb");

        this.io.saveCompressed(new CompressedStream(StreamHeader.of(CompressionSettings.defaults()), List.of()), path);

        try (Stream<Path> files = Files.list(this.dir)) {
            assertThat(files).containsExactly(path);
        }
    }

    @Test
    void aSavedCompressedStreamReadsBackEqual() throws IOException {
        Path path = this.dir.resolve("stream.acb");
        int[] sizes = IntStream.concat(IntStream.of(0, Short.MAX_VALUE), new Random(300).ints(300, 0, 64)).toArray();
        CompressedStream saved = new CompressedStream(
                StreamHeader.of(CompressionSettings.defaults()), randomBlocks(sizes));

        this.io.saveCompressed(saved, path);
        CompressedStream read = this.io.openCompressed(path);

        assertThat(read.header()).isEqualTo(saved.header());
        assertThat(read.blocks()).containsExactlyElementsOf(saved.blocks());
    }

    @Test
    void openingAFileThatIsNotAnAcbStreamFailsAsMalformed() throws IOException {
        Path path = this.write(CorpusFile.all().findFirst().orElseThrow());

        assertThatThrownBy(() -> this.io.openCompressed(path)).isInstanceOf(MalformedStreamException.class);
    }

    @Test
    void aReaderCountsTheSegmentsItsFileMakes() throws IOException {
        Path path = this.write(CorpusFile.all().findFirst().orElseThrow());
        int length = (int) Files.size(path);

        for (int segmentSize : SEGMENT_SIZES) {
            try (SegmentReader reader = this.io.readSegments(path, segmentSize)) {
                assertThat(reader.segmentCount()).as("segment size %d", segmentSize)
                        .isEqualTo(expectedSegmentSizes(length, segmentSize).size());
            }
        }
    }

    @Test
    void aFileThatGrowsAfterItIsOpenedIsReadOnlyToItsOldLength() throws IOException {
        Path path = Files.write(this.dir.resolve("growing"), new byte[]{1, 2, 3});
        List<byte[]> segments = new ArrayList<>();

        try (SegmentReader reader = this.io.readSegments(path, 2)) {
            Files.write(path, new byte[]{1, 2, 3, 4, 5}, StandardOpenOption.TRUNCATE_EXISTING);
            reader.forEachRemaining(segments::add);
        }

        assertThat(concatenated(segments)).containsExactly(1, 2, 3);
    }

    @Test
    void aFileThatShrinksAfterItIsOpenedFailsWhileReading() throws IOException {
        Path path = Files.write(this.dir.resolve("shrinking"), new byte[]{1, 2, 3, 4});

        try (SegmentReader reader = this.io.readSegments(path, 4)) {
            Files.write(path, new byte[]{1}, StandardOpenOption.TRUNCATE_EXISTING);

            assertThatThrownBy(reader::next).isInstanceOf(UncheckedIOException.class);
        }
    }

    @Test
    void aCompressedFileWrittenBlockByBlockReadsBackBlockByBlock() throws IOException {
        Path path = this.dir.resolve("stream.acb");
        StreamHeader header = StreamHeader.of(CompressionSettings.defaults());
        List<Block> blocks = randomBlocks(5, 0, 40, Short.MAX_VALUE);

        try (CompressedWriter writer = this.io.createCompressed(path, header, blocks.size())) {
            blocks.forEach(writer);
            writer.commit();
        }

        try (ContainerReader reader = this.io.readCompressed(path)) {
            assertThat(reader.header()).isEqualTo(header);
            assertThat(reader.blockCount()).isEqualTo(blocks.size());
            assertThat(reader.drain()).containsExactlyElementsOf(blocks);
        }
    }

    @Test
    void aCompressedWriterThatIsNotCommittedLeavesNothingBehind() throws IOException {
        Path path = this.dir.resolve("stream.acb");
        StreamHeader header = StreamHeader.of(CompressionSettings.defaults());

        try (CompressedWriter writer = this.io.createCompressed(path, header, 2)) {
            writer.accept(randomBlocks(5).getFirst());
        }

        assertThat(this.dir).isEmptyDirectory();
    }

    @Test
    void aCompressedWriterWithBlocksMissingRefusesToCommitAndLeavesNothingBehind() throws IOException {
        Path path = this.dir.resolve("stream.acb");
        StreamHeader header = StreamHeader.of(CompressionSettings.defaults());

        try (CompressedWriter writer = this.io.createCompressed(path, header, 2)) {
            writer.accept(randomBlocks(5).getFirst());

            assertThatThrownBy(writer::commit).isInstanceOf(IllegalStateException.class);
        }

        assertThat(this.dir).isEmptyDirectory();
    }

    @Test
    void aCompressedFileThatIsDamagedIsRefusedBeforeItsFirstBlockIsRead() throws IOException {
        Path path = this.dir.resolve("stream.acb");
        this.io.saveCompressed(new CompressedStream(StreamHeader.of(CompressionSettings.defaults()),
                randomBlocks(50, 60)), path);
        byte[] bytes = Files.readAllBytes(path);
        bytes[bytes.length - Integer.BYTES - 1] ^= 1;
        Files.write(path, bytes);

        assertThatThrownBy(() -> this.io.readCompressed(path)).isInstanceOf(MalformedStreamException.class);
    }

    private Path write(CorpusFile file) throws IOException {
        return Files.write(this.dir.resolve(file.name()), file.bytes());
    }

    private static List<Integer> expectedSegmentSizes(int length, int segmentSize) {
        List<Integer> sizes = new ArrayList<>();
        for (long from = 0; from < length; from += segmentSize) {
            sizes.add((int) Math.min(segmentSize, length - from));
        }
        return sizes;
    }

    private static byte[] concatenated(List<byte[]> segments) {
        return segments.stream().reduce(new byte[0], (a, b) -> {
            byte[] joined = Arrays.copyOf(a, a.length + b.length);
            System.arraycopy(b, 0, joined, a.length, b.length);
            return joined;
        });
    }

    private static List<Block> randomBlocks(int... sizes) {
        Random random = new Random(sizes.length);
        List<Block> blocks = new ArrayList<>();
        for (int size : sizes) {
            byte[] array = new byte[size];
            random.nextBytes(array);
            blocks.add(Block.coded(size + 1, array));
        }
        return blocks;
    }
}
