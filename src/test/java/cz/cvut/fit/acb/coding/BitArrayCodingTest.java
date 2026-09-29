package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BitArrayCodingTest {

    private static final TripletFieldId FLAG = new TripletFieldId(0, 1, TripletFieldKind.FLAG);
    private static final TripletFieldId DISTANCE = new TripletFieldId(1, 6, TripletFieldKind.DISTANCE);
    private static final TripletFieldId LITERAL = new TripletFieldId(2, 8, TripletFieldKind.LITERAL);

    @Test
    void fieldsAreWrittenAtTheirFullWidthMostSignificantBitFirstWithTheLastByteZeroPadded() {
        BitArrayWriter writer = new BitArrayWriter();
        writer.write(FLAG, 1);
        writer.write(DISTANCE, 0b101010);
        writer.write(LITERAL, 0b11000001);

        byte[] block = writer.finish();

        assertThat(block).containsExactly((byte) 0b11010101, (byte) 0b10000010);
    }

    @Test
    void fieldsComeBackInOrderAcrossByteBoundaries() throws MalformedStreamException {
        BitArrayWriter writer = new BitArrayWriter();
        writer.write(FLAG, 1);
        writer.write(DISTANCE, 63);
        writer.write(LITERAL, 200);
        writer.write(FLAG, 0);
        writer.write(LITERAL, 7);
        BitArrayReader reader = new BitArrayReader(writer.finish());

        int[] read = {reader.read(FLAG), reader.read(DISTANCE), reader.read(LITERAL), reader.read(FLAG),
                reader.read(LITERAL)};

        assertThat(read).containsExactly(1, 63, 200, 0, 7);
    }

    @Test
    void costsAreTheWidthsOfTheFieldsWritten() {
        BitArrayWriter writer = new BitArrayWriter();
        writer.write(DISTANCE, 1);
        writer.write(DISTANCE, 2);
        writer.write(LITERAL, 3);

        writer.finish();

        assertThat(writer.costs()).containsExactly(new FieldCost(TripletFieldKind.DISTANCE, 2, 12),
                new FieldCost(TripletFieldKind.LITERAL, 1, 8));
    }

    @Test
    void readingPastTheEndOfTheBlockIsMalformed() {
        BitArrayReader reader = new BitArrayReader(new byte[]{1});

        assertThatThrownBy(() -> {
            reader.read(LITERAL);
            reader.read(FLAG);
        }).isInstanceOf(MalformedStreamException.class);
    }
}
