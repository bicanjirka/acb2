package cz.cvut.fit.acb;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author jiri.bican
 */
public class ACBFileIO {
	private static final Logger logger = LogManager.getLogger();
	private final int bufferUnit;
	
	public ACBFileIO() {
		this(1_000_000);
	}
	
	public ACBFileIO(int bufferUnit) {
		this.bufferUnit = bufferUnit;
	}
	
	public void openParallel(Path path, Consumer<ByteBuffer> byteBufferConsumer) {
		try {
			SeekableByteChannel sbc = Files.newByteChannel(path);
			logger.info("Opened file '{}' [size = {}]", path, sbc.size());
			ExecutorService service = getExecutorService();
			List<Callable<Object>> tasks = new ArrayList<>();
			
			while (sbc.position() < sbc.size()) {
				int size = (int) Math.min(bufferUnit, sbc.size() - sbc.position());
				ByteBuffer bb = ByteBuffer.allocate(size);
				sbc.read(bb);
				
				tasks.add(Executors.callable(() -> {
					logger.info("Starting {} with {} bytes.", Thread.currentThread().getName(), size);
					byteBufferConsumer.accept(bb);
				}));
			}
			tasks.add(Executors.callable(() -> byteBufferConsumer.accept(null)));
			service.invokeAll(tasks);
//			service.shutdown();
//			try {
//				service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private ExecutorService getExecutorService() {
		return Executors.newWorkStealingPool();
	}
	
	/** Feeds the file to {@code byteBufferConsumer} in segments of the buffer unit, then {@code null}. */
	public void openParse(Path path, Consumer<ByteBuffer> byteBufferConsumer) {
		try (SeekableByteChannel sbc = Files.newByteChannel(path)) {
			logger.debug("Opened file '{}' [size = {}] parsed into {} units, {} bytes each", path, sbc.size(),
					Math.ceil(sbc.size() / (double) this.bufferUnit), this.bufferUnit);
			while (sbc.position() < sbc.size()) {
				int size = (int) Math.min(this.bufferUnit, sbc.size() - sbc.position());
				ByteBuffer bb = ByteBuffer.allocate(size);
				while (bb.hasRemaining()) {
					if (sbc.read(bb) < 0) {
						throw new EOFException("File shrank while reading: " + path);
					}
				}
				byteBufferConsumer.accept(bb);
			}
			byteBufferConsumer.accept(null);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot read " + path, e);
		}
	}
	
	public void saveCompressed(CompressedStream stream, Path output) {
		byte[] bytes = ContainerFormat.encode(stream);
		try {
			Files.write(output, bytes);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot write " + output, e);
		}
		int payloadSize = stream.payload().stream().mapToInt(array -> array.length).sum();
		logger.debug("Compressed into '{}' [size = {}, overhead = {}]", output, bytes.length, bytes.length - payloadSize);
	}
	
	/** @throws cz.cvut.fit.acb.format.MalformedStreamException if the file is not an intact ACB stream */
	public CompressedStream openCompressed(Path path) throws IOException {
		return ContainerFormat.decode(Files.readAllBytes(path));
	}
	
	/**
	 * Writes each decoded segment to {@code output} in order; the {@code null} end-of-stream marker
	 * closes the file. Every call returns an independent writer.
	 */
	public Consumer<ByteBuffer> parsedWriter(Path output) {
		return new ParsedWriter(output);
	}

	private static final class ParsedWriter implements Consumer<ByteBuffer> {
		private final Path output;
		private OutputStream stream;

		private ParsedWriter(Path output) {
			this.output = output;
		}

		@Override
		public void accept(ByteBuffer byteBuffer) {
			try {
				if (this.stream == null) {
					this.stream = Files.newOutputStream(this.output);
				}
				if (byteBuffer != null) {
					this.stream.write(byteBuffer.array());
				} else {
					this.stream.close();
				}
			} catch (IOException e) {
				throw new UncheckedIOException("Cannot write " + this.output, e);
			}
		}
	}
}
