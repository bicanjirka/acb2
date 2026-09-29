package cz.cvut.fit.acb;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The segments of a file, read lazily {@code segmentSize} bytes at a time; close it when done. It
 * reads the length the file had when it was opened, so a file that grows meanwhile is cut off
 * there and one that shrinks fails.
 */
public final class SegmentReader implements Iterator<byte[]>, Closeable {

    private final Path path;
    private final SeekableByteChannel channel;
    private final int segmentSize;
    private final long size;
    private long position;

    private SegmentReader(Path path, SeekableByteChannel channel, int segmentSize, long size) {
        this.path = path;
        this.channel = channel;
        this.segmentSize = segmentSize;
        this.size = size;
    }

    static SegmentReader open(Path path, int segmentSize) throws IOException {
        if (segmentSize < 1) {
            throw new IllegalArgumentException("segment size must be greater than zero: " + segmentSize);
        }
        SeekableByteChannel channel = Files.newByteChannel(path);
        try {
            return new SegmentReader(path, channel, segmentSize, channel.size());
        } catch (IOException | RuntimeException e) {
            try {
                channel.close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    /** How many segments the file makes, the last one shorter if need be. */
    public int segmentCount() {
        return Math.toIntExact((this.size + this.segmentSize - 1) / this.segmentSize);
    }

    @Override
    public boolean hasNext() {
        return this.position < this.size;
    }

    @Override
    public byte[] next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            ByteBuffer segment = ByteBuffer.allocate((int) Math.min(this.segmentSize, this.size - this.position));
            while (segment.hasRemaining()) {
                if (this.channel.read(segment) < 0) {
                    throw new EOFException("File shrank while reading: " + this.path);
                }
            }
            this.position += segment.capacity();
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
