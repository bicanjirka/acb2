package cz.cvut.fit.acb;

import cz.cvut.fit.acb.coding.TripletWriter;
import cz.cvut.fit.acb.format.CompressedStream;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.format.StreamHeader;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Well-formed containers whose triplets cannot be decoded: each must be reported, never crash. */
class CorruptPayloadTest {

    private static final TripletFieldId DISTANCE = new TripletFieldId(0, 6, TripletFieldKind.DISTANCE);
    private static final TripletFieldId LENGTH = new TripletFieldId(1, 4, TripletFieldKind.LENGTH);
    private static final TripletFieldId LITERAL = new TripletFieldId(2, 8, TripletFieldKind.LITERAL);

    private static final CompressionSettings SIMPLE_BITS = CompressionSettings.defaults()
            .withTripletCoding(TripletCoding.SIMPLE).withLengthBits(4).withEntropyCoding(EntropyCoding.BIT_ARRAY);
    private static final CompressionSettings SIMPLE_ARITHMETIC = SIMPLE_BITS
            .withEntropyCoding(EntropyCoding.ADAPTIVE_ARITHMETIC);

    @Test
    void aContentRankOutsideTheDictionaryIsMalformed() {
        CompressedStream stream = stream(SIMPLE_BITS, 10, fields -> {
            fields.write(DISTANCE, 5);
            fields.write(LENGTH, 3);
            fields.write(LITERAL, 'a');
        });

        assertThatThrownBy(() -> new Compressor(SIMPLE_BITS).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("outside a dictionary");
    }

    @Test
    void aTripletReachingPastTheEndOfItsSegmentIsMalformed() {
        CompressedStream stream = stream(SIMPLE_BITS, 2, fields -> {
            fields.write(DISTANCE, 0);
            fields.write(LENGTH, 0);
            fields.write(LITERAL, 'a');
            fields.write(DISTANCE, 0);
            fields.write(LENGTH, 3);
            fields.write(LITERAL, 'b');
        });

        assertThatThrownBy(() -> new Compressor(SIMPLE_BITS).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("past the end of its segment");
    }

    @Test
    void aStreamEndingInsideATripletIsMalformed() {
        CompressedStream stream = stream(SIMPLE_BITS, 10, fields -> {
            fields.write(DISTANCE, 0);
            fields.write(LENGTH, 0);
        });

        assertThatThrownBy(() -> new Compressor(SIMPLE_BITS).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("inside a triplet");
    }

    @Test
    void aPayloadWithoutTheArrayOfAFieldIsMalformed() {
        CompressedStream stream = new CompressedStream(StreamHeader.of(SIMPLE_ARITHMETIC), List.of(size(10)));

        assertThatThrownBy(() -> new Compressor(SIMPLE_ARITHMETIC).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("no array for triplet field 0");
    }

    @Test
    void aPayloadWithFewerArraysThanFieldsIsMalformed() {
        CompressedStream stream = new CompressedStream(StreamHeader.of(SIMPLE_ARITHMETIC),
                List.of(size(10), new byte[8]));

        assertThatThrownBy(() -> new Compressor(SIMPLE_ARITHMETIC).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("no array for triplet field 1");
    }

    @Test
    void arithmeticFieldsThatRunOutOfDataAreMalformedNotDecodedAsZeros() {
        CompressedStream stream = new CompressedStream(StreamHeader.of(SIMPLE_ARITHMETIC),
                List.of(size(500), new byte[0], new byte[0], new byte[0]));

        assertThatThrownBy(() -> new Compressor(SIMPLE_ARITHMETIC).decompress(stream))
                .isInstanceOf(MalformedStreamException.class);
    }

    @Test
    void aBitStreamTooShortForItsBitCountIsMalformed() {
        CompressedStream stream = new CompressedStream(StreamHeader.of(SIMPLE_BITS), List.of(size(10), new byte[3]));

        assertThatThrownBy(() -> new Compressor(SIMPLE_BITS).decompress(stream))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("bit count");
    }

    @Test
    void aSoundStreamIsStillDecoded() throws MalformedStreamException {
        CompressedStream stream = stream(SIMPLE_BITS, 3, fields -> {
            fields.write(DISTANCE, 0);
            fields.write(LENGTH, 0);
            fields.write(LITERAL, 'a');
            fields.write(DISTANCE, 0);
            fields.write(LENGTH, 1);
            fields.write(LITERAL, 'b');
        });

        assertThat(new Compressor(SIMPLE_BITS).decompress(stream)).containsExactly('a', 'a', 'b');
    }

    private static CompressedStream stream(CompressionSettings settings, int segmentSize,
                                           Consumer<TripletWriter> fields) {
        TripletWriter writer = new ACBProviderImpl(settings).getTripletWriter();
        writer.setSize(segmentSize);
        fields.accept(writer);
        return new CompressedStream(StreamHeader.of(settings), writer.finish());
    }

    private static byte[] size(int segmentSize) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(segmentSize).array();
    }
}
