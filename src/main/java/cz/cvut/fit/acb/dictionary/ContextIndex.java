package cz.cvut.fit.acb.dictionary;

/**
 * The positions of a segment sorted by their contexts, with the rank arithmetic the dictionary
 * needs. The rank of an entry is the number of entries before it.
 */
public interface ContextIndex {

    int size();

    /** How many entries sort strictly before {@code position}, whether or not it is an entry itself. */
    int rank(int position);

    /** Adds {@code position}; a lookup of the same position right before makes this cheaper. */
    void insert(int position);

    /** @throws IndexOutOfBoundsException if there is no entry of that rank */
    ContextCursor cursorAt(int rank);

    /**
     * Copies the entries on either side of the place {@code position} would sort in, at most
     * {@code reach} of each and no more than {@code into} holds, without a search for their ranks.
     */
    void around(int position, int reach, Surroundings into);
}
