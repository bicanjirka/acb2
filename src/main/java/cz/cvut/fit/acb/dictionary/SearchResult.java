package cz.cvut.fit.acb.dictionary;

/** What a dictionary search found: no match, or the best content with its distance from the context. */
public sealed interface SearchResult {

    static SearchResult miss() {
        return Miss.INSTANCE;
    }

    /** @param distance the context's rank minus the content's, within the window the settings allow */
    static SearchResult hit(int distance, int length) {
        return new Hit(distance, length);
    }

    enum Miss implements SearchResult {
        INSTANCE
    }

    record Hit(int distance, int length) implements SearchResult {

        public Hit {
            if (length < 1) {
                throw new IllegalArgumentException("a hit matches at least one byte: " + length);
            }
        }
    }
}
