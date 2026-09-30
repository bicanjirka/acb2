package cz.cvut.fit.acb.dictionary;

/**
 * The entries of a {@link ContextIndex} on either side of the place a context would sort in, nearest
 * first, with the first bytes of the context of each and how many bytes of context it has in common
 * with the entry next to it on the far side of the context: what a walk outward from the context
 * needs, copied out in one go. One is refilled for
 * every funnel, so it holds primitive arrays that are overwritten in place; only an index fills it,
 * and what a reader is handed is valid until the next fill.
 */
public final class Surroundings {

    private final int[] belowPositions;
    private final long[] belowPrefixes;
    private final int[] belowShared;
    private final int[] abovePositions;
    private final long[] abovePrefixes;
    private final int[] aboveShared;
    private int belowCount;
    private int aboveCount;

    /** @param reach the most entries on either side */
    public Surroundings(int reach) {
        this.belowPositions = new int[reach];
        this.belowPrefixes = new long[reach];
        this.belowShared = new int[reach];
        this.abovePositions = new int[reach];
        this.abovePrefixes = new long[reach];
        this.aboveShared = new int[reach];
    }

    int belowCount() {
        return this.belowCount;
    }

    int aboveCount() {
        return this.aboveCount;
    }

    /** The positions below the context, the nearest first. */
    int[] belowPositions() {
        return this.belowPositions;
    }

    /** The {@link ContextOrder#prefix prefixes} of the entries below. */
    long[] belowPrefixes() {
        return this.belowPrefixes;
    }

    /** For each entry below, the bytes it has in common with the one before it in the order, which is the next one out. */
    int[] belowShared() {
        return this.belowShared;
    }

    /** The positions above the context, the nearest first. */
    int[] abovePositions() {
        return this.abovePositions;
    }

    /** The {@link ContextOrder#prefix prefixes} of the entries above. */
    long[] abovePrefixes() {
        return this.abovePrefixes;
    }

    /** For each entry above, the bytes it has in common with the one before it in the order, which is the next one in. */
    int[] aboveShared() {
        return this.aboveShared;
    }

    /** Empties it, for an index that is about to fill it. */
    public void clear() {
        this.belowCount = 0;
        this.aboveCount = 0;
    }

    /** Adds the next entry below the context, further out than the ones before it. */
    public void addBelow(int position, long prefix, int shared) {
        this.belowPositions[this.belowCount] = position;
        this.belowPrefixes[this.belowCount] = prefix;
        this.belowShared[this.belowCount++] = shared;
    }

    /** Adds the next entry above the context, further out than the ones before it. */
    public void addAbove(int position, long prefix, int shared) {
        this.abovePositions[this.aboveCount] = position;
        this.abovePrefixes[this.aboveCount] = prefix;
        this.aboveShared[this.aboveCount++] = shared;
    }
}
