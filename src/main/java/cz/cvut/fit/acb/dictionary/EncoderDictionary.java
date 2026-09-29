package cz.cvut.fit.acb.dictionary;

/** The dictionary as the encoder uses it: it looks for the best match at a position. */
public interface EncoderDictionary extends Dictionary {

    SearchResult search(int idx);

    /**
     * How many bytes of the length of the match {@code distance} ranks from the context of {@code idx}
     * need not be sent, because the decoder can work them out.
     *
     * @throws IllegalArgumentException if the distance is not one this dictionary found for {@code idx}
     */
    int impliedLength(int idx, int distance);
}
