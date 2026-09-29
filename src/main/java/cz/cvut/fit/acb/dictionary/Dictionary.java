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
}
