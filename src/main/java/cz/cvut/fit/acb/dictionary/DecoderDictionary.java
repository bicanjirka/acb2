package cz.cvut.fit.acb.dictionary;

/** The dictionary as the decoder uses it: it names the context of a position and resolves ranks. */
public interface DecoderDictionary extends Dictionary {

    /**
     * The rank of the entry immediately below where {@code idx} would be inserted, or -1 if there is
     * none; distances are counted from it.
     */
    int contextRank(int idx);
}
