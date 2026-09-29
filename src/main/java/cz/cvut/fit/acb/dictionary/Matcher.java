package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/** A {@link MatchRule} at work over one dictionary: finds the best match, and says what of its length is implied. */
interface Matcher {

    SearchResult search(int idx);

    /** @throws MalformedStreamException if the distance does not name an entry of the window */
    int impliedLength(int idx, int distance) throws MalformedStreamException;
}
