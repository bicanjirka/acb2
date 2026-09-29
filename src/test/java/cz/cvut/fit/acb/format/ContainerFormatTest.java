package cz.cvut.fit.acb.format;

import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.EntropyCoding;
import cz.cvut.fit.acb.TripletCoding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainerFormatTest {

    private static final int SEGMENT_SIZE = 1000;
    private static final StreamHeader HEADER = new StreamHeader(6, 4, TripletCoding.VALACH, EntropyCoding.BIT_ARRAY,
            new int[]{45, 13, 10}, SEGMENT_SIZE);
    private static final List<Block> BLOCKS = List.of(Block.coded(7, new byte[]{1, 2, 3}),
            Block.stored(new byte[]{-1, 1, -128}), Block.coded(3, new byte[0]));

    /** Where the fields of {@link #HEADER} and the first block sit in an encoded stream. */
    private static final int SEGMENT_SIZE_OFFSET = 8;
    private static final int FIRST_FREQUENCY_OFFSET = 16;
    private static final int BLOCK_COUNT_OFFSET = FIRST_FREQUENCY_OFFSET + 3 * Integer.BYTES;
    private static final int FIRST_BLOCK_OFFSET = BLOCK_COUNT_OFFSET + Integer.BYTES;

    @ParameterizedTest
    @EnumSource(TripletCoding.class)
    void aStreamDecodesToTheHeaderAndBlocksItWasEncodedFrom(TripletCoding tripletCoding)
            throws MalformedStreamException {
        StreamHeader header = new StreamHeader(9, 3, tripletCoding, EntropyCoding.ADAPTIVE_ARITHMETIC,
                new int[]{2, 1}, SEGMENT_SIZE);

        CompressedStream decoded = ContainerFormat.decode(ContainerFormat.encode(new CompressedStream(header, BLOCKS)));

        assertThat(decoded.header()).isEqualTo(header);
        assertThat(decoded.blocks()).containsExactlyElementsOf(BLOCKS);
    }

    @Test
    void aStreamWithoutBlocksDecodesToOne() throws MalformedStreamException {
        CompressedStream decoded = ContainerFormat.decode(ContainerFormat.encode(new CompressedStream(HEADER,
                List.of())));

        assertThat(decoded.blocks()).isEmpty();
    }

    @Test
    void flippingAnyBitIsReportedAsMalformed() {
        byte[] encoded = encoded();

        for (int bit = 0; bit < encoded.length * Byte.SIZE; bit++) {
            byte[] corrupt = encoded.clone();
            corrupt[bit / Byte.SIZE] ^= (byte) (1 << (bit % Byte.SIZE));

            assertThatThrownBy(() -> ContainerFormat.decode(corrupt)).as("bit %d", bit)
                    .isInstanceOf(MalformedStreamException.class);
        }
    }

    @Test
    void everyTruncationIsReportedAsMalformed() {
        byte[] encoded = encoded();

        for (int length = 0; length < encoded.length; length++) {
            byte[] truncated = Arrays.copyOf(encoded, length);

            assertThatThrownBy(() -> ContainerFormat.decode(truncated)).as("length %d", length)
                    .isInstanceOf(MalformedStreamException.class);
        }
    }

    @Test
    void aJavaSerializedFileFromBeforeTheFormatIsNotAnAcbStream() {
        byte[] javaSerialization = {(byte) 0xAC, (byte) 0xED, 0x00, 0x05, 0x73, 0x72};

        assertThatThrownBy(() -> ContainerFormat.decode(javaSerialization))
                .isInstanceOf(MalformedStreamException.class).hasMessage("Not an ACB stream");
    }

    @Test
    void anotherVersionIsRejectedByName() {
        byte[] encoded = encoded();
        encoded[3] = (byte) (ContainerFormat.VERSION + 1);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("version");
    }

    @Test
    void anUnknownCoderCodeWithAValidChecksumIsRejected() {
        byte[] encoded = encoded();
        encoded[6] = 99;

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("triplet coding code 99");
    }

    @Test
    void aCodedLengthLargerThanTheStreamIsRejectedWithoutAllocatingIt() {
        byte[] encoded = encoded();
        int codedLengthOffset = FIRST_BLOCK_OFFSET + 1 + Integer.BYTES;
        ByteBuffer.wrap(encoded).putInt(codedLengthOffset, Integer.MAX_VALUE);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("coded byte count");
    }

    @Test
    void aStoredBlockLongerThanTheStreamIsRejectedWithoutAllocatingIt() {
        byte[] encoded = ContainerFormat.encode(new CompressedStream(HEADER, List.of(Block.stored(new byte[]{1}))));
        ByteBuffer.wrap(encoded).putInt(FIRST_BLOCK_OFFSET + 1, SEGMENT_SIZE);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("runs past the end");
    }

    @Test
    void aBlockLongerThanTheSegmentSizeIsRejected() {
        byte[] encoded = encoded();
        ByteBuffer.wrap(encoded).putInt(FIRST_BLOCK_OFFSET + 1, SEGMENT_SIZE + 1);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("block length");
    }

    @Test
    void aBlockOfNoBytesIsRejected() {
        byte[] encoded = encoded();
        ByteBuffer.wrap(encoded).putInt(FIRST_BLOCK_OFFSET + 1, 0);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("block length");
    }

    @Test
    void anUnknownBlockKindIsRejected() {
        byte[] encoded = encoded();
        encoded[FIRST_BLOCK_OFFSET] = 9;

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("block kind 9");
    }

    @Test
    void aBlockCountLargerThanTheStreamCouldHoldIsRejected() {
        byte[] encoded = encoded();
        ByteBuffer.wrap(encoded).putInt(BLOCK_COUNT_OFFSET, Integer.MAX_VALUE);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("block count");
    }

    @Test
    void aSegmentSizeOfZeroIsRejected() {
        byte[] encoded = encoded();
        ByteBuffer.wrap(encoded).putInt(SEGMENT_SIZE_OFFSET, 0);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("segment size");
    }

    @Test
    void aFieldWiderThanTheSettingsAllowAreRejectedBeforeAnyModelIsBuilt() {
        byte[] encoded = encoded();
        encoded[4] = (byte) (CompressionSettings.MAX_FIELD_BITS + 1);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("distance bit width");
    }

    @Test
    void moreLengthFrequenciesThanTheLengthAlphabetHasSymbolsAreRejected() {
        StreamHeader narrow = new StreamHeader(6, 1, TripletCoding.VALACH, EntropyCoding.ADAPTIVE_ARITHMETIC,
                new int[]{1, 1, 1}, SEGMENT_SIZE);
        byte[] encoded = ContainerFormat.encode(new CompressedStream(narrow, BLOCKS));

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("frequency count");
    }

    @Test
    void lengthFrequenciesTheModelCannotTotalAreRejected() {
        byte[] encoded = encoded();
        ByteBuffer.wrap(encoded).putInt(FIRST_FREQUENCY_OFFSET, Integer.MAX_VALUE);

        assertThatThrownBy(() -> ContainerFormat.decode(withChecksum(encoded)))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("length frequencies");
    }

    private static byte[] encoded() {
        return ContainerFormat.encode(new CompressedStream(HEADER, BLOCKS));
    }

    /** Re-seals edited bytes, so a test reaches the check behind the checksum. */
    private static byte[] withChecksum(byte[] encoded) {
        CRC32 crc = new CRC32();
        crc.update(encoded, 0, encoded.length - Integer.BYTES);
        ByteBuffer.wrap(encoded).putInt(encoded.length - Integer.BYTES, (int) crc.getValue());
        return encoded;
    }
}
