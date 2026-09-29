package cz.cvut.fit.acb.triplets;

/**
 * What both sides know about a literal before it is coded: the byte before it, and a byte it cannot
 * be. Either is -1 when there is none.
 */
public record LiteralContext(int previous, int excluded) {

    private static final LiteralContext NONE = new LiteralContext(-1, -1);

    public static LiteralContext none() {
        return NONE;
    }

    public LiteralContext {
        if (previous < -1 || previous > 255 || excluded < -1 || excluded > 255) {
            throw new IllegalArgumentException("not bytes: " + previous + ", " + excluded);
        }
    }
}
