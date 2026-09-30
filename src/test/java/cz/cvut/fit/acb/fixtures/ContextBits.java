package cz.cvut.fit.acb.fixtures;

/** How far two contexts agree, counted the obvious way, to check the fast ways against. */
public final class ContextBits {

    private ContextBits() {
    }

    /**
     * The equal bits of the contexts before {@code first} and before {@code second}, counted one bit at a
     * time, the nearest byte first and the top bit of a byte first, over at most {@code maxBytes} bytes
     * and never past the start of {@code bytes}.
     */
    public static int between(byte[] bytes, int first, int second, int maxBytes) {
        int limit = Math.min(maxBytes, Math.min(first, second));
        int bits = 0;
        for (int k = 1; k <= limit; k++) {
            for (int bit = 7; bit >= 0; bit--) {
                boolean equal = ((bytes[first - k] >> bit) & 1) == ((bytes[second - k] >> bit) & 1);
                if (!equal) {
                    return bits;
                }
                bits++;
            }
        }
        return bits;
    }
}
