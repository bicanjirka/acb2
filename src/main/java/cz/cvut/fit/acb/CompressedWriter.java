package cz.cvut.fit.acb;

import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.ContainerWriter;
import cz.cvut.fit.acb.format.StreamHeader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Writes a compressed stream to a file block by block: a {@link ContainerWriter} into a
 * {@link SegmentWriter}. {@link #commit()} seals the stream and moves the file into place; closing
 * without it leaves neither a partial file nor a replaced one.
 */
public final class CompressedWriter implements Consumer<Block>, Closeable {

    private final SegmentWriter file;
    private final ContainerWriter container;

    private CompressedWriter(SegmentWriter file, ContainerWriter container) {
        this.file = file;
        this.container = container;
    }

    /** The header and {@code blockCount}, the number of blocks that will follow, are written at once. */
    static CompressedWriter create(Path target, StreamHeader header, int blockCount) throws IOException {
        SegmentWriter file = SegmentWriter.create(target);
        try {
            return new CompressedWriter(file, ContainerWriter.begin(file, header, blockCount));
        } catch (RuntimeException e) {
            try {
                file.close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    @Override
    public void accept(Block block) {
        this.container.accept(block);
    }

    /** @throws IllegalStateException if fewer blocks were written than the header's count */
    public void commit() throws IOException {
        this.container.finish();
        this.file.commit();
    }

    @Override
    public void close() throws IOException {
        this.file.close();
    }
}
