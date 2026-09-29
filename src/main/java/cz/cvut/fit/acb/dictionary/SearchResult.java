package cz.cvut.fit.acb.dictionary;

/** What a dictionary search found: no match, or the best content with its distance from the context. */
public sealed interface SearchResult {

    static SearchResult miss() {
        return Miss.INSTANCE;
    }

    /** @param distance the context's rank minus the content's, within the window the settings allow */
    static SearchResult hit(int distance, int length) {
        return new Hit(distance, length, 0);
    }

    /** A hit whose first {@code implied} bytes of length the decoder works out itself. */
    static SearchResult hit(int distance, int length, int implied) {
        return new Hit(distance, length, implied);
    }

    enum Miss implements SearchResult {
        INSTANCE
    }

    record Hit(int distance, int length, int implied) implements SearchResult {

        public Hit {
            if (implied < 0 || length <= implied) {
                throw new IllegalArgumentException("a hit matches more bytes than are implied: " + length
                        + " and " + implied);
            }
        }
    }
}
