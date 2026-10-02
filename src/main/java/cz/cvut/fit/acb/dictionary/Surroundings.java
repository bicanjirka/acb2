package cz.cvut.fit.acb.dictionary;

/**
 * The entries of a {@link ContextIndex} on either side of the place a context would sort in, each with
 * how many bits of context it has in common with the entry before it in the order and the byte that
 * follows its context, and how many bits the nearest entry on each side has in common with the context:
 * what a walk outward from the context needs, copied out in one go. One is refilled for every funnel, so
 * it holds primitive arrays that are overwritten in place; only an index fills it, and what a reader is
 * handed is valid until the next fill.
 *
 * <p>The entries are kept in the order of the index, which a copy of a run of the index keeps. Those above
 * the context start at the beginning of their arrays, the nearest first; those below end at the end of
 * theirs, the nearest last, so that a reader goes from the end.
 *
 * <p>What an entry shares and the byte after its context are kept in one {@code int}, which an index keeps
 * the same way, so that a run is copied in one go: an array of each measured slower to insert into.
 */
public final class Surroundings {

    private static final int SHARED_BITS = 0xFFFF;
    private static final int BYTE_SHIFT = 16;

    private final int[] belowPositions;
    private final int[] belowShared;
    private final int[] abovePositions;
    private final int[] aboveShared;
    private int belowCount;
    private int aboveCount;
    private int belowAgreement;
    private int aboveAgreement;

    /** @param reach the most entries on either side */
    public Surroundings(int reach) {
        this.belowPositions = new int[reach];
        this.belowShared = new int[reach];
        this.abovePositions = new int[reach];
        this.aboveShared = new int[reach];
    }

    /** What an entry keeps besides its position: the bits it has in common with the one before, and the byte after its context. */
    public static int shared(int sharedBits, int followingByte) {
        return followingByte << BYTE_SHIFT | sharedBits;
    }

    /** The bits of {@link #shared}. */
    public static int sharedBits(int shared) {
        return shared & SHARED_BITS;
    }

    /** The byte of {@link #shared}. */
    public static int followingByte(int shared) {
        return shared >>> BYTE_SHIFT;
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

    /** {@link #shared} of each entry above, whose entry before is the next one in. */
    int[] aboveShared() {
        return this.aboveShared;
    }

    /** How many bits of context the nearest entry above has in common with the context; 0 if there is none. */
    int aboveAgreement() {
        return this.aboveAgreement;
    }

    int[] belowPositions() {
        return this.belowPositions;
    }

    /** {@link #shared} of each entry below, whose entry before is the next one out. */
    int[] belowShared() {
        return this.belowShared;
    }

    /** As {@link #aboveAgreement()}, for the nearest entry below. */
    int belowAgreement() {
        return this.belowAgreement;
    }

    /** Empties it, for an index that is about to fill it. */
    public void clear() {
        this.belowCount = 0;
        this.aboveCount = 0;
        this.belowAgreement = 0;
        this.aboveAgreement = 0;
    }

    /** Sets how many bits of context the nearest entry above has in common with the context. */
    public void agreeAbove(int bits) {
        this.aboveAgreement = bits;
    }

    /** Sets how many bits of context the nearest entry below has in common with the context. */
    public void agreeBelow(int bits) {
        this.belowAgreement = bits;
    }

    /**
     * Adds {@code count} entries from {@code from} on in a run of the index above the context, after
     * those already added.
     */
    public void addAbove(int[] positions, int[] shared, int from, int count) {
        System.arraycopy(positions, from, this.abovePositions, this.aboveCount, count);
        System.arraycopy(shared, from, this.aboveShared, this.aboveCount, count);
        this.aboveCount += count;
    }

    /**
     * Adds {@code count} entries from {@code from} on in a run of the index below the context, the last
     * of which is the nearest to it of those added, and which lie further out than those already added.
     */
    public void addBelow(int[] positions, int[] shared, int from, int count) {
        int start = this.belowPositions.length - this.belowCount - count;
        System.arraycopy(positions, from, this.belowPositions, start, count);
        System.arraycopy(shared, from, this.belowShared, start, count);
        this.belowCount += count;
    }
}
