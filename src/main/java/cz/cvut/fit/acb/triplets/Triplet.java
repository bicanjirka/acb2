package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.format.MalformedStreamException;

import java.util.function.IntUnaryOperator;

/**
 * One step of coding a segment, before a layout turns it into fields: a literal byte, or a match
 * against an earlier context. A distance is signed, {@code context rank - content rank}.
 */
public sealed interface Triplet {

    /** What both sides can work out of the length of a match, from its distance; the decoder may find the distance unsound. */
    @FunctionalInterface
    interface ImpliedLength {

        int of(int distance) throws MalformedStreamException;
    }

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

    /**
     * This triplet as it is written: the length of a match less the bytes of it that the decoder
     * works out itself. A literal is as it is.
     *
     * @throws IllegalArgumentException if nothing of the match would be left
     */
    Triplet without(IntUnaryOperator implied);

    /**
     * Undoes {@link #without}: gives a match back the bytes of its length that were not written.
     *
     * @throws MalformedStreamException if the distance of a match is unsound
     */
    Triplet with(ImpliedLength implied) throws MalformedStreamException;

    record Literal(byte value) implements Triplet {

        @Override
        public int consumed() {
            return 1;
        }

        @Override
        public Triplet without(IntUnaryOperator implied) {
            return this;
        }

        @Override
        public Triplet with(ImpliedLength implied) {
            return this;
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

        @Override
        public Triplet without(IntUnaryOperator implied) {
            return new Match(this.distance, this.length - implied.applyAsInt(this.distance));
        }

        @Override
        public Triplet with(ImpliedLength implied) throws MalformedStreamException {
            return new Match(this.distance, this.length + implied.of(this.distance));
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

        @Override
        public Triplet without(IntUnaryOperator implied) {
            return new MatchWithLiteral(this.distance, this.length - implied.applyAsInt(this.distance), this.literal);
        }

        @Override
        public Triplet with(ImpliedLength implied) throws MalformedStreamException {
            return new MatchWithLiteral(this.distance, this.length + implied.of(this.distance), this.literal);
        }
    }

    private static void requirePositive(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("a match copies at least one byte: " + length);
        }
    }
}
