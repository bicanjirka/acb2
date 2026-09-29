package cz.cvut.fit.acb.format;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.zip.CRC32;

/**
 * Reads a stream in the {@link ContainerFormat} block by block. {@link #open} reads the whole
 * source once to check the magic, the version and the checksum before it trusts anything in it,
 * then reads the header from a second pass; from there every count is bounded by what the stream
 * has left and every block by the segment size, so a damaged stream cannot make it allocate more.
 * Close it when done.
 */
public final class ContainerReader implements BlockSource, Closeable {

    private static final int CHECKSUM_BYTES = Integer.BYTES;
    private static final int PREFIX_BYTES = ContainerFormat.MAGIC.length + 1;
    private static final int CHUNK = 1 << 16;

    private final InputStream stream;
    private final WireReader in;
    private final StreamHeader header;
    private final int blockCount;
    private int delivered;

    private ContainerReader(InputStream stream, WireReader in, StreamHeader header, int blockCount) {
        this.stream = stream;
        this.in = in;
        this.header = header;
        this.blockCount = blockCount;
    }

    /** @throws MalformedStreamException if the source is not an intact ACB stream */
    public static ContainerReader open(ByteSource source) throws IOException, MalformedStreamException {
        long size = source.size();
        verify(source, size);
        InputStream stream = source.open();
        try {
            stream.skipNBytes(PREFIX_BYTES);
            WireReader in = new WireReader(stream, size - PREFIX_BYTES - CHECKSUM_BYTES);
            StreamHeader header = HeaderCodec.read(in);
            return new ContainerReader(stream, in, header, BlockCodec.readCount(in));
        } catch (IOException | RuntimeException e) {
            try {
                stream.close();
            } catch (IOException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    public StreamHeader header() {
        return this.header;
    }

    /** How many blocks the stream declares. */
    public int blockCount() {
        return this.blockCount;
    }

    /** The last call, the one that finds no block, also finds bytes left after the last one. */
    @Override
    public Optional<Block> next() throws MalformedStreamException {
        if (this.delivered == this.blockCount) {
            if (this.in.remaining() > 0) {
                throw new MalformedStreamException(this.in.remaining() + " unexpected bytes after the last block");
            }
            return Optional.empty();
        }
        this.delivered++;
        return Optional.of(BlockCodec.read(this.in, this.header.segmentSize()));
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    private static void verify(ByteSource source, long size) throws IOException, MalformedStreamException {
        if (size < PREFIX_BYTES + CHECKSUM_BYTES) {
            throw new MalformedStreamException("Not an ACB stream");
        }
        try (InputStream stream = source.open()) {
            CRC32 crc = new CRC32();
            byte[] prefix = stream.readNBytes(PREFIX_BYTES);
            requireMagicAndVersion(prefix);
            crc.update(prefix);
            byte[] chunk = new byte[CHUNK];
            long left = size - PREFIX_BYTES - CHECKSUM_BYTES;
            while (left > 0) {
                int read = stream.read(chunk, 0, (int) Math.min(chunk.length, left));
                if (read < 0) {
                    throw new MalformedStreamException("ACB stream is corrupt or truncated (checksum mismatch)");
                }
                crc.update(chunk, 0, read);
                left -= read;
            }
            byte[] stored = stream.readNBytes(CHECKSUM_BYTES);
            if (stored.length < CHECKSUM_BYTES || ByteBuffer.wrap(stored).getInt() != (int) crc.getValue()) {
                throw new MalformedStreamException("ACB stream is corrupt or truncated (checksum mismatch)");
            }
        }
    }

    private static void requireMagicAndVersion(byte[] prefix) throws MalformedStreamException {
        if (prefix.length < PREFIX_BYTES) {
            throw new MalformedStreamException("Not an ACB stream");
        }
        for (int i = 0; i < ContainerFormat.MAGIC.length; i++) {
            if (prefix[i] != ContainerFormat.MAGIC[i]) {
                throw new MalformedStreamException("Not an ACB stream");
            }
        }
        int version = Byte.toUnsignedInt(prefix[ContainerFormat.MAGIC.length]);
        if (version != ContainerFormat.VERSION) {
            throw new MalformedStreamException("Unsupported ACB stream version " + version + ", expected "
                    + ContainerFormat.VERSION);
        }
    }
}
