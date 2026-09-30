package cz.cvut.fit.acb.dictionary;

/** A set of byte values, as four words of bits: the bytes a literal cannot be, for instance. */
public record ByteSet(long zero, long one, long two, long three) {

    private static final ByteSet NONE = new ByteSet(0, 0, 0, 0);
    private static final int WORD_BITS = Long.SIZE;

    public static ByteSet none() {
        return NONE;
    }

    /** @throws IllegalArgumentException if a value is not a byte, that is 0 to 255 */
    public static ByteSet of(int... values) {
        Builder builder = builder();
        for (int value : values) {
            builder.add(value);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return (this.zero | this.one | this.two | this.three) == 0;
    }

    public boolean contains(int value) {
        return value >= 0 && value < Byte.MAX_VALUE - Byte.MIN_VALUE + 1
                && (this.word(value / WORD_BITS) >>> (value % WORD_BITS) & 1) != 0;
    }

    /** The least member that is at least {@code from}, or -1 if there is none. */
    public int next(int from) {
        int start = Math.max(0, from);
        for (int word = start / WORD_BITS; word < 4; word++) {
            long bits = this.word(word);
            if (word == start / WORD_BITS) {
                bits &= -1L << (start % WORD_BITS);
            }
            if (bits != 0) {
                return word * WORD_BITS + Long.numberOfTrailingZeros(bits);
            }
        }
        return -1;
    }

    private long word(int index) {
        return switch (index) {
            case 0 -> this.zero;
            case 1 -> this.one;
            case 2 -> this.two;
            default -> this.three;
        };
    }

    /** Collects the members of a set one by one; it is refilled for every literal, so it keeps its words. */
    public static final class Builder {

        private final long[] words = new long[4];

        private Builder() {
        }

        /** @throws IllegalArgumentException if {@code value} is not a byte, that is 0 to 255 */
        public Builder add(int value) {
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException("not a byte: " + value);
            }
            this.words[value / WORD_BITS] |= 1L << (value % WORD_BITS);
            return this;
        }

        public Builder addAll(ByteSet members) {
            this.words[0] |= members.zero;
            this.words[1] |= members.one;
            this.words[2] |= members.two;
            this.words[3] |= members.three;
            return this;
        }

        public ByteSet build() {
            return new ByteSet(this.words[0], this.words[1], this.words[2], this.words[3]);
        }
    }
}
