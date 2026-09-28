package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

public interface Dictionary {

    /** How many positions the dictionary holds. */
    int size();

    /**
     * The first {@code leng} bytes of the content of rank {@code cnt}, repeated if it is shorter.
     *
     * @throws MalformedStreamException if the rank is not in the dictionary
     */
    byte[] copy(int cnt, int leng) throws MalformedStreamException;

    DictionaryInfo search(int idx);

    DictionaryInfo searchContent(int ctx, int idx);

    int searchContext(int idx);

    void update(int idx, int count);

    /**
     * @return the position of the entry with rank {@code idx}
     * @throws MalformedStreamException if the rank is not in the dictionary
     */
    int select(int idx) throws MalformedStreamException;
}
