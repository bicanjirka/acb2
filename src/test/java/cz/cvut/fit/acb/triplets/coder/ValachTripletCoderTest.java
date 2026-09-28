package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.dictionary.SegmentBuffer;
import cz.cvut.fit.acb.triplets.coder.FieldRecorder.Field;
import org.junit.jupiter.api.Test;

import static cz.cvut.fit.acb.triplets.coder.FakeDictionary.match;
import static cz.cvut.fit.acb.triplets.coder.FakeDictionary.noMatch;
import static org.assertj.core.api.Assertions.assertThat;

class ValachTripletCoderTest {

    private static final int LENGTH = 0;
    private static final int DISTANCE = 1;
    private static final int LITERAL = 2;

    @Test
    void aOneByteMatchEndingTheSegmentIsWrittenAsALiteral() {
        SegmentBuffer segment = SegmentBuffer.of(new byte[]{'a', 'b'});
        FakeDictionary dictionary = FakeDictionary.matching(idx -> idx == 1 ? match(0, 1) : noMatch());
        FieldRecorder fields = new FieldRecorder();

        new ValachTripletCoder(segment, dictionary, 6, 4).encode(triplet -> triplet.visit(fields));

        assertThat(fields.written()).containsExactly(
                new Field(LENGTH, 0), new Field(LITERAL, 'a'),
                new Field(LENGTH, 0), new Field(LITERAL, 'b'));
    }

    @Test
    void aLongerMatchEndingTheSegmentGivesUpItsLastByteAsTheLiteral() {
        SegmentBuffer segment = SegmentBuffer.of(new byte[]{'a', 'a', 'z'});
        FakeDictionary dictionary = FakeDictionary.matching(idx -> idx == 1 ? match(3, 2) : noMatch());
        FieldRecorder fields = new FieldRecorder();

        new ValachTripletCoder(segment, dictionary, 6, 4).encode(triplet -> triplet.visit(fields));

        assertThat(fields.written()).containsExactly(
                new Field(LENGTH, 0), new Field(LITERAL, 'a'),
                new Field(LENGTH, 1), new Field(DISTANCE, 3), new Field(LITERAL, 'z'));
    }
}
