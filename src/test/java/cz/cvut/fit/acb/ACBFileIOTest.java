package cz.cvut.fit.acb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ACBFileIOTest {

	private static final int[] BUFFER_SIZES = {1, 2, 13, 1000, 1_000_000, Integer.MAX_VALUE};

	@TempDir
	Path dir;

	static List<CorpusFile> corpus() {
		return CorpusFile.all().toList();
	}

	@ParameterizedTest
	@MethodSource("corpus")
	void openParseCutsAFileIntoFullBuffersAndOneShorterRemainder(CorpusFile file) throws IOException {
		Path path = this.write(file);

		for (int bufferSize : BUFFER_SIZES) {
			List<Integer> segmentSizes = new ArrayList<>();
			new ACBFileIO(bufferSize).openParse(path, segment -> {
				if (segment != null) {
					segmentSizes.add(segment.array().length);
				}
			});

			assertThat(segmentSizes).as("buffer size %d", bufferSize)
					.isEqualTo(expectedSegmentSizes(file.bytes().length, bufferSize));
		}
	}

	@Test
	void openParseRejectsANegativeBufferSize() throws IOException {
		Path path = this.write(CorpusFile.all().findFirst().orElseThrow());

		assertThatThrownBy(() -> new ACBFileIO(-1).openParse(path, segment -> {
		})).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void openParseEndsTheStreamWithOneNullMarker() throws IOException {
		Path path = this.write(CorpusFile.all().findFirst().orElseThrow());
		List<Boolean> endMarkers = new ArrayList<>();

		new ACBFileIO(7).openParse(path, segment -> endMarkers.add(segment == null));

		assertThat(endMarkers).containsOnlyOnce(true).last().isEqualTo(true);
	}

	@Test
	void aSavedObjectListReadsBackEqual() {
		Path path = this.dir.resolve("object");
		for (int[] sizes : new int[][]{{0}, {1, 1, 1}, {10, 20}, randomSizes(50), {Short.MAX_VALUE}}) {
			List<byte[]> saved = randomArrays(sizes);
			AtomicReference<List<byte[]>> read = new AtomicReference<>();

			ACBFileIO io = new ACBFileIO();
			io.saveObject(saved, path);
			io.openObject(path, read::set);

			assertThat(read.get()).containsExactlyElementsOf(saved);
		}
	}

	@Test
	void aSavedArrayListReadsBackEqual() {
		Path path = this.dir.resolve("array");
		for (int[] sizes : new int[][]{{0}, {1, 1, 1}, {10, 20}, randomSizes(50), {Short.MAX_VALUE}}) {
			List<byte[]> saved = randomArrays(sizes);
			AtomicReference<List<byte[]>> read = new AtomicReference<>();

			ACBFileIO io = new ACBFileIO();
			io.saveArray(saved, path);
			io.openArray(path, read::set);

			assertThat(read.get()).containsExactlyElementsOf(saved);
		}
	}

	@Test
	void aParsedWriterWritesSegmentsInOrderAndClosesOnTheEndMarker() throws IOException {
		Path path = this.dir.resolve("parsed");
		Consumer<ByteBuffer> writer = new ACBFileIO().parsedWriter(path);

		writer.accept(ByteBuffer.wrap(new byte[]{1, 2}));
		writer.accept(ByteBuffer.wrap(new byte[]{3}));
		writer.accept(null);

		assertThat(Files.readAllBytes(path)).containsExactly(1, 2, 3);
	}

	private Path write(CorpusFile file) throws IOException {
		return Files.write(this.dir.resolve(file.name()), file.bytes());
	}

	private static List<Integer> expectedSegmentSizes(int length, int bufferSize) {
		List<Integer> sizes = new ArrayList<>();
		for (long from = 0; from < length; from += bufferSize) {
			sizes.add((int) Math.min(bufferSize, length - from));
		}
		return sizes;
	}

	private static int[] randomSizes(int count) {
		return new Random(count).ints(count, 0, 1024).toArray();
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
