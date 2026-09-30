package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.Triplet;
import cz.cvut.fit.acb.triplets.TripletLayout;
import cz.cvut.fit.acb.triplets.coder.FieldQueue.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripletLayoutTest {

    private static final int DISTANCE_BITS = 6;
    private static final int LENGTH_BITS = 4;

    private static TripletLayout layoutOf(TripletCoding coding) {
        return coding.layoutCoder().orElseThrow().layout(DISTANCE_BITS, LENGTH_BITS);
    }

    @Test
    void simpleWritesALiteralAloneWithADistanceAndLengthOfZero() {
        FieldQueue fields = new FieldQueue();

        layoutOf(TripletCoding.SIMPLE).write(Triplet.literal((byte) 'a'), fields);

        assertThat(fields.written()).containsExactly(new Field(0, 0), new Field(1, 0), new Field(2, 'a'));
    }

    @Test
    void simpleWritesAMatchAsDistanceLengthLiteral() {
        FieldQueue fields = new FieldQueue();

        layoutOf(TripletCoding.SIMPLE).write(Triplet.matchWithLiteral(3, 5, (byte) 'z'), fields);

        assertThat(fields.written()).containsExactly(new Field(0, 3), new Field(1, 5), new Field(2, 'z'));
    }

    @Test
    void valachWritesTheLengthFirstAndLeavesTheDistanceOutOfALiteral() {
        FieldQueue fields = new FieldQueue();

        layoutOf(TripletCoding.VALACH).write(Triplet.literal((byte) 'a'), fields);

        assertThat(fields.written()).containsExactly(new Field(0, 0), new Field(2, 'a'));
    }

    @Test
    void valachWritesAMatchAsLengthDistanceLiteral() {
        FieldQueue fields = new FieldQueue();

        layoutOf(TripletCoding.VALACH).write(Triplet.matchWithLiteral(3, 5, (byte) 'z'), fields);

        assertThat(fields.written()).containsExactly(new Field(0, 5), new Field(1, 3), new Field(2, 'z'));
    }

    @Test
    void salomonFlagsALiteralWithZeroAndABareMatchWithOne() {
        FieldQueue literal = new FieldQueue();
        FieldQueue match = new FieldQueue();

        layoutOf(TripletCoding.SALOMON).write(Triplet.literal((byte) 'a'), literal);
        layoutOf(TripletCoding.SALOMON).write(Triplet.match(3, 5), match);

        assertThat(literal.written()).containsExactly(new Field(0, 0), new Field(3, 'a'));
        assertThat(match.written()).containsExactly(new Field(0, 1), new Field(1, 3), new Field(2, 5));
    }

    @Test
    void salomon2KeepsTheLiteralOnAMatch() {
        FieldQueue fields = new FieldQueue();

        layoutOf(TripletCoding.SALOMON2).write(Triplet.matchWithLiteral(3, 5, (byte) 'z'), fields);

        assertThat(fields.written()).containsExactly(
                new Field(0, 1), new Field(1, 3), new Field(2, 5), new Field(3, 'z'));
    }

    @Test
    void aLayoutRefusesTheFormOfMatchItDoesNotCarry() {
        assertThatThrownBy(() -> layoutOf(TripletCoding.SALOMON)
                .write(Triplet.matchWithLiteral(0, 1, (byte) 'a'), new FieldQueue()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> layoutOf(TripletCoding.VALACH).write(Triplet.match(0, 1), new FieldQueue()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNegativeDistanceIsStoredInItsFieldWidthAndReadBackSigned() throws MalformedStreamException {
        FieldQueue fields = new FieldQueue();
        TripletLayout layout = layoutOf(TripletCoding.VALACH);

        layout.write(Triplet.matchWithLiteral(-32, 2, (byte) 'a'), fields);
        Triplet read = layout.read(fields);

        assertThat(fields.written()).contains(new Field(1, 32));
        assertThat(read).isEqualTo(Triplet.matchWithLiteral(-32, 2, (byte) 'a'));
    }

    @ParameterizedTest
    @EnumSource(value = TripletCoding.class, names = "ACB", mode = EnumSource.Mode.EXCLUDE)
    void aLayoutReadsBackALiteralAndTheMatchFormItCarries(TripletCoding coding) throws MalformedStreamException {
        TripletLayout layout = layoutOf(coding);
        FieldQueue fields = new FieldQueue();
        Triplet match = coding == TripletCoding.SALOMON
                ? Triplet.match(-5, 15) : Triplet.matchWithLiteral(-5, 15, (byte) 0x80);

        layout.write(Triplet.literal((byte) 0xFF), fields);
        layout.write(match, fields);

        assertThat(layout.read(fields)).isEqualTo(Triplet.literal((byte) 0xFF));
        assertThat(layout.read(fields)).isEqualTo(match);
    }

    @ParameterizedTest
    @EnumSource(value = TripletCoding.class, names = "ACB", mode = EnumSource.Mode.EXCLUDE)
    void aLayoutReportsAStreamThatEndsInsideATriplet(TripletCoding coding) {
        assertThatThrownBy(() -> layoutOf(coding).read(new FieldQueue()))
                .isInstanceOf(MalformedStreamException.class).hasMessageContaining("inside a triplet");
    }
}
