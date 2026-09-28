package cz.cvut.fit.acb.dictionary;

/** The order of dictionary entries: which of two positions has the lesser context. */
@FunctionalInterface
public interface ContextOrder {

    /** Bytes of context that decide the order; contexts equal over that depth are ordered by position. */
    int DEPTH = 10;

    /** Negative, zero or positive as the context of {@code first} sorts before, with or after that of {@code second}. */
    int compare(int first, int second);

    /**
     * Contexts compared right to left over at most {@link #DEPTH} bytes, as signed bytes; a context
     * that runs out of bytes, or ties, sorts by the smaller position first. Reads only bytes before
     * the positions.
     */
    static ContextOrder byLastBytes(ByteSequence sequence) {
        return (first, second) -> {
            int limit = Math.min(Math.min(first, second), DEPTH);
            for (int k = 1; k <= limit; k++) {
                byte a = sequence.byteAt(first - k);
                byte b = sequence.byteAt(second - k);
                if (a != b) {
                    return a - b;
                }
            }
            return Integer.compare(first, second);
        };
    }
}
