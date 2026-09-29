package cz.cvut.fit.acb.format;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.zip.CRC32;

/**
 * Writes a stream in the {@link ContainerFormat} block by block, handing each piece of bytes to
 * {@code out} as it is made, so nothing but one block is held. The format puts the block count
 * before the blocks, so it is given up front; {@link #finish()} checks it was kept and seals the
 * stream with its checksum.
 */
public final class ContainerWriter implements Consumer<Block> {

    /** Writes into the buffer of one piece; the pieces are what {@code out} receives. */
    @FunctionalInterface
    private interface Piece {
        void write(DataOutputStream out) throws IOException;
    }

    private final Consumer<byte[]> out;
    private final CRC32 checksum = new CRC32();
    private final int segmentSize;
    private final int blockCount;
    private int written;

    private ContainerWriter(Consumer<byte[]> out, int segmentSize, int blockCount) {
        this.out = out;
        this.segmentSize = segmentSize;
        this.blockCount = blockCount;
    }

    /** Writes the header and the block count at once. */
    public static ContainerWriter begin(Consumer<byte[]> out, StreamHeader header, int blockCount) {
        if (blockCount < 0) {
            throw new IllegalArgumentException("block count must not be negative: " + blockCount);
        }
        ContainerWriter writer = new ContainerWriter(out, header.segmentSize(), blockCount);
        writer.emit(piece -> {
            piece.write(ContainerFormat.MAGIC);
            piece.writeByte(ContainerFormat.VERSION);
            HeaderCodec.write(header, piece);
            piece.writeInt(blockCount);
        });
        return writer;
    }

    /**
     * @throws IllegalStateException if the declared count of blocks is already written
     * @throws IllegalArgumentException if the block is longer than the segment size of the header
     */
    @Override
    public void accept(Block block) {
        if (this.written == this.blockCount) {
            throw new IllegalStateException("More blocks than the " + this.blockCount + " declared");
        }
        if (block.rawLength() > this.segmentSize) {
            throw new IllegalArgumentException("A block of " + block.rawLength() + " bytes exceeds the segment size "
                    + this.segmentSize);
        }
        this.written++;
        this.emit(piece -> BlockCodec.write(block, piece));
    }

    /** @throws IllegalStateException if fewer blocks were written than declared */
    public void finish() {
        if (this.written != this.blockCount) {
            throw new IllegalStateException("Only " + this.written + " of the " + this.blockCount
                    + " declared blocks were written");
        }
        this.out.accept(ByteBuffer.allocate(Integer.BYTES).putInt((int) this.checksum.getValue()).array());
    }

    private void emit(Piece body) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            body.write(new DataOutputStream(bytes));
            byte[] piece = bytes.toByteArray();
            this.checksum.update(piece);
            this.out.accept(piece);
        } catch (IOException e) {
            throw new UncheckedIOException("In-memory write failed", e);
        }
    }
}
