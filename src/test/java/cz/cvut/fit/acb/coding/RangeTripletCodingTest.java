package cz.cvut.fit.acb.coding;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldId;
import cz.cvut.fit.acb.triplets.TripletFieldKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RangeTripletCodingTest {

    private static final TripletFieldId DISTANCE = new TripletFieldId(0, 6, TripletFieldKind.DISTANCE);
    private static final TripletFieldId LENGTH = new TripletFieldId(1, 4, TripletFieldKind.LENGTH);
    private static final TripletFieldId LITERAL = new TripletFieldId(2, 8, TripletFieldKind.LITERAL);

    @Test
    void fieldsInterleavedInOneStreamComeBackInOrder() throws MalformedStreamException {
        RangeTripletWriter writer = new RangeTripletWriter(LengthFrequencies.flat());
        writer.write(DISTANCE, 63);
        writer.write(LENGTH, 15);
        writer.write(LITERAL, 255);
        writer.write(DISTANCE, 0);
        writer.write(LENGTH, 0);
        writer.write(LITERAL, 0);

        RangeTripletReader reader = new RangeTripletReader(writer.finish(), LengthFrequencies.flat());

        assertThat(new int[]{reader.read(DISTANCE), reader.read(LENGTH), reader.read(LITERAL),
                reader.read(DISTANCE), reader.read(LENGTH), reader.read(LITERAL)})
                .containsExactly(63, 15, 255, 0, 0, 0);
    }

    @Test
    void startingFrequenciesOfTheLengthFieldMustBeKnownToTheReaderToo() throws MalformedStreamException {
        LengthFrequencies frequencies = LengthFrequencies.of(45, 13, 10, 7, 5, 4);
        RangeTripletWriter writer = new RangeTripletWriter(frequencies);
        for (int length = 0; length < 16; length++) {
            writer.write(LENGTH, length);
        }

        RangeTripletReader reader = new RangeTripletReader(writer.finish(), frequencies);

        for (int length = 0; length < 16; length++) {
            assertThat(reader.read(LENGTH)).isEqualTo(length);
        }
    }

    @Test
    void aSkewedFieldCostsFewerBitsThanItsWidth() {
        RangeTripletWriter writer = new RangeTripletWriter(LengthFrequencies.flat());
        for (int i = 0; i < 1000; i++) {
            writer.write(LITERAL, 'a');
        }

        writer.finish();

        FieldCost literals = writer.costs().getFirst();
        assertThat(literals.kind()).isEqualTo(TripletFieldKind.LITERAL);
        assertThat(literals.symbols()).isEqualTo(1000);
        assertThat(literals.bits()).isLessThan(1000 * 2);
    }

    @Test
    void costsListOnlyTheKindsThatWereWritten() {
        RangeTripletWriter writer = new RangeTripletWriter(LengthFrequencies.flat());
        writer.write(LENGTH, 1);
        writer.write(LITERAL, 1);

        writer.finish();

        assertThat(writer.costs()).extracting(FieldCost::kind)
                .containsExactly(TripletFieldKind.LENGTH, TripletFieldKind.LITERAL);
    }

    @Test
    void aBlockThatRunsOutOfDataIsMalformedNotDecodedAsZeros() throws MalformedStreamException {
        RangeTripletReader reader = new RangeTripletReader(new byte[0], LengthFrequencies.flat());

        assertThatThrownBy(() -> {
            for (int i = 0; i < 10_000; i++) {
                reader.read(LITERAL);
            }
        }).isInstanceOf(MalformedStreamException.class);
    }
}
