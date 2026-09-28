package cz.cvut.fit.acb.triplets.coder;

import cz.cvut.fit.acb.ACBProviderImpl;
import cz.cvut.fit.acb.CompressionSettings;
import cz.cvut.fit.acb.TripletCoding;
import cz.cvut.fit.acb.dictionary.ByteArray;
import cz.cvut.fit.acb.triplets.coder.FieldRecorder.Field;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The thesis's worked examples through the real dictionary, pinned to what docs/ALGORITHM.md
 * section 6 records for the current rules. When a rule in section 3.5 is removed, the expected
 * triplets here and there change together.
 */
class WorkedExampleTest {

    @Test
    void mississippiIsCodedAsTheSimpleTripletsOfSection6() {
        List<Field> fields = encode(TripletCoding.SIMPLE, "mississippi");

        assertThat(fields).containsExactlyElementsOf(concat(
                simple(0, 0, 'm'), simple(0, 0, 'i'), simple(0, 0, 's'),
                simple(1, 1, 'i'), simple(0, 3, 'p'), simple(1, 1, 'i')));
    }

    @Test
    void mississippiIsCodedAsTheValachTripletsOfSection6() {
        List<Field> fields = encode(TripletCoding.VALACH, "mississippi");

        assertThat(fields).containsExactlyElementsOf(concat(
                valachLiteral('m'), valachLiteral('i'), valachLiteral('s'),
                valachMatch(1, 1, 'i'), valachMatch(3, 0, 'p'), valachMatch(1, 1, 'i')));
    }

    @Test
    void mississippiIsCodedAsTheSalomonTripletsOfSection6() {
        List<Field> fields = encode(TripletCoding.SALOMON, "mississippi");

        assertThat(fields).containsExactlyElementsOf(concat(
                salomonLiteral('m'), salomonLiteral('i'), salomonLiteral('s'),
                salomonMatch(1, 1), salomonMatch(1, 4), salomonLiteral('p'),
                salomonMatch(1, 1), salomonMatch(1, 1)));
    }

    @Test
    void mississippiIsCodedAsTheSalomon2TripletsOfSection6() {
        List<Field> fields = encode(TripletCoding.SALOMON2, "mississippi");

        assertThat(fields).containsExactlyElementsOf(concat(
                salomonLiteral('m'), salomonLiteral('i'), salomonLiteral('s'),
                salomon2Match(1, 1, 'i'), salomon2Match(0, 3, 'p'), salomon2Match(1, 1, 'i')));
    }

    @Test
    void aRunOfOneByteIsCodedAsTheSimpleTripletsOfSection6() {
        List<Field> fields = encode(TripletCoding.SIMPLE, "sssss");

        assertThat(fields).containsExactlyElementsOf(concat(
                simple(0, 0, 's'), simple(0, 0, 's'), simple(0, 2, 's')));
    }

    private static List<Field> encode(TripletCoding coding, String text) {
        CompressionSettings settings = CompressionSettings.defaults().withTripletCoding(coding)
                .withDistanceBits(8).withLengthBits(8);
        ACBProviderImpl provider = new ACBProviderImpl(settings);
        ByteArray segment = new ByteArray(text.getBytes());
        FieldRecorder recorder = new FieldRecorder();

        provider.getCoder(segment, provider.getDictionary(segment)).encode(triplet -> triplet.visit(recorder));

        return recorder.written();
    }

    @SafeVarargs
    private static List<Field> concat(List<Field>... triplets) {
        return Arrays.stream(triplets).flatMap(List::stream).toList();
    }

    // Field indices follow each coder's TripletFieldId numbering.

    private static List<Field> simple(int distance, int length, char literal) {
        return List.of(new Field(0, distance), new Field(1, length), new Field(2, literal));
    }

    private static List<Field> valachLiteral(char literal) {
        return List.of(new Field(0, 0), new Field(2, literal));
    }

    private static List<Field> valachMatch(int length, int distance, char literal) {
        return List.of(new Field(0, length), new Field(1, distance), new Field(2, literal));
    }

    private static List<Field> salomonLiteral(char literal) {
        return List.of(new Field(0, 0), new Field(3, literal));
    }

    private static List<Field> salomonMatch(int distance, int length) {
        return List.of(new Field(0, 1), new Field(1, distance), new Field(2, length));
    }

    private static List<Field> salomon2Match(int distance, int length, char literal) {
        return List.of(new Field(0, 1), new Field(1, distance), new Field(2, length), new Field(3, literal));
    }
}
