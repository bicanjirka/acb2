package cz.cvut.fit.acb.dictionary;

/** The order of dictionary entries: which of two positions has the lesser context. */
@FunctionalInterface
public interface ContextOrder {

    /** Negative, zero or positive as the context of {@code first} sorts before, with or after that of {@code second}. */
    int compare(int first, int second);

    /**
     * Contexts compared right to left over at most {@code depth} bytes, as unsigned bytes; a context
     * that runs out of bytes, or ties, sorts by the smaller position first. Reads only bytes before
     * the positions.
     */
    static ContextOrder byLastBytes(SegmentBuffer segment, int depth) {
        return (first, second) -> {
            byte[] bytes = segment.bytes();
            int limit = Math.min(Math.min(first, second), depth);
            for (int k = 1; k <= limit; k++) {
                int a = Byte.toUnsignedInt(bytes[first - k]);
                int b = Byte.toUnsignedInt(bytes[second - k]);
                if (a != b) {
                    return a - b;
                }
            }
            return Integer.compare(first, second);
        };
    }
}
