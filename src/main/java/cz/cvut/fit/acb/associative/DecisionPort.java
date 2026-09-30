package cz.cvut.fit.acb.associative;

import cz.cvut.fit.acb.format.MalformedStreamException;
import cz.cvut.fit.acb.triplets.TripletFieldKind;

/**
 * Where the decisions of a mixed step come from, a bit at a time: the encoder knows the text and codes the
 * answer it is given; the decoder reads the answer from the stream and rebuilds the text. A step hands the
 * encoder's answer only to {@link #bit}, and goes on from what that returns, so the two sides take the
 * same path.
 */
interface DecisionPort {

    /** The byte at {@code at} as the encoder knows it; the decoder does not know it yet and gets 0. */
    int textByte(int at);

    /**
     * Codes one decision: the encoder codes {@code encoderBit}, the decoder reads a bit.
     *
     * @param probabilityOfOne in units of {@code 2^-16}, strictly between 0 and 1
     * @return the bit
     */
    int bit(TripletFieldKind kind, int encoderBit, int probabilityOfOne) throws MalformedStreamException;

    /** The step has found the byte at the end of the text; the decoder appends it. */
    void appended(int value);
}
