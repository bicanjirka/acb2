package cz.cvut.fit.acb;

import cz.cvut.fit.acb.fixtures.CraftedStreams;
import cz.cvut.fit.acb.format.Block;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Well-formed containers whose blocks cannot be decoded: each must be reported, never crash. */
class CorruptPayloadTest {

    private static final TripletFieldId DISTANCE = new TripletFieldId(0, 6, TripletFieldKind.DISTANCE);
    private static final TripletFieldId LENGTH = new TripletFieldId(1, 4, TripletFieldKind.LENGTH);

    private static final CompressionSettings SIMPLE_BITS = CompressionSettings.defaults()
            .withTripletCoding(TripletCoding.SIMPLE).withLengthBits(4).withEntropyCoding(EntropyCoding.BIT_ARRAY);
    private static final CompressionSettings SIMPLE_RANGE = SIMPLE_BITS
            .withEntropyCoding(EntropyCoding.ADAPTIVE_ARITHMETIC);

    @Test
    void aContentRankOutsideTheDictionaryIsMalformed() {
        CompressedStream stream = CraftedStreams.oneBlock(SIMPLE_BITS, 10,
                Triplet.matchWithLiteral(5, 3, (byte) 'a'));

        assertThatThrownBy(() -> new Compressor(SIMPLE_BITS).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("outside a dictionary");
    }

    @Test
    void aTripletReachingPastTheEndOfItsSegmentIsMalformed() {
        CompressedStream stream = CraftedStreams.oneBlock(SIMPLE_BITS, 2,
                Triplet.literal((byte) 'a'), Triplet.matchWithLiteral(0, 3, (byte) 'b'));

        assertThatThrownBy(() -> new Compressor(SIMPLE_BITS).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("past the end of its segment");
    }

    @Test
    void aBlockEndingInsideATripletIsMalformed() {
        CompressedStream stream = CraftedStreams.oneBlock(SIMPLE_BITS, 10, fields -> {
            fields.write(DISTANCE, 0);
            fields.write(LENGTH, 0);
        });

        assertThatThrownBy(() -> new Compressor(SIMPLE_BITS).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("inside a triplet");
    }

    @Test
    void aRangeCodedBlockThatRunsOutOfDataIsMalformedNotDecodedAsZeros() {
        CompressedStream stream = new CompressedStream(StreamHeader.of(SIMPLE_RANGE),
                List.of(Block.coded(500, new byte[0])));

        assertThatThrownBy(() -> new Compressor(SIMPLE_RANGE).decompress(stream))
                .isInstanceOf(MalformedStreamException.class);
    }

    @Test
    void aBitArrayBlockTooShortForItsSegmentIsMalformed() {
        CompressedStream stream = new CompressedStream(StreamHeader.of(SIMPLE_BITS),
                List.of(Block.coded(10, new byte[3])));

        assertThatThrownBy(() -> new Compressor(SIMPLE_BITS).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("inside a triplet");
    }

    @Test
    void aBlockLongerThanTheHeadersSegmentSizeCannotBeInAStream() {
        StreamHeader header = StreamHeader.of(SIMPLE_BITS.withSegmentSize(4));
        List<Block> blocks = List.of(Block.coded(5, new byte[1]));

        assertThatThrownBy(() -> new CompressedStream(header, blocks)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aSoundStreamIsStillDecoded() throws MalformedStreamException {
        CompressedStream stream = CraftedStreams.oneBlock(SIMPLE_BITS, 3,
                Triplet.literal((byte) 'a'), Triplet.matchWithLiteral(0, 1, (byte) 'b'));

        assertThat(new Compressor(SIMPLE_BITS).decompress(stream)).containsExactly('a', 'a', 'b');
    }
}
