package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.ContainerFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        return SegmentReader.open(path, segmentSize);
    }

    /**
     * Writes decoded segments in order. Call {@link SegmentWriter#commit()} when all are written;
     * closing without it discards them.
     */
    public SegmentWriter writeSegments(Path path) throws IOException {
        return SegmentWriter.create(path);
    }

    public void saveCompressed(CompressedStream stream, Path output) throws IOException {
        byte[] bytes = ContainerFormat.encode(stream);
        try (SegmentWriter writer = SegmentWriter.create(output)) {
            writer.accept(bytes);
            writer.commit();
        }
        int blockBytes = stream.blocks().stream().mapToInt(Block::storedLength).sum();
        LOG.debug("Compressed into '{}' [size = {}, overhead = {}]", output, bytes.length, bytes.length - blockBytes);
    }

    /** @throws cz.cvut.fit.acb.format.MalformedStreamException if the file is not an intact ACB stream */
    public CompressedStream openCompressed(Path path) throws IOException {
        return ContainerFormat.decode(Files.readAllBytes(path));
    }
}
