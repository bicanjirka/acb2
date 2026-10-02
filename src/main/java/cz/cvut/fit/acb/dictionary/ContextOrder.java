package cz.cvut.fit.acb.dictionary;

/** The order of dictionary entries: which of two positions has the lesser context. */
public interface ContextOrder {

    /**
     * Contexts compared right to left over at most {@code depth} bytes, as unsigned bytes; a context
     * that runs out of bytes, or ties, sorts by the smaller position first. Reads only bytes before
     * the positions. Eight bytes are compared at a time, which measured faster than one byte at a
     * time once contexts agree for long.
     */
    static ContextOrder byLastBytes(SegmentBuffer segment, int depth) {
        return new LastBytesOrder(segment, depth);
    }

    /** Negative, zero or positive as the context of {@code first} sorts before, with or after that of {@code second}. */
    int compare(int first, int second);

    /**
     * How many bits of context the two positions have in common, going back from each, the nearest byte
     * first and the top bit of a byte first, over at most the depth of the order: eight for each byte in
     * common, and the top bits of the next byte that are, unless a context ends there. Of three entries in
     * order, the first and the last have in common as many as the fewest of the two neighbours do, to the bit.
     */
    int sharedBits(int first, int second);

    /**
     * As {@link #sharedBits(int, int)}, given the {@link #prefix prefixes} of the two positions,
     * which spares reading the text when they differ.
     */
    int sharedBits(int first, long firstPrefix, int second, long secondPrefix);

    /** The byte at {@code position}, the first after its context, which must be known. */
    int followingByte(int position);

    /**
     * The first eight bytes of the context of {@code position}, nearest first, as an unsigned number
     * padded with zeros: of two positions with different prefixes, the lesser prefix is the lesser
     * context, so the comparison of the bytes can be left to those whose prefixes are equal.
     */
    long prefix(int position);
}
