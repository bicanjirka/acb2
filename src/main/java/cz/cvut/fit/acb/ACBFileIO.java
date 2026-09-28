package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * The file side of the CLI: reads inputs as segments, writes decoded segments, and stores
 * compressed streams in the {@link ContainerFormat}.
 *
 * @author jiri.bican
 */
public final class ACBFileIO {

    private static final Logger logger = LogManager.getLogger();

    /** Reads {@code path} lazily, {@code segmentSize} bytes at a time; close it when done. */
    public SegmentReader readSegments(Path path, int segmentSize) throws IOException {
        if (segmentSize < 1) {
            throw new IllegalArgumentException("segment size must be greater than zero: " + segmentSize);
        }
        return new SegmentReader(path, Files.newByteChannel(path), segmentSize);
    }

    /** Writes decoded segments to {@code path} in order; close it when done. */
    public SegmentWriter writeSegments(Path path) throws IOException {
        return new SegmentWriter(path, Files.newOutputStream(path));
    }

    public void saveCompressed(CompressedStream stream, Path output) throws IOException {
        byte[] bytes = ContainerFormat.encode(stream);
        Files.write(output, bytes);
        int payloadSize = stream.payload().stream().mapToInt(array -> array.length).sum();
        logger.debug("Compressed into '{}' [size = {}, overhead = {}]", output, bytes.length, bytes.length - payloadSize);
    }

    /** @throws cz.cvut.fit.acb.format.MalformedStreamException if the file is not an intact ACB stream */
    public CompressedStream openCompressed(Path path) throws IOException {
        return ContainerFormat.decode(Files.readAllBytes(path));
    }

    public static final class SegmentReader implements Iterator<byte[]>, Closeable {

        private final Path path;
        private final SeekableByteChannel channel;
        private final int segmentSize;

        private SegmentReader(Path path, SeekableByteChannel channel, int segmentSize) {
            this.path = path;
            this.channel = channel;
            this.segmentSize = segmentSize;
        }

        @Override
        public boolean hasNext() {
            try {
                return this.channel.position() < this.channel.size();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + this.path, e);
            }
        }

        @Override
        public byte[] next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                int size = (int) Math.min(this.segmentSize, this.channel.size() - this.channel.position());
                ByteBuffer segment = ByteBuffer.allocate(size);
                while (segment.hasRemaining()) {
                    if (this.channel.read(segment) < 0) {
                        throw new EOFException("File shrank while reading: " + this.path);
                    }
                }
                return segment.array();
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read " + this.path, e);
            }
        }

        @Override
        public void close() throws IOException {
            this.channel.close();
        }
    }

    public static final class SegmentWriter implements Consumer<byte[]>, Closeable {

        private final Path path;
        private final OutputStream stream;

        private SegmentWriter(Path path, OutputStream stream) {
            this.path = path;
            this.stream = stream;
        }

        @Override
        public void accept(byte[] segment) {
            try {
                this.stream.write(segment);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot write " + this.path, e);
            }
        }

        @Override
        public void close() throws IOException {
            this.stream.close();
        }
    }
}
