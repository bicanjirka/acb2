package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.ByteSource;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerReader;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The file side of the CLI: reads inputs as segments, writes decoded segments, and stores
 * compressed streams in the container format, all a block at a time. An output is written to a
 * temporary sibling and moved into place when complete, so a failure leaves neither a partial file
 * nor a replaced one.
 */
public final class ACBFileIO {

    private static final Logger LOG = LogManager.getLogger();

    /** A file that can be read from the start again, as {@link ContainerReader} needs. */
    private record FileSource(Path path) implements ByteSource {

        @Override
        public long size() throws IOException {
            return Files.size(this.path);
        }

        @Override
        public InputStream open() throws IOException {
            return Files.newInputStream(this.path);
        }
    }

    /** Reads {@code path} lazily, {@code segmentSize} bytes at a time; close it when done. */
    public SegmentReader readSegments(Path path, int segmentSize) throws IOException {
        return SegmentReader.open(path, segmentSize);
    }

    /**
     * Writes decoded segments in order. Call {@link SegmentWriter#commit()} when all are written;
     * closing without it discards them.
     */
    public SegmentWriter writeSegments(Path path) throws IOException {
        return SegmentWriter.create(path);
    }

    /**
     * Writes a compressed stream of {@code blockCount} blocks block by block. Call
     * {@link CompressedWriter#commit()} when all are written; closing without it discards them.
     */
    public CompressedWriter createCompressed(Path output, StreamHeader header, int blockCount) throws IOException {
        return CompressedWriter.create(output, header, blockCount);
    }

    public void saveCompressed(CompressedStream stream, Path output) throws IOException {
        try (CompressedWriter writer = this.createCompressed(output, stream.header(), stream.blocks().size())) {
            stream.blocks().forEach(writer);
            writer.commit();
        }
        LOG.debug("Compressed into '{}' [size = {}]", output, Files.size(output));
    }

    /**
     * The blocks of a compressed file, read as they are asked for once the whole file has been
     * checked; close it when done.
     *
     * @throws MalformedStreamException if the file is not an intact ACB stream
     */
    public ContainerReader readCompressed(Path path) throws IOException {
        return ContainerReader.open(new FileSource(path));
    }

    /** @throws MalformedStreamException if the file is not an intact ACB stream */
    public CompressedStream openCompressed(Path path) throws IOException {
        try (ContainerReader reader = this.readCompressed(path)) {
            return new CompressedStream(reader.header(), reader.drain());
        }
    }
}
