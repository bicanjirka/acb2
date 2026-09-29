package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.coding.LengthFrequencies;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainerStreamingTest {

    private static final int SEGMENT_SIZE = 1000;
    private static final StreamHeader HEADER = new StreamHeader(6, 4, TripletCoding.VALACH,
            EntropyCoding.ADAPTIVE_ARITHMETIC, LengthFrequencies.of(3, 2), SEGMENT_SIZE, 10);
    private static final List<Block> BLOCKS = List.of(Block.coded(7, new byte[]{1, 2, 3}),
            Block.stored(new byte[]{-1, 1, -128}), Block.coded(900, new byte[400]));

    @Test
    void aWriterHandsOnEachPieceAsItIsMadeAndSealsTheStreamOnlyWhenFinished() {
        List<byte[]> pieces = new ArrayList<>();
        ContainerWriter writer = ContainerWriter.begin(pieces::add, HEADER, BLOCKS.size());
        int afterHeader = pieces.size();

        BLOCKS.forEach(writer);
        int beforeFinish = pieces.size();
        writer.finish();

        assertThat(afterHeader).isEqualTo(1);
        assertThat(beforeFinish).isEqualTo(1 + BLOCKS.size());
        assertThat(pieces).hasSize(beforeFinish + 1);
        assertThat(pieces.getLast()).hasSize(Integer.BYTES);
    }

    @Test
    void thePiecesOfAWriterJoinIntoAStreamThatDecodes() throws MalformedStreamException {
        ByteArrayOutputStream joined = new ByteArrayOutputStream();
        ContainerWriter writer = ContainerWriter.begin(joined::writeBytes, HEADER, BLOCKS.size());
        BLOCKS.forEach(writer);
        writer.finish();

        CompressedStream decoded = ContainerFormat.decode(joined.toByteArray());

        assertThat(decoded.header()).isEqualTo(HEADER);
        assertThat(decoded.blocks()).containsExactlyElementsOf(BLOCKS);
    }

    @Test
    void finishingBeforeTheDeclaredBlocksAreWrittenIsRejected() {
        ContainerWriter writer = ContainerWriter.begin(piece -> {
        }, HEADER, 2);
        writer.accept(BLOCKS.getFirst());

        assertThatThrownBy(writer::finish).isInstanceOf(IllegalStateException.class).hasMessageContaining("1 of the 2");
    }

    @Test
    void aBlockBeyondTheDeclaredCountIsRejected() {
        ContainerWriter writer = ContainerWriter.begin(piece -> {
        }, HEADER, 1);
        writer.accept(BLOCKS.getFirst());

        assertThatThrownBy(() -> writer.accept(BLOCKS.get(1))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aBlockLongerThanTheSegmentSizeIsRejectedByTheWriter() {
        ContainerWriter writer = ContainerWriter.begin(piece -> {
        }, HEADER, 1);

        assertThatThrownBy(() -> writer.accept(Block.stored(new byte[SEGMENT_SIZE + 1])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNegativeBlockCountIsRejected() {
        assertThatThrownBy(() -> ContainerWriter.begin(piece -> {
        }, HEADER, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aReaderDeliversTheBlocksOneAtATimeAndThenNone() throws IOException {
        try (ContainerReader reader = ContainerReader.open(ByteSource.of(encoded(BLOCKS)))) {
            List<Optional<Block>> delivered = new ArrayList<>();
            for (int i = 0; i <= BLOCKS.size(); i++) {
                delivered.add(reader.next());
            }

            assertThat(reader.header()).isEqualTo(HEADER);
            assertThat(reader.blockCount()).isEqualTo(BLOCKS.size());
            assertThat(delivered).containsExactly(Optional.of(BLOCKS.get(0)), Optional.of(BLOCKS.get(1)),
                    Optional.of(BLOCKS.get(2)), Optional.empty());
        }
    }

    @Test
    void aReaderChecksTheWholeStreamBeforeItDeliversTheFirstBlock() {
        byte[] encoded = encoded(BLOCKS);
        encoded[encoded.length - Integer.BYTES - 1] ^= 1;

        assertThatThrownBy(() -> ContainerReader.open(ByteSource.of(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("checksum");
    }

    @Test
    void aStreamTooShortToHoldAPrefixAndAChecksumIsNotAnAcbStream() {
        assertThatThrownBy(() -> ContainerReader.open(ByteSource.of(new byte[]{'A', 'C', 'B', 4, 0, 0, 0})))
                .isInstanceOf(MalformedStreamException.class).hasMessage("Not an ACB stream");
    }

    @Test
    void bytesLeftAfterTheLastDeclaredBlockAreFoundWhenTheEndIsAsked() throws IOException {
        byte[] encoded = encoded(BLOCKS);
        int blockCountOffset = ContainerFormat.encode(new CompressedStream(HEADER, List.of())).length - 2 * Integer.BYTES;
        ByteBuffer.wrap(encoded).putInt(blockCountOffset, BLOCKS.size() - 1);

        try (ContainerReader reader = ContainerReader.open(ByteSource.of(seal(encoded)))) {
            reader.next();
            reader.next();

            assertThatThrownBy(reader::next).isInstanceOf(MalformedStreamException.class)
                    .hasMessageContaining("unexpected bytes after the last block");
        }
    }

    @Test
    void aCodedLengthLongerThanASegmentIsRejectedEvenWhenTheStreamHoldsThoseBytes() {
        StreamHeader small = new StreamHeader(6, 4, TripletCoding.VALACH, EntropyCoding.ADAPTIVE_ARITHMETIC,
                LengthFrequencies.flat(), 10, 10);
        byte[] encoded = ContainerFormat.encode(new CompressedStream(small, List.of(Block.coded(5, new byte[20]))));

        assertThatThrownBy(() -> ContainerFormat.decode(encoded)).isInstanceOf(MalformedStreamException.class)
                .hasMessageContaining("coded byte count 20");
    }

    private static byte[] encoded(List<Block> blocks) {
        return ContainerFormat.encode(new CompressedStream(HEADER, blocks));
    }

    private static byte[] seal(byte[] encoded) {
        CRC32 crc = new CRC32();
        crc.update(encoded, 0, encoded.length - Integer.BYTES);
        ByteBuffer.wrap(encoded).putInt(encoded.length - Integer.BYTES, (int) crc.getValue());
        return encoded;
    }
}
