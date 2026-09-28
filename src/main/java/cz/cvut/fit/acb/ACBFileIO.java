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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * The file side of the CLI: reads inputs as segments, writes decoded segments, and stores
 * compressed streams in the {@link ContainerFormat}. An output is written to a temporary sibling
 * and moved into place when complete, so a failure leaves neither a partial file nor a replaced
 * one.
 */
public final class ACBFileIO {

    private static final Logger LOG = LogManager.getLogger();

    /** Reads {@code path} lazily, {@code segmentSize} bytes at a time; close it when done. */
    public SegmentReader readSegments(Path path, int segmentSize) throws IOException {
        if (segmentSize < 1) {
            throw new IllegalArgumentException("segment size must be greater than zero: " + segmentSize);
        }
        return new SegmentReader(path, Files.newByteChannel(path), segmentSize);
    }

    /**
     * Writes decoded segments in order. Call {@link SegmentWriter#commit()} when all are written;
     * closing without it discards them.
     */
    public SegmentWriter writeSegments(Path path) throws IOException {
        Path temp = temporarySibling(path);
        try {
            return new SegmentWriter(path, temp, Files.newOutputStream(temp));
        } catch (IOException | RuntimeException e) {
            discard(temp, e);
            throw e;
        }
    }

    public void saveCompressed(CompressedStream stream, Path output) throws IOException {
        byte[] bytes = ContainerFormat.encode(stream);
        Path temp = temporarySibling(output);
        try {
            Files.write(temp, bytes);
            moveInto(temp, output);
        } catch (IOException | RuntimeException e) {
            discard(temp, e);
            throw e;
        }
        int payloadSize = stream.payload().stream().mapToInt(array -> array.length).sum();
        LOG.debug("Compressed into '{}' [size = {}, overhead = {}]", output, bytes.length, bytes.length - payloadSize);
    }

    /** @throws cz.cvut.fit.acb.format.MalformedStreamException if the file is not an intact ACB stream */
    public CompressedStream openCompressed(Path path) throws IOException {
        return ContainerFormat.decode(Files.readAllBytes(path));
    }

    private static Path temporarySibling(Path target) throws IOException {
        Path absolute = target.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        return Files.createTempFile(absolute.getParent(), "." + absolute.getFileName() + ".", ".tmp");
    }

    private static void moveInto(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void discard(Path temp, Exception cause) {
        try {
            Files.deleteIfExists(temp);
        } catch (IOException e) {
            cause.addSuppressed(e);
        }
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

        private final Path target;
        private final Path temp;
        private final OutputStream stream;
        private boolean committed;

        private SegmentWriter(Path target, Path temp, OutputStream stream) {
            this.target = target;
            this.temp = temp;
            this.stream = stream;
        }

        @Override
        public void accept(byte[] segment) {
            try {
                this.stream.write(segment);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot write " + this.target, e);
            }
        }

        /** Moves everything written into place, replacing any file already there. */
        public void commit() throws IOException {
            this.stream.close();
            moveInto(this.temp, this.target);
            this.committed = true;
        }

        /** Discards what was written unless it was committed. */
        @Override
        public void close() throws IOException {
            if (this.committed) {
                return;
            }
            try {
                this.stream.close();
            } finally {
                Files.deleteIfExists(this.temp);
            }
        }
    }
}
