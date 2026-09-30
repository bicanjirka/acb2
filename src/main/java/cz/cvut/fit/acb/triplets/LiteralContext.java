package cz.cvut.fit.acb.triplets;

import cz.cvut.fit.acb.dictionary.ByteSet;

import java.util.Objects;

/**
 * What both sides know about a literal before it is coded: the byte before it, or -1 if there is none,
 * and the bytes it cannot be, which are those that would have made a match longer.
 */
public record LiteralContext(int previous, ByteSet excluded) {

    private static final LiteralContext NONE = new LiteralContext(-1, ByteSet.none());

    public static LiteralContext none() {
        return NONE;
    }

    public LiteralContext {
        if (previous < -1 || previous > 255) {
            throw new IllegalArgumentException("not a byte: " + previous);
        }
        Objects.requireNonNull(excluded, "excluded");
    }
}
