package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** The dictionary as the decoder uses it: it names the context of a position and resolves ranks. */
public interface DecoderDictionary extends Dictionary {

    /**
     * The rank of the neighbour of {@code idx} whose context agrees longer with it, or -1 if there
     * is none; distances are counted from it.
     */
    int contextRank(int idx);

    /**
     * How many bytes of the length of the match {@code distance} ranks from the context of {@code idx}
     * need not be sent, because both sides can work them out.
     *
     * @throws MalformedStreamException if the distance does not name an entry
     */
    int impliedLength(int idx, int distance) throws MalformedStreamException;
}
