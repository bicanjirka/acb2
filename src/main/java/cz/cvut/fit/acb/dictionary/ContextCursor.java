package cz.cvut.fit.acb.dictionary;

/**
 * A place in a {@link ContextIndex} that walks to the neighbouring ranks without a search. Valid
 * only until the index is next changed.
 */
public interface ContextCursor {

    /** The cursor of a walk that has run out of ranks: absent, and it goes nowhere. */
    static ContextCursor none() {
        return Absent.INSTANCE;
    }

    /** Whether this is a place in the index, not {@link #none()}. */
    default boolean isPresent() {
        return true;
    }

    int rank();

    int position();

    /** @return whether there was a next rank; if not, the cursor stays where it is */
    boolean moveUp();

    /** @return whether there was a previous rank; if not, the cursor stays where it is */
    boolean moveDown();

    enum Absent implements ContextCursor {
        INSTANCE;

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public int rank() {
            throw new IllegalStateException("There is no cursor");
        }

        @Override
        public int position() {
            throw new IllegalStateException("There is no cursor");
        }

        @Override
        public boolean moveUp() {
            return false;
        }

        @Override
        public boolean moveDown() {
            return false;
        }
    }
}
