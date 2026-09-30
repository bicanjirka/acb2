package cz.cvut.fit.acb.dictionary;

/**
 * The entries of a {@link ContextIndex} on either side of the place a context would sort in, with the
 * first bytes of the context of each and how many bytes of context it has in common with the entry
 * before it in the order: what a walk outward from the context needs, copied out in one go. One is
 * refilled for every funnel, so it holds primitive arrays that are overwritten in place; only an index
 * fills it, and what a reader is handed is valid until the next fill.
 *
 * <p>The entries are kept in the order of the index, which a copy of a run of the index keeps. Those above
 * the context start at the beginning of their arrays, the nearest first; those below end at the end of
 * theirs, the nearest last, so that a reader goes from the end.
 */
public final class Surroundings {

    private final int[] belowPositions;
    private final long[] belowPrefixes;
    private final byte[] belowShared;
    private final int[] abovePositions;
    private final long[] abovePrefixes;
    private final byte[] aboveShared;
    private int belowCount;
    private int aboveCount;

    /** @param reach the most entries on either side */
    public Surroundings(int reach) {
        this.belowPositions = new int[reach];
        this.belowPrefixes = new long[reach];
        this.belowShared = new byte[reach];
        this.abovePositions = new int[reach];
        this.abovePrefixes = new long[reach];
        this.aboveShared = new byte[reach];
    }

    /** How many entries on either side at most. */
    int capacity() {
        return this.belowPositions.length;
    }

    int belowCount() {
        return this.belowCount;
    }

    int aboveCount() {
        return this.aboveCount;
    }

    int[] abovePositions() {
        return this.abovePositions;
    }

    long[] abovePrefixes() {
        return this.abovePrefixes;
    }

    /** What each entry above has in common with the one before it in the order, which is the next one in. */
    byte[] aboveShared() {
        return this.aboveShared;
    }

    int[] belowPositions() {
        return this.belowPositions;
    }

    long[] belowPrefixes() {
        return this.belowPrefixes;
    }

    /** What each entry below has in common with the one before it in the order, which is the next one out. */
    byte[] belowShared() {
        return this.belowShared;
    }

    /** Empties it, for an index that is about to fill it. */
    public void clear() {
        this.belowCount = 0;
        this.aboveCount = 0;
    }

    /**
     * Adds {@code count} entries from {@code from} on in a run of the index above the context, after
     * those already added.
     */
    public void addAbove(int[] positions, long[] prefixes, byte[] shared, int from, int count) {
        System.arraycopy(positions, from, this.abovePositions, this.aboveCount, count);
        System.arraycopy(prefixes, from, this.abovePrefixes, this.aboveCount, count);
        System.arraycopy(shared, from, this.aboveShared, this.aboveCount, count);
        this.aboveCount += count;
    }

    /**
     * Adds {@code count} entries from {@code from} on in a run of the index below the context, the last
     * of which is the nearest to it of those added, and which lie further out than those already added.
     */
    public void addBelow(int[] positions, long[] prefixes, byte[] shared, int from, int count) {
        int start = this.belowPositions.length - this.belowCount - count;
        System.arraycopy(positions, from, this.belowPositions, start, count);
        System.arraycopy(prefixes, from, this.belowPrefixes, start, count);
        System.arraycopy(shared, from, this.belowShared, start, count);
        this.belowCount += count;
    }
}
