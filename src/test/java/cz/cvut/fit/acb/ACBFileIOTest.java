package cz.cvut.fit.acb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
			try (ACBFileIO.SegmentReader reader = this.io.readSegments(path, segmentSize)) {
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

		try (ACBFileIO.SegmentWriter writer = this.io.writeSegments(path)) {
			writer.accept(new byte[]{1, 2});
			writer.accept(new byte[]{3});
		}

		assertThat(path).hasBinaryContent(new byte[]{1, 2, 3});
	}

	@Test
	void aSavedCompressedStreamReadsBackEqual() throws IOException {
		Path path = this.dir.resolve("stream.acb");
		int[] sizes = IntStream.concat(IntStream.of(0, Short.MAX_VALUE), new Random(300).ints(300, 0, 64)).toArray();
		CompressedStream saved = new CompressedStream(
				StreamHeader.of(CompressionSettings.defaults()), randomArrays(sizes));

		this.io.saveCompressed(saved, path);
		CompressedStream read = this.io.openCompressed(path);

		assertThat(read.header()).isEqualTo(saved.header());
		assertThat(read.payload()).containsExactlyElementsOf(saved.payload());
	}

	@Test
	void openingAFileThatIsNotAnAcbStreamFailsAsMalformed() throws IOException {
		Path path = this.write(CorpusFile.all().findFirst().orElseThrow());

		assertThatThrownBy(() -> this.io.openCompressed(path)).isInstanceOf(MalformedStreamException.class);
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

	private static List<byte[]> randomArrays(int... sizes) {
		Random random = new Random(sizes.length);
		List<byte[]> arrays = new ArrayList<>();
		for (int size : sizes) {
			byte[] array = new byte[size];
			random.nextBytes(array);
			arrays.add(array);
		}
		return arrays;
	}
}
