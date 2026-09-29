package cz.cvut.fit.acb.dictionary;

/** Which candidate of the search window is the best match, and how much of its length the decoder can work out itself. */
public enum MatchRule {

    /** The longest match; the nearest to the context among equals. Nothing of the length is implied. */
    NEAREST,

    /**
     * The longest match; the lexicographically smallest content among equals. The decoder can work
     * out the longest common prefix of the best content with the contents that sort below it, and
     * the length sent is the match length less that prefix.
     */
    SMALLEST_WITH_LCP
}
