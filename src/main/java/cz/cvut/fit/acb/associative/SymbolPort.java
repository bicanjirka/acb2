package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.coding.CumulativeTable;
import cz.cvut.fit.acb.dictionary.Funnel;
import cz.cvut.fit.acb.format.MalformedStreamException;

/**
 * Where the symbols of a step come from: the encoder knows the text and codes the symbol it finds
 * there; the decoder reads the symbol from the stream and rebuilds the text. Everything else in a step
 * is the same on both sides, so it is written once, against this.
 */
interface SymbolPort {

    /**
     * @return 0 when no candidate of the funnel continues the text at {@code idx}, else the number of
     * the one that does, counted from 1
     */
    int position(int idx, Funnel funnel, CumulativeTable table) throws MalformedStreamException;

    /**
     * @param floor how many bytes of the match the decoder knows of
     * @return how many bytes the match has beyond {@code floor}, less the one byte it must have
     */
    int length(CumulativeTable table, int floor) throws MalformedStreamException;

    /** The literal at position {@code at}; the decoder appends it to its text. */
    int literal(int at, CumulativeTable table) throws MalformedStreamException;

    /** A match of {@code count} bytes of the content at {@code from} has been coded; the decoder copies it. */
    void copied(int from, int count);
}
