package cz.cvut.fit.acb.dictionary;

/** The dictionary as the encoder uses it: it looks for the best match at a position. */
public interface EncoderDictionary extends Dictionary {

    SearchResult search(int idx);
}
