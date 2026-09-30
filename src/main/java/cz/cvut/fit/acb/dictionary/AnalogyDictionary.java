package cz.cvut.fit.acb.dictionary;

import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * The dictionary of the associative coder, the same on both sides: it holds the positions of a
 * segment ordered by their contexts, and gathers the funnels of analogies of a context. A funnel is
 * valid until the next one of its kind is asked for.
 */
public interface AnalogyDictionary {

    /** How many positions the dictionary holds. */
    int size();

    /**
     * Adds what a step that coded the {@code count} bytes from {@code idx} on leaves in the dictionary;
     * the segment must already hold them. A long step is worth one position, not one per byte.
     */
    void update(int idx, int count);

    /**
     * @return the position of the entry with rank {@code rank}
     * @throws MalformedStreamException if the rank is not in the dictionary
     */
    int select(int rank) throws MalformedStreamException;

    /** The analogies of the context before {@code position}, weighted to tell which one continues the text. */
    Funnel funnel(int position);

    /** The analogies of the context before {@code position}, weighted to vote on the byte that follows it. */
    Funnel forecast(int position);
}
