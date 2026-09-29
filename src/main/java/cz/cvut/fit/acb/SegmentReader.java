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

/** The segments of a file, read lazily {@code segmentSize} bytes at a time; close it when done. */
public final class SegmentReader implements Iterator<byte[]>, Closeable {

    private final Path path;
    private final SeekableByteChannel channel;
    private final int segmentSize;

    private SegmentReader(Path path, SeekableByteChannel channel, int segmentSize) {
        this.path = path;
        this.channel = channel;
        this.segmentSize = segmentSize;
    }

    static SegmentReader open(Path path, int segmentSize) throws IOException {
        if (segmentSize < 1) {
            throw new IllegalArgumentException("segment size must be greater than zero: " + segmentSize);
        }
        return new SegmentReader(path, Files.newByteChannel(path), segmentSize);
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
