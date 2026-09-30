package cz.cvut.fit.acb.dictionary;

import java.util.Arrays;

/**
 * Common prefixes of contents for the rules that imply one, {@link MatchRule#SMALLEST_WITH_LCP} and
 * {@link MatchRule#NEAREST_WITH_PREFIX}, and the order of contents the first of them needs. The
 * encoder and the decoder see only the bytes before the position being coded, so a content is
 * compared as far as that and one that ends there sorts before every longer content it is a prefix of.
 */
final class LcpMatches {

    private final SegmentBuffer segment;

    LcpMatches(SegmentBuffer segment) {
        this.segment = segment;
    }

    /** Negative, zero or positive as the content at {@code first} sorts before, with or after that at {@code second}. */
    int compare(int idx, int first, int second) {
        return this.order(idx, first, second, this.commonPrefix(idx, first, second));
    }

    /** How many bytes the two contents share, over the bytes before {@code idx}. */
    int commonPrefix(int idx, int first, int second) {
        byte[] bytes = this.segment.bytes();
        int mismatch = Arrays.mismatch(bytes, first, idx, bytes, second, idx);
        return mismatch < 0 ? Math.min(idx - first, idx - second) : mismatch;
    }

    /**
     * The longest common prefix of the content at {@code best} with those among {@code candidates}
     * that sort below it; 0 if none does.
     */
    int impliedLength(int idx, int best, int[] candidates) {
        int implied = 0;
        for (int candidate : candidates) {
            if (candidate != best) {
                int shared = this.commonPrefix(idx, candidate, best);
                if (shared > implied && this.order(idx, candidate, best, shared) < 0) {
                    implied = shared;
                }
            }
        }
        return implied;
    }

    /** The order of two contents that share {@code shared} bytes. */
    private int order(int idx, int first, int second, int shared) {
        int firstLength = idx - first;
        int secondLength = idx - second;
        if (shared == Math.min(firstLength, secondLength)) {
            return Integer.compare(firstLength, secondLength);
        }
        byte[] bytes = this.segment.bytes();
        return Byte.toUnsignedInt(bytes[first + shared]) - Byte.toUnsignedInt(bytes[second + shared]);
    }
}
