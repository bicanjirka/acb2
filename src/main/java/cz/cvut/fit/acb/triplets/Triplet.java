package cz.cvut.fit.acb.triplets;

/**
 * One step of coding a segment, before a layout turns it into fields: a literal byte, or a match
 * against an earlier context. A distance is signed, {@code context rank - content rank}.
 */
public sealed interface Triplet {

    static Triplet literal(byte value) {
        return new Literal(value);
    }

    /** A match with no literal after it: the segment goes on with the byte after the copy. */
    static Triplet match(int distance, int length) {
        return new Match(distance, length);
    }

    /** A match followed by the byte that ended it. */
    static Triplet matchWithLiteral(int distance, int length, byte literal) {
        return new MatchWithLiteral(distance, length, literal);
    }

    /** How many bytes of the segment the step codes. */
    int consumed();

    record Literal(byte value) implements Triplet {

        @Override
        public int consumed() {
            return 1;
        }
    }

    record Match(int distance, int length) implements Triplet {

        public Match {
            requirePositive(length);
        }

        @Override
        public int consumed() {
            return this.length;
        }
    }

    record MatchWithLiteral(int distance, int length, byte literal) implements Triplet {

        public MatchWithLiteral {
            requirePositive(length);
        }

        @Override
        public int consumed() {
            return this.length + 1;
        }
    }

    private static void requirePositive(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("a match copies at least one byte: " + length);
        }
    }
}
