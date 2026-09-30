package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** What both sides do to a dictionary: grow it by the bytes a step coded, and read its order. */
public interface Dictionary {

    /** How many positions the dictionary holds. */
    int size();

    /** Adds the positions {@code idx .. idx + count - 1}, whose bytes the segment must already hold. */
    void update(int idx, int count);

    /**
     * @return the position of the entry with rank {@code rank}
     * @throws MalformedStreamException if the rank is not in the dictionary
     */
    int select(int rank) throws MalformedStreamException;

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

    /**
     * The bytes that would have made a match longer: of the candidates of the window, those whose first
     * {@code length} bytes are the same as the text's, which the content at {@code content} gives, go on
     * with one byte each. With a {@code length} of 0 every candidate qualifies and {@code content} means
     * nothing. A candidate whose next byte is not known yet, because it lies at or past {@code idx}, is
     * left out.
     */
    ByteSet continuations(int idx, int content, int length);
}
