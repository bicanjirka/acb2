package cz.cvut.fit.acb.dictionary;

/** The dictionary as the encoder uses it: it looks for the best match at a position. */
public interface EncoderDictionary extends Dictionary {

    SearchResult search(int idx);

    /**
     * As {@link Dictionary#impliedLength}, for a distance the encoder found itself.
     *
     * @throws IllegalArgumentException if the distance is not one this dictionary found for {@code idx}
     */
    @Override
    int impliedLength(int idx, int distance);
}
