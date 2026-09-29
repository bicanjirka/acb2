package cz.cvut.fit.acb;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

/**
 * Writes segments in order to a temporary sibling of the target. {@link #commit()} moves the file
 * into place once all are written; closing without it discards them, so a failure leaves neither a
 * partial file nor a replaced one.
 */
public final class SegmentWriter implements Consumer<byte[]>, Closeable {

    private final Path target;
    private final Path temp;
    private final OutputStream stream;
    private boolean committed;

    private SegmentWriter(Path target, Path temp, OutputStream stream) {
        this.target = target;
        this.temp = temp;
        this.stream = stream;
    }

    static SegmentWriter create(Path target) throws IOException {
        Path temp = temporarySibling(target);
        try {
            return new SegmentWriter(target, temp, Files.newOutputStream(temp));
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
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
        try {
            Files.move(this.temp, this.target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(this.temp, this.target, StandardCopyOption.REPLACE_EXISTING);
        }
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

    private static Path temporarySibling(Path target) throws IOException {
        Path absolute = target.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        return Files.createTempFile(absolute.getParent(), "." + absolute.getFileName() + ".", ".tmp");
    }
}
