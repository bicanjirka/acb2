package cz.cvut.fit.acb.dictionary;

import java.util.function.Function;

/**
 * Which candidate of the search window is the best match, and how much of its length the decoder can
 * work out itself. A constant carries all there is to a rule, so a new rule is added here and as a
 * {@link Matcher}, and nowhere else.
 */
public enum MatchRule {

    /** The longest match; the nearest to the context among equals. Nothing of the length is implied. */
    NEAREST(1, NearestMatcher::new),

    /**
     * The longest match; the lexicographically smallest content among equals. The decoder can work
     * out the longest common prefix of the best content with the contents that sort below it, and
     * the length sent is the match length less that prefix.
     */
    SMALLEST_WITH_LCP(4, SmallestWithLcpMatcher::new),

    /**
     * The longest match; the nearest to the context among equals, as {@link #NEAREST}. The decoder can
     * work out the longest common prefix of the best content with the contents walked before it, and
     * the length sent is the match length less that prefix.
     */
    NEAREST_WITH_PREFIX(4, NearestWithPrefixMatcher::new);

    private final int limitFactor;
    private final Function<ContextRanking, Matcher> matchers;

    MatchRule(int limitFactor, Function<ContextRanking, Matcher> matchers) {
        this.limitFactor = limitFactor;
        this.matchers = matchers;
    }

    /**
     * How long a match is measured, given the longest length a triplet carries: one that long may have
     * been cut short, and one shorter ended on a mismatch or the segment's end. A rule that implies
     * part of the length measures further, so that the part is not all there is to a match.
     */
    int matchLimit(int maxLength) {
        return this.limitFactor * maxLength;
    }

    Matcher over(ContextRanking ranking) {
        return this.matchers.apply(ranking);
    }
}
