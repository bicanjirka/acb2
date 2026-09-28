package cz.cvut.fit.acb;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import cz.cvut.fit.acb.fixtures.CorpusFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ACBClientTest {
	
	private static final int OK = 0;
	private static final int FATAL = 1;
	
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
	void aCompressedFileDecompressesToTheOriginal() throws IOException {
		Path input = this.corpusFile("binary");
		Path compressed = this.dir.resolve("binary.acb");
		Path restored = this.dir.resolve("binary.out");
		run(input, compressed, "-tc", "valach", "-bs");
		
		int exitCode = run(compressed, restored, "-de", "-tc", "valach", "-bs");
		
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
		
		assertThat(exitCode).isEqualTo(FATAL);
		assertThat(Files.readAllBytes(input)).isEqualTo(original);
	}
	
	@Test
	void aMissingInputFails() {
		int exitCode = run(this.dir.resolve("missing"), this.dir.resolve("out"));
		
		assertThat(exitCode).isEqualTo(FATAL);
	}
	
	@Test
	void lcpTripletCodingIsRejectedUntilItRoundTrips() throws IOException {
		Path input = this.corpusFile("aaaa");
		Path output = this.dir.resolve("aaaa.acb");
		
		int exitCode = run(input, output, "-tc", "lcp");
		
		assertThat(exitCode).isEqualTo(FATAL);
		assertThat(output).doesNotExist();
	}
	
	@Test
	void customLengthFrequenciesRoundTripWhenBothSidesUseThem() throws IOException {
		Path input = this.corpusFile("loremipsum");
		Path compressed = this.dir.resolve("lorem.acb");
		Path restored = this.dir.resolve("lorem.out");
		run(input, compressed, "-af", "1,2,3,4");
		
		int exitCode = run(compressed, restored, "-de", "-af", "1,2,3,4");
		
		assertThat(exitCode).isEqualTo(OK);
		assertThat(restored).hasSameBinaryContentAs(input);
	}
	
	@Test
	void lengthFrequenciesMustBePositiveIntegers() throws IOException {
		Path input = this.corpusFile("aaaa");
		
		assertThat(run(input, this.dir.resolve("a"), "-af", "3,0")).isEqualTo(FATAL);
		assertThat(run(input, this.dir.resolve("b"), "-af", "3,x")).isEqualTo(FATAL);
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
