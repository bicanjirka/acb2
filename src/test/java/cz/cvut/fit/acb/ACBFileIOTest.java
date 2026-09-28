package cz.cvut.fit.acb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
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
	void aSavedCompressedStreamReadsBackEqual() throws IOException {
		Path path = this.dir.resolve("stream.acb");
		int[] sizes = IntStream.concat(IntStream.of(0, Short.MAX_VALUE), new Random(300).ints(300, 0, 64)).toArray();
		CompressedStream saved = new CompressedStream(
				StreamHeader.of(new ACBProviderParameters()), randomArrays(sizes));
		ACBFileIO io = new ACBFileIO();
		
		io.saveCompressed(saved, path);
		CompressedStream read = io.openCompressed(path);
		
		assertThat(read.header()).isEqualTo(saved.header());
		assertThat(read.payload()).containsExactlyElementsOf(saved.payload());
	}
	
	@Test
	void openingAFileThatIsNotAnAcbStreamFailsAsMalformed() throws IOException {
		Path path = this.write(CorpusFile.all().findFirst().orElseThrow());
		
		assertThatThrownBy(() -> new ACBFileIO().openCompressed(path))
				.isInstanceOf(MalformedStreamException.class);
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
