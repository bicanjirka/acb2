package cz.cvut.fit.acb.dictionary;

/**
 * A place in a {@link ContextIndex} that walks to the neighbouring ranks without a search. Valid
 * only until the index is next changed.
 */
public interface ContextCursor {

    int rank();

    int position();

    /** @return whether there was a next rank; if not, the cursor stays where it is */
    boolean moveUp();

    /** @return whether there was a previous rank; if not, the cursor stays where it is */
    boolean moveDown();
}
