package cz.cvut.fit.acb.dictionary;

/**
 * What a dictionary needs to know of the settings: how many bytes of context order it, how far from
 * the context a content may lie in ranks, the longest length a triplet carries, and the rule that
 * picks the best match.
 */
public record SearchWindow(int contextDepth, int maxDistance, int maxLength, MatchRule rule) {

    /** How long a match is measured; see {@link MatchRule#matchLimit}. */
    public int matchLimit() {
        return this.rule.matchLimit(this.maxLength);
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
