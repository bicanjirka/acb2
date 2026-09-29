package cz.cvut.fit.acb.dictionary;

/**
 * What a dictionary needs to know of the settings: how many bytes of context order it, how far from
 * the context a content may lie in ranks, the longest length a triplet carries, and the rule that
 * picks the best match.
 */
public record SearchWindow(int contextDepth, int maxDistance, int maxLength, MatchRule rule) {

    /** Candidates are matched up to this many times the longest length a triplet carries, for the LCP rule. */
    private static final int LCP_MATCH_FACTOR = 4;

    /** How long a match is measured; one that long may have been cut short, and one shorter ended on a mismatch or the segment's end. */
    public int matchLimit() {
        return switch (this.rule) {
            case NEAREST -> this.maxLength;
            case SMALLEST_WITH_LCP -> LCP_MATCH_FACTOR * this.maxLength;
        };
    }

    /** The lowest rank of the window around {@code ctx}. */
    int first(int ctx) {
        return Math.max(0, ctx - this.maxDistance + 1);
    }

    /** The highest rank of the window around {@code ctx}, in a dictionary of {@code size} entries. */
    int last(int ctx, int size) {
        return Math.min(size - 1, ctx + this.maxDistance);
    }
}
